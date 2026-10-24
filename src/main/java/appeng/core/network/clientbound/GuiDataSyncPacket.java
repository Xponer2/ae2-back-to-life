
package appeng.core.network.clientbound;

import java.util.function.Consumer;

import io.netty.buffer.Unpooled;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.AEBaseMenu;

/**
 * This packet is used to synchronize menu-fields from server to client.
 */
public final class GuiDataSyncPacket implements ClientboundPacket {
    private final int containerId;
    private final byte[] syncData;

    public GuiDataSyncPacket(int containerId, byte[] syncData) {
        this.containerId = containerId;
        this.syncData = syncData;
    }

    public int containerId() {
        return containerId;
    }

    public byte[] syncData() {
        return syncData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuiDataSyncPacket)) return false;
        GuiDataSyncPacket that = (GuiDataSyncPacket) o;
        return containerId == that.containerId
                && java.util.Objects.equals(syncData, that.syncData);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, syncData);
    }

    @Override
    public String toString() {
        return "GuiDataSyncPacket[" + "containerId=" + containerId + ", " + "syncData=" + syncData + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, GuiDataSyncPacket> STREAM_CODEC = StreamCodec.ofMember(
            GuiDataSyncPacket::write,
            GuiDataSyncPacket::decode);

    public static final Type<GuiDataSyncPacket> TYPE = CustomAppEngPayload.createType("");

    @Override
    public Type<GuiDataSyncPacket> type() {
        return TYPE;
    }

    public GuiDataSyncPacket(int containerId, Consumer<RegistryFriendlyByteBuf> writer) {
        this(containerId, createSyncData(writer));
    }

    private static byte[] createSyncData(Consumer<RegistryFriendlyByteBuf> writer) {
        var buffer = RegistryFriendlyByteBuf.create();
        writer.accept(buffer);
        var result = new byte[buffer.readableBytes()];
        buffer.readBytes(result);
        return result;
    }

    public static GuiDataSyncPacket decode(RegistryFriendlyByteBuf data) {
        var containerId = data.readVarInt();
        var syncData = data.readByteArray();
        return new GuiDataSyncPacket(containerId, syncData);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeVarInt(containerId);
        data.writeByteArray(syncData);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        Container c = player.openContainer;
        if (c instanceof AEBaseMenu baseMenu && c.windowId == this.containerId) {
            baseMenu.receiveServerSyncData(
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(this.syncData)));
        }
    }

}
