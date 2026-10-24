package appeng.api.orientation;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import appeng.block.BlockStateProperties;
import appeng.util.BlockPlaceContext;

/**
 * Extends {@link FacingStrategy} to also allow the block to be rotated around its facing axis.
 */
public class FacingWithSpinStrategy implements IOrientationStrategy {

    private final List<IProperty<?>> properties;

    protected FacingWithSpinStrategy() {
        this.properties = ImmutableList.of(
                BlockStateProperties.FACING,
                SPIN);
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        return state.getValue(BlockStateProperties.FACING);
    }

    @Override
    public int getSpin(IBlockState state) {
        return state.getValue(SPIN);
    }

    @Override
    public IBlockState setFacing(IBlockState state, EnumFacing facing) {
        return state.withProperty(BlockStateProperties.FACING, facing);
    }

    @Override
    public IBlockState setSpin(IBlockState state, int spin) {
        return state.withProperty(SPIN, spin);
    }

    @Override
    public IBlockState getStateForPlacement(IBlockState state, BlockPlaceContext context) {
        var up = EnumFacing.UP;
        var forward = context.getHorizontalDirection().getOpposite();
        var player = context.getPlayer();
        if (player != null) {
            if (player.rotationPitch > 65) {
                up = forward.getOpposite();
                forward = EnumFacing.UP;
            } else if (player.rotationPitch < -65) {
                up = forward.getOpposite();
                forward = EnumFacing.DOWN;
            }
        }

        return setOrientation(state, forward, up);
    }

    @Override
    public Collection<IProperty<?>> getProperties() {
        return properties;
    }
}
