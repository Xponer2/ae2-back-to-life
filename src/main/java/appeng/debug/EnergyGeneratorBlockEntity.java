/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.debug;

import com.google.common.math.IntMath;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.IEnergyStorage;

import appeng.api.capabilities.Capabilities;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.core.definitions.BlockEntityType;

public class EnergyGeneratorBlockEntity extends AEBaseBlockEntity implements ServerTickingBlockEntity, IEnergyStorage {
    /**
     * The base energy injected each tick. Adjacent energy generators will increase it to pow(base, #generators).
     */
    private int generationRate = 8;

    public EnergyGeneratorBlockEntity() {
        super();
    }

    @Override
    public void serverTick() {
        World level = this.getLevel();

        int tier = 1;
        for (EnumFacing facing : EnumFacing.VALUES) {
            final TileEntity te = level.getTileEntity(this.getPos().offset(facing));

            if (te instanceof EnergyGeneratorBlockEntity) {
                tier++;
            }
        }

        final int energyToInsert = IntMath.pow(generationRate, tier);

        for (EnumFacing facing : EnumFacing.VALUES) {
            var consumer = Capabilities.EnergyStorage.BLOCK.getCapability(getLevel(),  getPos().offset(facing),
                    facing.getOpposite());
            if (consumer != null && consumer.canReceive()) {
                consumer.receiveEnergy(energyToInsert, false);
            }
        }
    }

    public int getGenerationRate() {
        return generationRate;
    }

    public void setGenerationRate(int generationRate) {
        this.generationRate = generationRate;
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        if (data.hasKey("generationRate", Constants.NBT.TAG_INT)) {
            generationRate = data.getInteger("generationRate");
        }
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setInteger("generationRate", generationRate);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return maxExtract;
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
