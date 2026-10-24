package appeng.parts.automation;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.capabilities.BlockCapability;
import appeng.api.capabilities.BlockCapabilityCache;
import appeng.api.capabilities.Capabilities;
import appeng.api.storage.MEStorage;

public class ForgeExternalStorageStrategy<T, S> implements ExternalStorageStrategy {
    private final BlockCapabilityCache<T, EnumFacing> cache;
    private final HandlerStrategy<T, S> conversion;

    public ForgeExternalStorageStrategy(BlockCapability<T, EnumFacing> capability,
            HandlerStrategy<T, S> conversion,
            WorldServer level,
            BlockPos fromPos,
            EnumFacing fromSide) {
        this.cache = BlockCapabilityCache.create(capability, level, fromPos, fromSide);
        this.conversion = conversion;
    }

    @Nullable
    @Override
    public MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback) {
        var storage = cache.getCapability();
        if (storage == null) {
            return null;
        }

        var result = conversion.getFacade(storage);
        result.setChangeListener(injectOrExtractCallback);
        result.setExtractableOnly(extractableOnly);
        return result;
    }

    public static ExternalStorageStrategy createItem(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        return new ForgeExternalStorageStrategy<>(
                Capabilities.ItemHandler.BLOCK,
                HandlerStrategy.ITEMS,
                level,
                fromPos,
                fromSide);
    }

    public static ExternalStorageStrategy createFluid(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        return new ForgeExternalStorageStrategy<>(
                Capabilities.FluidHandler.BLOCK,
                HandlerStrategy.FLUIDS,
                level,
                fromPos,
                fromSide);
    }
}
