package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.helpers.IMouseWheelItem;

public final class MouseWheelPacket implements ServerboundPacket {
    private final boolean wheelUp;

    public MouseWheelPacket(boolean wheelUp) {
        this.wheelUp = wheelUp;
    }

    public boolean wheelUp() {
        return wheelUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MouseWheelPacket)) return false;
        MouseWheelPacket that = (MouseWheelPacket) o;
        return wheelUp == that.wheelUp;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(wheelUp);
    }

    @Override
    public String toString() {
        return "MouseWheelPacket[" + "wheelUp=" + wheelUp + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MouseWheelPacket> STREAM_CODEC = StreamCodec.ofMember(
            MouseWheelPacket::write,
            MouseWheelPacket::decode);

    public static final Type<MouseWheelPacket> TYPE = CustomAppEngPayload.createType("mouse_wheel");

    @Override
    public Type<MouseWheelPacket> type() {
        return TYPE;
    }

    public static MouseWheelPacket decode(RegistryFriendlyByteBuf byteBuf) {
        var wheelUp = byteBuf.readBoolean();
        return new MouseWheelPacket(wheelUp);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBoolean(wheelUp);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        var mainHand = player.getHeldItem(EnumHand.MAIN_HAND);
        var offHand = player.getHeldItem(EnumHand.OFF_HAND);

        if (mainHand.getItem() instanceof IMouseWheelItem mouseWheelItem) {
            mouseWheelItem.onWheel(mainHand, wheelUp);
        } else if (offHand.getItem() instanceof IMouseWheelItem mouseWheelItem) {
            mouseWheelItem.onWheel(offHand, wheelUp);
        }
    }
}
