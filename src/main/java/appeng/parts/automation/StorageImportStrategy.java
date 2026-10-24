package appeng.parts.automation;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.capabilities.BlockCapability;
import appeng.api.capabilities.BlockCapabilityCache;
import appeng.api.capabilities.Capabilities;
import appeng.api.config.Actionable;
import appeng.core.AELog;

/**
 * Strategy for efficiently importing stacks from external storage into an internal
 * {@link appeng.api.storage.MEStorage}.
 */
public class StorageImportStrategy<T, S> implements StackImportStrategy {
    private final BlockCapabilityCache<T, EnumFacing> cache;
    private final HandlerStrategy<T, S> conversion;

    public StorageImportStrategy(BlockCapability<T, EnumFacing> capability,
            HandlerStrategy<T, S> conversion,
            WorldServer level,
            BlockPos fromPos,
            EnumFacing fromSide) {
        this.cache = BlockCapabilityCache.create(capability, level, fromPos, fromSide);
        this.conversion = conversion;
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(conversion.getKeyType())) {
            return false;
        }

        var adjacentHandler = cache.getCapability();
        if (adjacentHandler == null) {
            return false;
        }

        var adjacentStorage = conversion.getFacade(adjacentHandler);

        long remainingTransferAmount = context.getOperationsRemaining()
                * (long) conversion.getKeyType().getAmountPerOperation();

        var inv = context.getInternalStorage();

        // Try to find an extractable resource that fits our filter
        for (int i = 0; i < adjacentStorage.getSlots() && remainingTransferAmount > 0; i++) {
            var resource = adjacentStorage.getStackInSlot(i);
            if (resource == null
                    // Regard a filter that is set on the bus
                    || context.isInFilter(resource.what()) == context.isInverted()) {
                continue;
            }

            // Check how much of *this* resource we can actually insert into the network, it might be 0
            // if the cells are partitioned or there's not enough types left, etc.
            var amountForThisResource = inv.getInventory().insert(resource.what(), remainingTransferAmount,
                    Actionable.SIMULATE,
                    context.getActionSource());

            // Try to simulate-extract it
            var amount = adjacentStorage.extract(resource.what(), amountForThisResource, Actionable.MODULATE,
                    context.getActionSource());
            if (amount > 0) {
                var inserted = inv.getInventory().insert(resource.what(), amount, Actionable.MODULATE,
                        context.getActionSource());

                if (inserted < amount) {
                    // Be nice and try to give the overflow back
                    long leftover = amount - inserted;
                    leftover -= adjacentStorage.insert(resource.what(), leftover, Actionable.MODULATE,
                            context.getActionSource());
                    if (leftover > 0) {
                        AELog.warn("Extracted %dx%s from adjacent storage and voided it because network refused insert",
                                leftover, resource.what());
                    }
                }

                var opsUsed = Math.max(1, inserted / conversion.getKeyType().getAmountPerOperation());
                context.reduceOperationsRemaining(opsUsed);
                remainingTransferAmount -= inserted;
            }
        }

        return false;
    }

    public static StackImportStrategy createItem(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        return new StorageImportStrategy<>(
                Capabilities.ItemHandler.BLOCK,
                HandlerStrategy.ITEMS,
                level,
                fromPos,
                fromSide);
    }

    public static StackImportStrategy createFluid(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        return new StorageImportStrategy<>(
                Capabilities.FluidHandler.BLOCK,
                HandlerStrategy.FLUIDS,
                level,
                fromPos,
                fromSide);
    }
}
