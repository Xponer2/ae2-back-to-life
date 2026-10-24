package appeng.parts.automation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.capabilities.BlockCapability;
import appeng.api.capabilities.BlockCapabilityCache;
import appeng.api.capabilities.Capabilities;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;

public class StorageExportStrategy<T, S> implements StackExportStrategy {
    private static final Logger LOG = LogManager.getLogger(StorageExportStrategy.class);
    private final BlockCapabilityCache<T, EnumFacing> cache;
    private final HandlerStrategy<T, S> handlerStrategy;

    public StorageExportStrategy(BlockCapability<T, EnumFacing> capability,
            HandlerStrategy<T, S> handlerStrategy,
            WorldServer level,
            BlockPos fromPos,
            EnumFacing fromSide) {
        this.handlerStrategy = handlerStrategy;
        this.cache = BlockCapabilityCache.create(capability, level, fromPos, fromSide);
    }

    @Override
    public long transfer(StackTransferContext context, AEKey what, long amount) {
        if (!handlerStrategy.isSupported(what)) {
            return 0;
        }

        var adjacentStorage = cache.getCapability();
        if (adjacentStorage == null) {
            return 0;
        }

        var inv = context.getInternalStorage();

        var extracted = StorageHelper.poweredExtraction(
                context.getEnergySource(),
                inv.getInventory(),
                what,
                amount,
                context.getActionSource(),
                Actionable.SIMULATE);

        long wasInserted = handlerStrategy.insert(adjacentStorage, what, extracted, Actionable.SIMULATE);

        if (wasInserted > 0) {
            extracted = StorageHelper.poweredExtraction(
                    context.getEnergySource(),
                    inv.getInventory(),
                    what,
                    wasInserted,
                    context.getActionSource(),
                    Actionable.MODULATE);

            wasInserted = handlerStrategy.insert(adjacentStorage, what, extracted, Actionable.MODULATE);

            if (wasInserted < extracted) {
                // Be nice and try to give the overflow back
                long leftover = extracted - wasInserted;
                leftover -= inv.getInventory().insert(what, leftover, Actionable.MODULATE, context.getActionSource());
                if (leftover > 0) {
                    LOG.error("Storage export: adjacent block unexpectedly refused insert, voided {}x{}", leftover,
                            what);
                }
            }
        }

        return wasInserted;
    }

    @Override
    public long push(AEKey what, long amount, Actionable mode) {
        if (!handlerStrategy.isSupported(what)) {
            return 0;
        }

        var adjacentStorage = cache.getCapability();
        if (adjacentStorage == null) {
            return 0;
        }

        return handlerStrategy.insert(adjacentStorage, what, amount, mode);
    }

    public static StackExportStrategy createItem(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        return new StorageExportStrategy<>(
                Capabilities.ItemHandler.BLOCK,
                HandlerStrategy.ITEMS,
                level,
                fromPos,
                fromSide);
    }

    public static StackExportStrategy createFluid(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        return new StorageExportStrategy<>(
                Capabilities.FluidHandler.BLOCK,
                HandlerStrategy.FLUIDS,
                level,
                fromPos,
                fromSide);
    }
}
