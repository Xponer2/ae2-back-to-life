package appeng.api.orientation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import appeng.util.BlockPlaceContext;

/**
 * Implements a strategy that allows blocks to be oriented using a single directional property.
 */
public class FacingStrategy implements IOrientationStrategy {
    private final PropertyDirection property;
    private final List<IProperty<?>> properties;
    private final boolean allowsPlayerRotation;

    protected FacingStrategy(PropertyDirection property) {
        this(property, true);
    }

    protected FacingStrategy(PropertyDirection property, boolean allowsPlayerRotation) {
        this.property = property;
        this.properties = Collections.singletonList(property);
        this.allowsPlayerRotation = allowsPlayerRotation;
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        return state.getValue(property);
    }

    @Override
    public IBlockState setFacing(IBlockState state, EnumFacing facing) {
        if (!property.getAllowedValues().contains(facing)) {
            return state;
        }
        return state.withProperty(property, facing);
    }

    @Override
    public IBlockState getStateForPlacement(IBlockState state, BlockPlaceContext context) {
        return setFacing(state, context.getClickedFace());
    }

    @Override
    public boolean allowsPlayerRotation() {
        return allowsPlayerRotation;
    }

    @Override
    public Collection<IProperty<?>> getProperties() {
        return properties;
    }
}
