package appeng.core.network.serverbound;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.implementations.PatternAccessTermMenu;

/**
 * Used for the pattern access terminal when the client shift-clicks an item in the player inventory.
 */
public final class QuickMovePatternPacket implements ServerboundPacket {
    private final int containerId;
    private final int clickedSlot;
    private final List<Long> allowedPatternContainers;

    public QuickMovePatternPacket(int containerId, int clickedSlot, List<Long> allowedPatternContainers) {
        this.containerId = containerId;
        this.clickedSlot = clickedSlot;
        this.allowedPatternContainers = allowedPatternContainers;
    }

    public int containerId() {
        return containerId;
    }

    public int clickedSlot() {
        return clickedSlot;
    }

    public List<Long> allowedPatternContainers() {
        return allowedPatternContainers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuickMovePatternPacket)) return false;
        QuickMovePatternPacket that = (QuickMovePatternPacket) o;
        return containerId == that.containerId
                && clickedSlot == that.clickedSlot
                && java.util.Objects.equals(allowedPatternContainers, that.allowedPatternContainers);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, clickedSlot, allowedPatternContainers);
    }

    @Override
    public String toString() {
        return "QuickMovePatternPacket[" + "containerId=" + containerId + ", " + "clickedSlot=" + clickedSlot + ", " + "allowedPatternContainers=" + allowedPatternContainers + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickMovePatternPacket> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.VAR_INT,
                    QuickMovePatternPacket::containerId,
                    ByteBufCodecs.VAR_INT,
                    QuickMovePatternPacket::clickedSlot,
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()),
                    QuickMovePatternPacket::allowedPatternContainers,
                    QuickMovePatternPacket::new);
    public static final Type<QuickMovePatternPacket> TYPE = CustomAppEngPayload.createType("quick_move_pattern");

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer.windowId == containerId
                && player.openContainer instanceof PatternAccessTermMenu menu) {
            menu.quickMovePattern(player, clickedSlot, allowedPatternContainers);
        }
    }

    @Override
    public Type<QuickMovePatternPacket> type() {
        return TYPE;
    }
}
