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

package appeng.block.networking;

import appeng.core.CreativeTabOutput;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import appeng.api.ids.AEComponents;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.networking.EnergyCellBlockEntity;

public class EnergyCellBlock extends AEBaseEntityBlock<EnergyCellBlockEntity> {

    public static final int MAX_FULLNESS = 4;

    public static final PropertyInteger ENERGY_STORAGE = PropertyInteger.create("fullness", 0, MAX_FULLNESS);

    private final double maxPower;
    private final double chargeRate;
    private final int priority;

    public EnergyCellBlock(double maxPower, double chargeRate, int priority) {
        super(glassProps());
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.priority = priority;
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        super.addToMainCreativeTab(parameters, output);

        var charged = new ItemStack(this, 1);
        AEComponents.STORED_ENERGY.set(charged, getMaxPower());

        output.accept(charged);
    }

    public double getMaxPower() {
        return this.maxPower;
    }

    public double getChargeRate() {
        return this.chargeRate;
    }

    public int getPriority() {
        return this.priority;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ENERGY_STORAGE);
    }

    @Override
    public boolean hasAnalogOutputSignal(IBlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockState state, World level, BlockPos pos) {
        var cell = getTileEntity(level, pos);
        if (cell != null) {
            var currentPower = cell.getAECurrentPower();
            var maxPower = cell.getAEMaxPower();
            var fillFactor = currentPower / maxPower;
            return MathHelper.floor(fillFactor * 14.0F) + (currentPower > 0 ? 1 : 0);
        }
        return 0;
    }
}
