package appeng.api.orientation;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.block.properties.IProperty;

class NoOrientationStrategy implements IOrientationStrategy {
    @Override
    public Collection<IProperty<?>> getProperties() {
        return Collections.emptyList();
    }
}
