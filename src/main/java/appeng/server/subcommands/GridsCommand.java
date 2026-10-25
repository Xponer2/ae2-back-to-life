package appeng.server.subcommands;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import appeng.api.networking.GridHelper;
import appeng.core.network.PacketDistributor;
import appeng.core.network.clientbound.ExportedGridContent;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.hooks.ticking.TickHandler;
import appeng.me.Grid;
import appeng.me.service.StatisticsService;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.server.ISubCommand;
import appeng.util.Platform;

public class GridsCommand implements ISubCommand {
    private static final Logger LOG = LogManager.getLogger(GridsCommand.class);

    // AnvilChunkLoader.writeChunkToNBT is private
    private static final Method WRITE_CHUNK_TO_NBT = ObfuscationReflectionHelper.findMethod(AnvilChunkLoader.class,
            "func_75820_a", void.class, Chunk.class, World.class, NBTTagCompound.class);

    public static String buildExportCommand(int gridSerial) {
        return "/ae2 grids export " + gridSerial;
    }

    @Override
    public String getArgumentUsage() {
        return "export [gridSerial]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        return args.length == 1 ? CommandBase.getListOfStringsMatchingLastWord(args, "export")
                : Collections.emptyList();
    }

    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0 || !args[0].equals("export")) {
            throw new WrongUsageException("/ae2 grids " + getArgumentUsage());
        }
        if (args.length == 1) {
            exportGrids(sender);
            return;
        }

        var gridSerial = CommandBase.parseInt(args[1]);
        // Find the starting grid
        for (var grid : TickHandler.instance().getGridList()) {
            if (grid.getSerialNumber() == gridSerial) {
                exportGrid(grid, sender);
                return;
            }
        }

        throw new CommandException("No such grid found");
    }

    private void exportGrids(ICommandSender source) {

        var grids = TickHandler.instance().getGridList();

        source.sendMessage(new TextComponentString("Exporting " + grids.size() + " grids"));

        exportGrids(0, grids, source);

    }

    private void exportGrid(Grid startGrid, ICommandSender source) {

        // Collect all reachable grids
        var reachableGrids = Collections.newSetFromMap(new IdentityHashMap<Grid, Boolean>());
        reachableGrids.add(startGrid);
        var openSet = Collections.newSetFromMap(new IdentityHashMap<Grid, Boolean>());
        openSet.add(startGrid);

        while (!openSet.isEmpty()) {
            var it = openSet.iterator();
            var grid = it.next();
            it.remove();
            for (var node : grid.getNodes()) {
                if (node.getOwner() instanceof AEBasePart basePart) {
                    visitGridInFrontOfPart(basePart, reachableGrids, openSet);
                } else if (node.getOwner() instanceof PatternProviderLogicHost patternProvider) {
                    for (var targetSide : patternProvider.getTargets()) {
                        visitGridAt(
                                patternProvider.getTileEntity().getWorld(),
                                patternProvider.getTileEntity().getPos().offset(targetSide),
                                reachableGrids,
                                openSet);
                    }
                } else if (node.getOwner() instanceof MEP2PTunnelPart meTunnel) {
                    var tunnelGrid = (Grid) meTunnel.getMainNode().getGrid();
                    if (tunnelGrid != null && reachableGrids.add(tunnelGrid)) {
                        openSet.add(tunnelGrid);
                    }
                }
            }
        }

        exportGrids(startGrid.getSerialNumber(), reachableGrids, source);
    }

    private static void visitGridInFrontOfPart(AEBasePart part, Set<Grid> reachableGrids, Set<Grid> openSet) {
        var partSide = part.getSide();
        if (partSide == null) {
            return;
        }
        // Storage buses that are attached to devices on different grids are interesting to us
        var hostBe = part.getTileEntity();
        var targetPos = hostBe.getPos().offset(partSide);
        visitGridAt(hostBe.getWorld(), targetPos, reachableGrids, openSet);
    }

    private static void visitGridAt(World level, BlockPos pos, Set<Grid> reachableGrids, Set<Grid> openSet) {
        var targetGridHost = GridHelper.getNodeHost(level, pos);
        if (targetGridHost != null) {
            for (var side : Platform.DIRECTIONS_WITH_NULL) {
                var nodeOnSide = targetGridHost.getGridNode(side);
                if (nodeOnSide != null) {
                    var nodeGrid = (Grid) nodeOnSide.getGrid();
                    if (reachableGrids.add(nodeGrid)) {
                        openSet.add(nodeGrid);
                    }
                }
            }
        }
    }

    private void exportGrids(int baseSerialNumber, Collection<Grid> grids, ICommandSender source) {
        source.sendMessage(new TextComponentString("Exporting " + grids.size() + " grids"));
        LOG.info("Exporting {} grids for {}", grids.size(), source.getName());

        if (source instanceof EntityPlayerMP player) {
            PacketDistributor.sendToPlayer(player,
                    new ExportedGridContent(baseSerialNumber, ExportedGridContent.ContentType.FIRST_CHUNK,
                            new byte[0]));

            try (var out = new SendToPlayerStream(player, baseSerialNumber)) {
                exportGrids(grids, out);
            }
        } else {
            var targetPath = Paths.get("grids.zip");
            try (var out = Files.newOutputStream(targetPath)) {
                exportGrids(grids, out);
            } catch (IOException e) {
                LOG.error("Failed to export grids.", e);
                source.sendMessage(new TextComponentString("Failed to export grids: " + e));
            }
        }

    }

    private void exportGrids(Iterable<Grid> grids, OutputStream out) {
        try (var zipOut = new ZipOutputStream(out)) {
            // Collect all chunks that grids live in and dump them all later
            var chunksByLevel = new HashMap<WorldServer, Set<ChunkPos>>();

            for (var grid : grids) {
                var statisticsService = grid.getService(StatisticsService.class);
                for (var entry : statisticsService.getChunks().entrySet()) {
                    chunksByLevel.computeIfAbsent(entry.getKey(), level -> new HashSet<>())
                            .addAll(entry.getValue().elementSet());
                }

                var entry = new ZipEntry("grid_" + grid.getSerialNumber() + ".json");
                zipOut.putNextEntry(entry);

                try (var writer = new JsonWriter(
                        new OutputStreamWriter(new CloseShieldOutputStream(zipOut), StandardCharsets.UTF_8))) {
                    writer.setIndent(" ");
                    grid.export(writer);
                }
            }

            zipOut.putNextEntry(new ZipEntry("chunks/"));
            for (var entry : chunksByLevel.entrySet()) {
                var level = entry.getKey();
                var chunks = entry.getValue();
                var baseName = sanitizeName(level.provider.getDimensionType().getName() + "_"
                        + level.provider.getDimension());
                // Only used to serialize chunks the way the region files store them, never saves anything
                var chunkLoader = new AnvilChunkLoader(level.getSaveHandler().getWorldDirectory(),
                        level.getMinecraftServer().getDataFixer());
                for (var chunk : chunks) {
                    var levelTag = new NBTTagCompound();
                    try {
                        WRITE_CHUNK_TO_NBT.invoke(chunkLoader, level.getChunk(chunk.x, chunk.z), level, levelTag);
                    } catch (ReflectiveOperationException e) {
                        throw new IOException("Failed to serialize chunk " + chunk, e);
                    }
                    var serializedChunk = new NBTTagCompound();
                    serializedChunk.setTag("Level", levelTag);

                    zipOut.putNextEntry(new ZipEntry("chunks/" + baseName + "_" + chunk.x + "_" + chunk.z + ".nbt"));
                    CompressedStreamTools.writeCompressed(serializedChunk, new CloseShieldOutputStream(zipOut));

                    zipOut.putNextEntry(new ZipEntry("chunks/" + baseName + "_" + chunk.x + "_" + chunk.z + ".snbt"));
                    zipOut.write(serializedChunk.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sanitizeName(String string) {
        return string.replaceAll("[^A-Za-z0-9-,]", "_");
    }

    private static class SendToPlayerStream extends OutputStream {
        private static final int FLUSH_AFTER = 512 * 1024;
        private final ByteArrayOutputStream bout; // 512kb buffer
        private final EntityPlayerMP player;
        private final int baseSerialNumber;
        private boolean closed;

        public SendToPlayerStream(EntityPlayerMP player, int baseSerialNumber) {
            this.player = player;
            this.baseSerialNumber = baseSerialNumber;
            bout = new ByteArrayOutputStream(FLUSH_AFTER);
        }

        @Override
        public void write(int b) {
            Preconditions.checkState(!closed, "stream already closed");
            bout.write(b);
            if (bout.size() > FLUSH_AFTER) {
                PacketDistributor.sendToPlayer(player,
                        new ExportedGridContent(baseSerialNumber, ExportedGridContent.ContentType.CHUNK,
                                bout.toByteArray()));
                bout.reset();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            Preconditions.checkState(!closed, "stream already closed");
            bout.write(b, off, len);
            if (bout.size() > FLUSH_AFTER) {
                PacketDistributor.sendToPlayer(player,
                        new ExportedGridContent(baseSerialNumber, ExportedGridContent.ContentType.CHUNK,
                                bout.toByteArray()));
                bout.reset();
            }
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                PacketDistributor.sendToPlayer(player,
                        new ExportedGridContent(baseSerialNumber, ExportedGridContent.ContentType.LAST_CHUNK,
                                bout.toByteArray()));
                bout.reset();
            }
        }
    }
}
