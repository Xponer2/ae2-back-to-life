package appeng.core.definitions;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import appeng.block.AEBaseEntityBlock;

public final class DeferredBlockEntityType<T extends TileEntity> implements Supplier<BlockEntityType<T>> {
    private final Class<T> blockEntityClass;
    private final BlockEntityType<T> type;
    private final BlockDefinition<? extends AEBaseEntityBlock<?>>[] blockDefinitions;

    public DeferredBlockEntityType(Class<T> blockEntityClass, BlockEntityType<T> type,
            BlockDefinition<? extends AEBaseEntityBlock<?>>[] blockDefinitions) {
        this.blockEntityClass = blockEntityClass;
        this.type = type;
        this.blockDefinitions = blockDefinitions;
    }

    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    BlockDefinition<? extends AEBaseEntityBlock<?>>[] getBlockDefinitions() {
        return blockDefinitions;
    }

    @Override
    public BlockEntityType<T> get() {
        return type;
    }

    @Nullable
    public T getTileEntity(IBlockAccess level, BlockPos pos) {
        return type.getTileEntity(level, pos);
    }
}
