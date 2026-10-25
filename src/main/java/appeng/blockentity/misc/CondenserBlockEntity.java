/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.blockentity.misc;


import javax.annotation.Nullable;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import appeng.api.config.CondenserOutput;
import appeng.api.config.Settings;
import appeng.api.implementations.items.IStorageComponent;
import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.BlockEntityType;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemFilters;

public class CondenserBlockEntity extends AEBaseInvBlockEntity implements IConfigurableObject {

    public static final int BYTE_MULTIPLIER = 8;

    private final IConfigManager cm = IConfigManager.builder(() -> {
        saveChanges();
        addPower(0);
    })
            .registerSetting(Settings.CONDENSER_OUTPUT, CondenserOutput.TRASH)
            .build();

    private final AppEngInternalInventory outputSlot = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory storageSlot = new AppEngInternalInventory(this, 1);
    private final InternalInventory inputSlot = new CondenseItemHandler();
    private final IFluidHandler fluidHandler = new FluidHandler();

    /**
     * This is used to expose a fake ME subnetwork that is only composed of this condenser. The purpose of this is to
     * enable the condenser to override the {@link MEStorage#isPreferredStorageFor} method to make sure a condenser is
     * only ever used if an item can't go anywhere else.
     */
    private final CondenserMEStorage meStorage = new CondenserMEStorage(this);

    private final InternalInventory externalInv = new CombinedInternalInventory(this.inputSlot,
            new FilteredInternalInventory(this.outputSlot, AEItemFilters.EXTRACT_ONLY));
    private final InternalInventory combinedInv = new CombinedInternalInventory(this.inputSlot, this.outputSlot,
            this.storageSlot);

    private double storedPower = 0;

    public CondenserBlockEntity() {
        super();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.cm.writeToNBT(data);
        data.setDouble("storedPower", this.getStoredPower());
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.cm.readFromNBT(data);
        this.setStoredPower(data.getDouble("storedPower"));
    }

    public double getStorage() {
        final ItemStack is = this.storageSlot.getStackInSlot(0);
        if (!is.isEmpty() && is.getItem() instanceof IStorageComponent sc) {
            if (sc.isStorageComponent(is)) {
                return sc.getBytes(is) * BYTE_MULTIPLIER;
            }
        }
        return 0;
    }

    public void addPower(double rawPower) {
        this.setStoredPower(this.getStoredPower() + rawPower);
        this.setStoredPower(Math.max(0.0, Math.min(this.getStorage(), this.getStoredPower())));
        fillOutput();
    }

    private void fillOutput() {
        var requiredPower = this.getRequiredPower();
        var output = this.getOutput();
        if (requiredPower <= 0 || output.isEmpty()) {
            return;
        }

        // Produce as many items as the stored power allows and the output slot can hold in one go
        var power = this.getStoredPower();
        var amount = (int) Math.min(power / requiredPower, output.getMaxStackSize());
        if (amount <= 0) {
            return;
        }
        output.setCount(amount);
        amount -= this.outputSlot.insertItem(0, output, true).getCount();
        if (amount > 0) {
            this.setStoredPower(power - requiredPower * amount);
            output.setCount(amount);
            this.outputSlot.insertItem(0, output, false);
        }
    }

    boolean canAddOutput() {
        return this.outputSlot.insertItem(0, getOutput(), true).isEmpty();
    }

    InternalInventory getOutputSlot() {
        return this.outputSlot;
    }

    private ItemStack getOutput() {

        return switch (this.cm.getSetting(Settings.CONDENSER_OUTPUT)) {
            case MATTER_BALLS -> AEItems.MATTER_BALL.stack();
            case SINGULARITY -> AEItems.SINGULARITY.stack();
            default -> ItemStack.EMPTY;
        };
    }

    public double getRequiredPower() {
        return this.cm.getSetting(Settings.CONDENSER_OUTPUT).requiredPower;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.combinedInv;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (inv == outputSlot)
            fillOutput();
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    public double getStoredPower() {
        return this.storedPower;
    }

    private void setStoredPower(double storedPower) {
        this.storedPower = storedPower;
        // Stored power is not observable from the outside, so only mark the chunk as unsaved instead of using
        // markDirty(), which would also trigger neighbor updates.
        if (this.world != null) {
            this.world.markChunkDirty(this.getPos(), this);
        }
    }

    public InternalInventory getExternalInv() {
        return externalInv;
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public MEStorage getMEStorage() {
        return meStorage;
    }

    private class CondenseItemHandler extends BaseInternalInventory {
        @Override
        public int size() {
            // We only expose the void slot
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            // The void slot never has any content
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canAddOutput();
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (!stack.isEmpty()) {
                CondenserBlockEntity.this.addPower(stack.getCount());
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!canAddOutput()) {
                return stack;
            }
            if (!simulate && !stack.isEmpty()) {
                CondenserBlockEntity.this.addPower(stack.getCount());
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * A fluid handler that exposes a 1 bucket tank that can only be filled, and - when filled - will add power to this
     * condenser.
     */
    private class FluidHandler implements IFluidHandler {

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {
                    new FluidTankProperties(null, AEFluidKey.AMOUNT_BUCKET, true, false) };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            int amount = resource == null || resource.amount <= 0 ? 0
                    : Math.min(resource.amount, AEFluidKey.AMOUNT_BUCKET);

            if (doFill && amount > 0) {
                var what = AEFluidKey.of(resource);
                if (what != null) {
                    var transferFactor = (double) what.getAmountPerOperation();
                    CondenserBlockEntity.this.addPower(amount / transferFactor);
                }
            }

            return amount;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }
    }
}
