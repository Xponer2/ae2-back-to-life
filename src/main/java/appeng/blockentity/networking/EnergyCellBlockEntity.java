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

package appeng.blockentity.networking;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import appeng.api.components.DataComponentMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.ids.AEComponents;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.events.GridPowerStorageStateChanged;
import appeng.api.networking.events.GridPowerStorageStateChanged.PowerEventType;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.block.networking.EnergyCellBlock;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.BlockEntityType;
import appeng.me.energy.StoredEnergyAmount;
import appeng.util.Platform;
import appeng.util.SettingsFrom;

public class EnergyCellBlockEntity extends AENetworkedBlockEntity implements IAEPowerStorage, IGridTickable {

    private final StoredEnergyAmount stored;
    private byte currentDisplayLevel;
    private boolean neighborChangePending;

    public EnergyCellBlockEntity() {
        super();
        this.getMainNode()
                .setIdlePowerUsage(0)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this);

        var cellBlock = (EnergyCellBlock) getBlockState().getBlock();
        this.stored = new StoredEnergyAmount(0, cellBlock.getMaxPower(), this::emitPowerEvent);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.COVERED;
    }

    @Override
    public void onReady() {
        super.onReady();
        final int value = this.world.getBlockState(this.pos).getValue(EnergyCellBlock.ENERGY_STORAGE);
        this.currentDisplayLevel = (byte) value;
        this.updateStateForPowerLevel();
    }

    /**
     * Given a fill factor, return the storage level used for the state of the block. This is also used for determining
     * the item model.
     */
    public static int getStorageLevelFromFillFactor(double fillFactor) {
        return (int) Math.floor(EnergyCellBlock.MAX_FULLNESS * MathHelper.clamp(fillFactor + 0.01, 0, 1));
    }

    /**
     * Updates the block state of this cell so that it matches the power level.
     */
    private void updateStateForPowerLevel() {
        if (this.isRemoved()) {
            return;
        }

        int storageLevel = getStorageLevelFromFillFactor(this.stored.getAmount() / this.stored.getMaximum());

        if (this.currentDisplayLevel != storageLevel) {
            this.currentDisplayLevel = (byte) storageLevel;
            setBlockStateProperty(EnergyCellBlock.ENERGY_STORAGE, storageLevel);
        }
    }

    /**
     * Marks the current chunk as unsaved, without creating additional loading tickets that would potentially keep this
     * block loaded forever.
     */
    private void setChangedNoTicketUpdate() {
        if (this.world != null) {
            this.world.markChunkDirty(this.pos, this);
        }
    }

    private void onAmountChanged() {
        setChangedNoTicketUpdate();

        // Delay the notification since this happens while energy is being extracted/injected from the grid.
        // During injection/extraction, the grid should not be modified.
        // So defer the notification until the next time this cell is in a ticking chunk.
        if (!neighborChangePending) {
            neighborChangePending = true;
            getMainNode().ifPresent((grid, node) -> {
                grid.getTickManager().alertDevice(node);
            });
        }
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setDouble("internalCurrentPower", this.stored.getAmount());
        data.setBoolean("neighborChangePending", this.neighborChangePending);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.stored.setStored(data.getDouble("internalCurrentPower"));
        this.neighborChangePending = data.getBoolean("neighborChangePending");
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable EntityPlayer player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var storedEnergy = AEComponents.STORED_ENERGY.get(input);
            if (storedEnergy != null) {
                this.stored.setStored(storedEnergy);
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom from, DataComponentMap.Builder data, @Nullable EntityPlayer player) {
        super.exportSettings(from, data, player);

        if (from == SettingsFrom.DISMANTLE_ITEM && stored.getAmount() > 0) {
            AEComponents.STORED_ENERGY.set(data, stored.getAmount());
        }
    }

    @Override
    public final double injectAEPower(double amt, Actionable mode) {
        var inserted = this.stored.insert(amt, mode == Actionable.MODULATE);
        if (mode == Actionable.MODULATE && inserted > 0) {
            this.onAmountChanged();
        }
        return amt - inserted;
    }

    @Override
    public final double extractAEPower(double amt, Actionable mode, PowerMultiplier pm) {
        return pm.divide(this.extractAEPower(pm.multiply(amt), mode));
    }

    private double extractAEPower(double amt, Actionable mode) {
        var extracted = this.stored.extract(amt, mode == Actionable.MODULATE);
        if (mode == Actionable.MODULATE && extracted > 0) {
            this.onAmountChanged();
        }
        return extracted;
    }

    @Override
    public double getAEMaxPower() {
        return this.stored.getMaximum();
    }

    @Override
    public double getAECurrentPower() {
        return this.stored.getAmount();
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return true;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public int getPriority() {
        return ((EnergyCellBlock) getBlockState().getBlock()).getPriority();
    }

    private void emitPowerEvent(PowerEventType type) {
        getMainNode().ifPresent(
                grid -> grid.postEvent(new GridPowerStorageStateChanged(this, type)));
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, !neighborChangePending);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (Platform.areBlockEntitiesTicking(getLevel(), getPos())) {
            if (neighborChangePending) {
                neighborChangePending = false;
                markDirty(); // update comparators
                updateStateForPowerLevel(); // and update block state
            }
            return TickRateModulation.SLEEP;
        } else {
            return TickRateModulation.IDLE;
        }
    }
}
