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

package appeng.worldgen.meteorite.fallout;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.worldgen.meteorite.MeteoriteBlockPutter;

public class Fallout {
    private final MeteoriteBlockPutter putter;
    private final IBlockState skyStone;
    protected final Random random;

    public Fallout(MeteoriteBlockPutter putter, IBlockState skyStone, Random random) {
        this.putter = putter;
        this.skyStone = skyStone;
        this.random = random;
    }

    public int adjustCrater() {
        return 0;
    }

    public void getRandomFall(World level, BlockPos pos) {
        var a = random.nextFloat();
        if (a > 0.9f) {
            this.putter.put(level, pos, Blocks.STONE.getDefaultState());
        } else if (a > 0.8f) {
            this.putter.put(level, pos, Blocks.COBBLESTONE.getDefaultState());
        } else if (a > 0.7f) {
            this.putter.put(level, pos, Blocks.DIRT.getDefaultState());
        } else {
            this.putter.put(level, pos, Blocks.GRAVEL.getDefaultState());
        }
    }

    public void getRandomInset(World level, BlockPos pos) {
        var a = random.nextFloat();
        if (a > 0.9f) {
            this.putter.put(level, pos, Blocks.COBBLESTONE.getDefaultState());
        } else if (a > 0.8f) {
            this.putter.put(level, pos, Blocks.STONE.getDefaultState());
        } else if (a > 0.7f) {
            this.putter.put(level, pos, Blocks.GRASS.getDefaultState());
        } else if (a > 0.6f) {
            this.putter.put(level, pos, this.skyStone);
        } else if (a > 0.5f) {
            this.putter.put(level, pos, Blocks.GRAVEL.getDefaultState());
        } else {
            this.putter.put(level, pos, Blocks.AIR.getDefaultState());
        }
    }
}
