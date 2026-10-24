/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.implementations.blockentities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;

/**
 * Provides crafting services to adjacent pattern providers for automatic crafting. Can be provided via capability on
 * your block entity.
 */
public interface ICraftingMachine {

    @Nullable
    static ICraftingMachine of(@Nullable TileEntity blockEntity, EnumFacing side) {
        if (blockEntity == null || blockEntity.getWorld() == null) {
            return null;
        }

        return AECapabilities.CRAFTING_MACHINE.getCapability(blockEntity.getLevel(),  blockEntity.getPos(), blockEntity.getBlockState(),
                blockEntity, side);
    }

    @Nullable
    static ICraftingMachine of(World level, BlockPos pos, EnumFacing side) {
        return AECapabilities.CRAFTING_MACHINE.getCapability(level,  pos, side);
    }

    /**
     * Describe how this machine is displayed and grouped in the pattern access terminal.
     */
    PatternContainerGroup getCraftingMachineInfo();

    /**
     * inserts a crafting plan, and the necessary items into the crafting machine.
     *
     * @param inputs The crafting ingredients. The array layout corresponds to {@link IPatternDetails#getInputs()} of
     *               <code>patternDetails</code>.
     * @return if it was accepted, all or nothing.
     */
    boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, EnumFacing ejectionDirection);

    /**
     * check if the crafting machine is accepting pushes via pushPattern, if this is false, all calls to push will fail,
     * you can try inserting into the inventory instead.
     *
     * @return true, if pushPattern can complete, if its false push will always be false.
     */
    boolean acceptsPlans();
}
