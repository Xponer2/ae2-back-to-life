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

package appeng.spatial;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import appeng.core.definitions.AEBlocks;

/**
 * Fills every chunk of the spatial storage dimension with matrix frames, like the 1.21 generator.
 */
public class SpatialStorageChunkGenerator implements IChunkGenerator {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;

    private final World level;

    public SpatialStorageChunkGenerator(World level) {
        this.level = level;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        var primer = new ChunkPrimer();
        var defaultBlockState = AEBlocks.MATRIX_FRAME.block().getDefaultState();
        for (int cx = 0; cx < 16; cx++) {
            for (int cz = 0; cz < 16; cz++) {
                for (int cy = MIN_Y; cy < HEIGHT; cy++) {
                    primer.setBlockState(cx, cy, cz, defaultBlockState);
                }
            }
        }

        var chunk = new Chunk(this.level, primer, x, z);
        var biomeId = (byte) Biome.getIdForBiome(level.provider.getBiomeForCoords(BlockPos.ORIGIN));
        var biomes = chunk.getBiomeArray();
        for (int i = 0; i < biomes.length; i++) {
            biomes[i] = biomeId;
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
    }

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, String structureName, BlockPos position,
            boolean findUnexplored) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
    }

    @Override
    public boolean isInsideStructure(World world, String structureName, BlockPos pos) {
        return false;
    }
}
