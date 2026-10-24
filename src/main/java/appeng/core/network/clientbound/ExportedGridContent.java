package appeng.core.network.clientbound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;

/**
 * Contains data produced by {@link appeng.server.subcommands.GridsCommand}
 */
public final class ExportedGridContent implements ClientboundPacket {
    private final int serialNumber;
    private final ContentType contentType;
    private final byte[] compressedData;

    public ExportedGridContent(int serialNumber, ContentType contentType, byte[] compressedData) {
        this.serialNumber = serialNumber;
        this.contentType = contentType;
        this.compressedData = compressedData;
    }

    public int serialNumber() {
        return serialNumber;
    }

    public ContentType contentType() {
        return contentType;
    }

    public byte[] compressedData() {
        return compressedData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExportedGridContent)) return false;
        ExportedGridContent that = (ExportedGridContent) o;
        return serialNumber == that.serialNumber
                && java.util.Objects.equals(contentType, that.contentType)
                && java.util.Objects.equals(compressedData, that.compressedData);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(serialNumber, contentType, compressedData);
    }

    @Override
    public String toString() {
        return "ExportedGridContent[" + "serialNumber=" + serialNumber + ", " + "contentType=" + contentType + ", " + "compressedData=" + compressedData + "]";
    }

    public static final Type<ExportedGridContent> TYPE = CustomAppEngPayload.createType("exported_grid_content");

    @Override
    public Type<ExportedGridContent> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ExportedGridContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ExportedGridContent::serialNumber,
            ContentType.STREAM_CODEC, ExportedGridContent::contentType,
            ByteBufCodecs.BYTE_ARRAY, ExportedGridContent::compressedData,
            ExportedGridContent::new);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private static final Logger LOG = LogManager.getLogger(ExportedGridContent.class);

    @Override
    public void handleOnClient(EntityPlayer player) {
        var saveDir = Minecraft.getMinecraft().gameDir.toPath();
        String filename;
        var spServer = Minecraft.getMinecraft().getIntegratedServer();
        var connection = Minecraft.getMinecraft().getConnection();
        if (spServer != null) {
            saveDir = spServer.getDataDirectory().toPath();
            filename = "ae2_grid_";
        } else if (connection != null) {
            filename = "ae2_grid_from_server_";
        } else {
            LOG.error("Ignoring grid export without a connection to a server.");
            return;
        }

        saveDir = saveDir.toAbsolutePath().normalize();

        filename += serialNumber + "_" + TIMESTAMP_FORMATTER.format(LocalDateTime.now()) + ".zip";

        OpenOption[] openOptions = new OpenOption[0];
        if (contentType != ContentType.FIRST_CHUNK) {
            openOptions = new OpenOption[] { StandardOpenOption.APPEND };
        }

        var tempPath = saveDir.resolve(filename + ".tmp");
        var finalPath = saveDir.resolve(filename);
        try (var out = Files.newOutputStream(tempPath, openOptions)) {
            out.write(compressedData);
        } catch (IOException e) {
            player.sendMessage(
                    new TextComponentString("Failed to write exported grid data to " + tempPath)
                            .setStyle(new Style().setColor(TextFormatting.RED)));
            LOG.error("Failed to write exported grid data to {}", tempPath, e);
            return;
        }

        if (contentType == ContentType.LAST_CHUNK) {
            try {
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOG.error("Failed to move grid export {} into place", finalPath, e);
            }

            player.sendMessage(new TextComponentString("Saved grid data for grid #" + serialNumber + " from server to ")
                    .appendSibling(new TextComponentString(finalPath.toString()).setStyle(new Style()
                            .setUnderlined(true)
                            .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                    finalPath.getParent().toString())))));
        }
    }

    public enum ContentType {
        FIRST_CHUNK,
        CHUNK,
        LAST_CHUNK;

        public static final StreamCodec<RegistryFriendlyByteBuf, ContentType> STREAM_CODEC = ByteBufCodecs
                .enumCodec(ContentType.class);
    }
}
