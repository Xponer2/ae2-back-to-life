package appeng.api.orientation;

import net.minecraft.block.state.IBlockState;

import appeng.block.BlockStateProperties;
import appeng.util.BlockPlaceContext;

/**
 * Implements a strategy that allows blocks to be oriented using a single directional property. It doesn't allow up and
 * down, and uses the player facing instead in those cases.
 */
public class HorizontalFacingStrategy extends FacingStrategy {
    protected HorizontalFacingStrategy() {
        super(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public IBlockState getStateForPlacement(IBlockState state, BlockPlaceContext context) {
        return setFacing(state, context.getHorizontalDirection().getOpposite());
    }
}
