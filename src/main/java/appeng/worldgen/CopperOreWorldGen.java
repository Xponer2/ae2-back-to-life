/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.worldgen;

import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import appeng.core.AEConfig;
import appeng.core.definitions.AEBlocks;

/**
 * AE2's own copper ore (vanilla 1.12.2 has no copper). Like the 1.21 vanilla copper ore
 * scaled to the 1.12.2 height: veins of up to 10 blocks, 16 tries per chunk, y 0..96 with a triangular distribution
 * peaking at y 48, in stone of the overworld. Can be turned off in the config.
 */
public final class CopperOreWorldGen implements IWorldGenerator {
    private static final int VEIN_SIZE = 10;
    private static final int TRIES_PER_CHUNK = 16;
    private static final int PEAK_Y = 48;

    private WorldGenMinable generator;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator,
            IChunkProvider chunkProvider) {
        if (world.provider.getDimensionType() != DimensionType.OVERWORLD
                || !AEConfig.instance().isGenerateCopperOre()) {
            return;
        }
        if (generator == null) {
            // Replaces natural stone (incl. granite, diorite, andesite) like the vanilla ores
            generator = new WorldGenMinable(AEBlocks.COPPER_ORE.block().getDefaultState(), VEIN_SIZE);
        }
        for (int i = 0; i < TRIES_PER_CHUNK; i++) {
            // From the chunk corner like the vanilla ores: WorldGenMinable adds the decoration offset of 8 itself, so
            // the vein stays in the chunks that are being populated
            int x = (chunkX << 4) + random.nextInt(16);
            int y = random.nextInt(PEAK_Y + 1) + random.nextInt(PEAK_Y + 1);
            int z = (chunkZ << 4) + random.nextInt(16);
            generator.generate(world, random, new BlockPos(x, y, z));
        }
    }
}
