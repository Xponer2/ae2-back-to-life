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

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import appeng.worldgen.meteorite.MeteoriteBlockPutter;

public class FalloutCopy extends Fallout {
    private static final float SPECIFIED_BLOCK_THRESHOLD = 0.9f;
    private static final float AIR_BLOCK_THRESHOLD = 0.8f;
    private static final float BLOCK_THRESHOLD_STEP = 0.1f;

    private final IBlockState block;
    private final MeteoriteBlockPutter putter;

    public FalloutCopy(World level, BlockPos pos, MeteoriteBlockPutter putter,
            IBlockState skyStone, Random random) {
        super(putter, skyStone, random);
        this.putter = putter;
        var biome = level.getBiome(pos);
        // 1.21 biome tags -> Forge biome dictionary types
        if (BiomeDictionary.hasType(biome, Type.MESA)) {
            block = Blocks.HARDENED_CLAY.getDefaultState();
        } else if (BiomeDictionary.hasType(biome, Type.SNOWY)) {
            block = Blocks.SNOW.getDefaultState();
        } else if (BiomeDictionary.hasType(biome, Type.BEACH) || BiomeDictionary.hasType(biome, Type.SANDY)) {
            block = Blocks.SAND.getDefaultState();
        } else if (BiomeDictionary.hasType(biome, Type.PLAINS) || BiomeDictionary.hasType(biome, Type.FOREST)) {
            block = Blocks.DIRT.getDefaultState();
        } else {
            block = Blocks.COBBLESTONE.getDefaultState();
        }
    }

    @Override
    public void getRandomFall(World level, BlockPos pos) {
        var a = random.nextFloat();
        if (a > SPECIFIED_BLOCK_THRESHOLD) {
            this.putter.put(level, pos, this.block);
        } else {
            this.getOther(level, pos, a);
        }
    }

    public void getOther(World level, BlockPos pos, float a) {

    }

    @Override
    public void getRandomInset(World level, BlockPos pos) {
        var a = random.nextFloat();
        if (a > SPECIFIED_BLOCK_THRESHOLD) {
            this.putter.put(level, pos, this.block);
        } else if (a > AIR_BLOCK_THRESHOLD) {
            this.putter.put(level, pos, Blocks.AIR.getDefaultState());
        } else {
            this.getOther(level, pos, a - BLOCK_THRESHOLD_STEP);
        }
    }
}
