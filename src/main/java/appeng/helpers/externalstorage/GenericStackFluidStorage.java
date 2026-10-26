package appeng.helpers.externalstorage;

import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import javax.annotation.Nullable;
import com.google.common.primitives.Ints;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKeyType;

/**
 * Exposes a {@link GenericInternalInventory} as the platforms external fluid storage interface.
 */
public class GenericStackFluidStorage implements IFluidHandler {
    private final GenericInternalInventory inv;

    public GenericStackFluidStorage(GenericInternalInventory inv) {
        this.inv = inv;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        var capacity = Ints.saturatedCast(inv.getCapacity(AEKeyType.fluids()));
        var result = new IFluidTankProperties[inv.size()];
        for (int tank = 0; tank < result.length; tank++) {
            FluidStack contents = null;
            if (inv.getKey(tank) instanceof AEFluidKey what) {
                contents = what.toStack(Ints.saturatedCast(inv.getAmount(tank)));
            }
            result[tank] = new FluidTankProperties(contents, capacity);
        }
        return result;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        var what = AEFluidKey.of(resource);
        if (what == null) {
            return 0;
        }

        int inserted = 0;
        for (int i = 0; i < inv.size() && inserted < resource.amount; ++i) {
            inserted += (int) inv.insert(i, what, resource.amount - inserted, Actionable.of(doFill));
        }
        return inserted;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        var what = AEFluidKey.of(resource);
        if (what == null) {
            return null;
        }

        return extract(what, resource.amount, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        // Find first fluid in tanks
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getKey(i) instanceof AEFluidKey fluidKey) {
                return extract(fluidKey, maxDrain, doDrain);
            }
        }
        return null;
    }

    @Nullable
    private FluidStack extract(AEFluidKey what, int amount, boolean doDrain) {
        int extracted = 0;
        for (int i = 0; i < inv.size() && extracted < amount; ++i) {
            extracted += (int) inv.extract(i, what, amount - extracted, Actionable.of(doDrain));
        }

        if (extracted > 0) {
            return what.toStack(extracted);
        }
        return null;
    }
}
