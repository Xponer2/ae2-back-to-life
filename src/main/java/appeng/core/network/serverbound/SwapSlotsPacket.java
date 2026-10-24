
package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.AEBaseMenu;

public final class SwapSlotsPacket implements ServerboundPacket {
    private final int slotA;
    private final int slotB;

    public SwapSlotsPacket(int slotA, int slotB) {
        this.slotA = slotA;
        this.slotB = slotB;
    }

    public int slotA() {
        return slotA;
    }

    public int slotB() {
        return slotB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SwapSlotsPacket)) return false;
        SwapSlotsPacket that = (SwapSlotsPacket) o;
        return slotA == that.slotA
                && slotB == that.slotB;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(slotA, slotB);
    }

    @Override
    public String toString() {
        return "SwapSlotsPacket[" + "slotA=" + slotA + ", " + "slotB=" + slotB + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapSlotsPacket> STREAM_CODEC = StreamCodec.ofMember(
            SwapSlotsPacket::write,
            SwapSlotsPacket::decode);

    public static final Type<SwapSlotsPacket> TYPE = CustomAppEngPayload.createType("swap_slots");

    @Override
    public Type<SwapSlotsPacket> type() {
        return TYPE;
    }

    public static SwapSlotsPacket decode(RegistryFriendlyByteBuf stream) {
        var slotA = stream.readInt();
        var slotB = stream.readInt();
        return new SwapSlotsPacket(slotA, slotB);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeInt(slotA);
        data.writeInt(slotB);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player != null && player.openContainer instanceof AEBaseMenu) {
            ((AEBaseMenu) player.openContainer).swapSlotContents(this.slotA, this.slotB);
        }
    }
}
