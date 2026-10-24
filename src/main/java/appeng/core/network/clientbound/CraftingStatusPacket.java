
package appeng.core.network.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.me.crafting.CraftingStatus;

public final class CraftingStatusPacket implements ClientboundPacket {
    private final int containerId;
    private final CraftingStatus status;

    public CraftingStatusPacket(int containerId, CraftingStatus status) {
        this.containerId = containerId;
        this.status = status;
    }

    public int containerId() {
        return containerId;
    }

    public CraftingStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftingStatusPacket)) return false;
        CraftingStatusPacket that = (CraftingStatusPacket) o;
        return containerId == that.containerId
                && java.util.Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, status);
    }

    @Override
    public String toString() {
        return "CraftingStatusPacket[" + "containerId=" + containerId + ", " + "status=" + status + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingStatusPacket> STREAM_CODEC = StreamCodec.ofMember(
            CraftingStatusPacket::write,
            CraftingStatusPacket::decode);

    public static final Type<CraftingStatusPacket> TYPE = CustomAppEngPayload.createType("crafting_status");

    @Override
    public Type<CraftingStatusPacket> type() {
        return TYPE;
    }

    public static CraftingStatusPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CraftingStatusPacket(
                buffer.readInt(),
                CraftingStatus.read(buffer));
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeInt(containerId);
        status.write(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        if (player.openContainer == null || player.openContainer.windowId != containerId) {
            return; // Packet received for an invalid container id, i.e. after closing it client-side
        }

        GuiScreen screen = Minecraft.getMinecraft().currentScreen;

        if (screen instanceof CraftingCPUScreen<?> cpuScreen) {
            cpuScreen.postUpdate(this.status);
        }
    }

}
