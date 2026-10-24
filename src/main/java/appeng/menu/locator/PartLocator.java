package appeng.menu.locator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import appeng.api.parts.PartHelper;
import appeng.core.AELog;
import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Locates a part on an {@link appeng.api.parts.IPartHost} using the position of its host and the side it's attached to.
 */
final class PartLocator implements MenuHostLocator {
    private final BlockPos pos;
    private final EnumFacing side;

    public PartLocator(BlockPos pos, @Nullable EnumFacing side) {
        this.pos = pos;
        this.side = side;
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable public EnumFacing side() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartLocator)) return false;
        PartLocator that = (PartLocator) o;
        return java.util.Objects.equals(pos, that.pos)
                && java.util.Objects.equals(side, that.side);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pos, side);
    }

    @Override
    public String toString() {
        return "PartLocator[" + "pos=" + pos + ", " + "side=" + side + "]";
    }

    @Override
    public <T> @Nullable T locate(EntityPlayer player, Class<T> hostInterface) {
        var part = PartHelper.getPart(player.world, pos, side);
        if (hostInterface.isInstance(part)) {
            return hostInterface.cast(part);
        } else if (part != null) {
            AELog.warn("Part at %s does not implement host interface %s",
                    part, hostInterface);
        }

        return null;
    }

    public void writeToPacket(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(side != null);
        if (side != null) {
            buf.writeByte(side.ordinal());
        }
    }

    public static PartLocator readFromPacket(RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        EnumFacing side = null;
        if (buf.readBoolean()) {
            side = EnumFacing.VALUES[buf.readByte()];
        }
        return new PartLocator(pos, side);
    }
}
