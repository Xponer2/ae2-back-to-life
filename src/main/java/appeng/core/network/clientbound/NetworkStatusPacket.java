
package appeng.core.network.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.me.networktool.NetworkStatusScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.me.networktool.NetworkStatus;

public final class NetworkStatusPacket implements ClientboundPacket {
    private final NetworkStatus status;

    public NetworkStatusPacket(NetworkStatus status) {
        this.status = status;
    }

    public NetworkStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkStatusPacket)) return false;
        NetworkStatusPacket that = (NetworkStatusPacket) o;
        return java.util.Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status);
    }

    @Override
    public String toString() {
        return "NetworkStatusPacket[" + "status=" + status + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkStatusPacket> STREAM_CODEC = StreamCodec.ofMember(
            NetworkStatusPacket::write,
            NetworkStatusPacket::decode);

    public static final Type<NetworkStatusPacket> TYPE = CustomAppEngPayload.createType("network_status");

    @Override
    public Type<NetworkStatusPacket> type() {
        return TYPE;
    }

    public static NetworkStatusPacket decode(RegistryFriendlyByteBuf data) {
        var status = NetworkStatus.read(data);
        return new NetworkStatusPacket(status);
    }

    public void write(RegistryFriendlyByteBuf data) {
        status.write(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        final GuiScreen gs = Minecraft.getMinecraft().currentScreen;

        if (gs instanceof NetworkStatusScreen) {
            ((NetworkStatusScreen) gs).processServerUpdate(status);
        }
    }

}
