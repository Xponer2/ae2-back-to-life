/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.server.subcommands;

import appeng.util.TextUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.google.common.math.StatsAccumulator;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;

import appeng.api.util.Registries;
import appeng.server.ISubCommand;
import appeng.worldgen.meteorite.MeteoriteStructure;
import appeng.worldgen.meteorite.MeteoriteStructurePiece;
import appeng.worldgen.meteorite.PlacedMeteoriteSettings;

import static net.minecraft.commands.Commands.literal;

/**
 * This is a testing command to validate quartz ore generation.
 */
public class TestMeteoritesCommand implements ISubCommand {

    @Override
    public void addArguments(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(literal("force").executes(ctx -> {
            test(ctx.getSource().getServer(), ctx.getSource(), true);
            return 1;
        }));
    }

    @Override
    public void call(MinecraftServer srv, CommandContext<CommandSourceStack> ctx,
            CommandSourceStack sender) {
        test(srv, sender, false);
    }

    private static void test(MinecraftServer srv, CommandSourceStack sender, boolean force) {
        int radius = 100;

        EntityPlayerMP player = null;
        try {
            player = sender.getPlayerOrException();
        } catch (CommandSyntaxException ignored) {
        }
        WorldServer level;
        BlockPos centerBlock;
        if (player != null) {
            level = player.serverLevel();
            centerBlock = BlockPos.containing(player.posX, 0, player.posZ);
        } else {
            level = srv.getLevel(World.OVERWORLD);
            centerBlock = level.getSpawnPoint();
        }

        ChunkPos center = new ChunkPos(centerBlock);

        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structure = structures.get(MeteoriteStructure.KEY);
        var structureSets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var structureSet = structureSets.getHolderOrThrow(MeteoriteStructure.STRUCTURE_SET_KEY);

        var generatorState = level.getChunkProvider().getGeneratorState();

        // Find all meteorites in the given rectangle
        List<PlacedMeteoriteSettings> found = new ArrayList<>();
        int chunksChecked = 0;
        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                chunksChecked++;
                if (!generatorState.hasStructureChunkInRange(structureSet, cx, cz, 0)) {
                    continue;
                }

                var chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS);
                // The actual relevant information is in the structure piece
                MeteoriteStructurePiece piece = getMeteoritePieceFromChunk(chunk, structure);
                if (piece != null) {
                    found.add(piece.getSettings());
                }
            }
        }

        // Create stats on how far apart the meteorites are
        StatsAccumulator stats = new StatsAccumulator();
        for (PlacedMeteoriteSettings settings : found) {
            double closestOther = Double.NaN;
            for (PlacedMeteoriteSettings otherSettings : found) {
                if (otherSettings != settings) {
                    double d = settings.getPos().distanceSq(otherSettings.getPos());
                    if (Double.isNaN(closestOther) || d < closestOther) {
                        closestOther = d;
                    }
                }
            }

            if (!Double.isNaN(closestOther)) {
                stats.add(Math.sqrt(closestOther));
            }
        }

        found.sort(Comparator.comparingDouble(settings -> settings.getPos().distanceSq(centerBlock)));

        sendLine(sender, "Chunks checked: %d", chunksChecked);
        sendLine(sender, "Meteorites found: %d", found.size());
        if (stats.count() > 0) {
            sendLine(sender, "Closest: min=%.2f max=%.2f mean=%.2f stddev=%.2f", stats.min(), stats.max(), stats.mean(),
                    stats.populationStandardDeviation());
        }

        int closestCount = Math.min(10, found.size());
        for (int i = 0; i < closestCount; i++) {
            PlacedMeteoriteSettings settings = found.get(i);
            BlockPos pos = settings.getPos();

            String state = "not final";

            if (force && settings.getFallout() == null) {
                Chunk chunk = level.getChunk(pos);
                MeteoriteStructurePiece piece = getMeteoritePieceFromChunk(chunk, structure);
                if (piece == null) {
                    state = "removed";
                } else {
                    settings = piece.getSettings();
                    pos = settings.getPos();
                }
            }

            ITextComponent restOfLine;
            if (settings.getFallout() == null) {
                restOfLine = new TextComponentString(
                        String.format(Locale.ROOT, ", radius=%.2f [%s]", settings.getMeteoriteRadius(), state));
            } else {
                restOfLine = new TextComponentString(String.format(Locale.ROOT, ", radius=%.2f, crater=%s, fallout=%s",
                        settings.getMeteoriteRadius(), settings.getCraterType().name().toLowerCase(),
                        settings.getFallout().name().toLowerCase()));
            }

            ITextComponent msg = new TextComponentString(" #" + (i + 1) + " ");
            msg.appendSibling(getClickablePosition(level, settings, pos)).appendSibling(restOfLine);

            // Add a tooltip
            String biomeId = level.getBiome(pos).unwrapKey().map(bk -> bk.location().toString()).orElse("unknown");
            ITextComponent tooltip = new TextComponentString(settings + "\nBiome: ").createCopy()
                    .appendSibling(biomeId);
            TextUtil.withStyle(msg, style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));

            sender.sendSuccess(() -> msg, true);
        }
    }

    // Add a clickable link to teleport the user to the Meteorite
    private static ITextComponent getClickablePosition(WorldServer level, PlacedMeteoriteSettings settings,
            BlockPos pos) {
        BlockPos tpPos = pos.above((int) Math.ceil(settings.getMeteoriteRadius()));
        int surfaceY = level.getHeightmapPos(Types.WORLD_SURFACE, tpPos).getY();
        if (surfaceY > tpPos.getY()) {
            tpPos = new BlockPos(tpPos.getX(), surfaceY, tpPos.getZ());
        }

        String displayText = String.format(Locale.ROOT, "pos=%d,%d,%d", tpPos.getX(), tpPos.getY(), tpPos.getZ());
        String tpCommand = String.format(Locale.ROOT, "/tp @s %d %d %d", tpPos.getX(), tpPos.getY(), tpPos.getZ());

        return TextUtil.withStyle(new TextComponentString(displayText), TextFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent(Action.RUN_COMMAND, tpCommand)));
    }

    private static MeteoriteStructurePiece getMeteoritePieceFromChunk(Chunk chunk,
            Structure structure) {
        var start = chunk.getStartForStructure(structure);

        if (start != null && start.getPieces().size() > 0
                && start.getPieces().get(0) instanceof MeteoriteStructurePiece) {
            return (MeteoriteStructurePiece) start.getPieces().get(0);
        }
        return null;
    }

    private static void sendLine(CommandSourceStack sender, String text, Object... args) {
        sender.sendSuccess(() -> new TextComponentString(String.format(Locale.ROOT, text, args)), true);
    }

}
