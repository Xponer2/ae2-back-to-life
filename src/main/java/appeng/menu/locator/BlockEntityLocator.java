package appeng.menu.locator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import appeng.core.AELog;
import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Locates a {@link net.minecraft.world.level.block.entity.BlockEntity} that hosts a menu.
 */
final class BlockEntityLocator implements MenuHostLocator {
    private final BlockPos pos;

    public BlockEntityLocator(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockEntityLocator)) return false;
        BlockEntityLocator that = (BlockEntityLocator) o;
        return java.util.Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pos);
    }

    public void writeToPacket(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static BlockEntityLocator readFromPacket(RegistryFriendlyByteBuf buf) {
        return new BlockEntityLocator(buf.readBlockPos());
    }

    @Override
    public <T> @Nullable T locate(EntityPlayer player, Class<T> hostInterface) {
        var blockEntity = player.world.getTileEntity(pos);

        if (hostInterface.isInstance(blockEntity)) {
            return hostInterface.cast(blockEntity);
        } else if (blockEntity != null) {
            AELog.warn("Cannot locate menu host @ %s, %s does not implement %s",
                    pos, blockEntity, hostInterface);
        }

        return null;
    }

    @Override
    public String toString() {
        return "BlockEntity{pos=" + pos + '}';
    }
}
