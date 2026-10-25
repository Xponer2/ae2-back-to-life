/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.spatial;

import net.minecraft.world.biome.Biome;

/**
 * The single biome of the spatial storage dimension (1.21 defines it in data: no precipitation, values of the vanilla
 * void biome, dark sky).
 */
public class SpatialStorageBiome extends Biome {
    public SpatialStorageBiome() {
        super(new BiomeProperties("Spatial Storage")
                .setRainDisabled()
                .setTemperature(0.5F)
                .setRainfall(0.5F)
                .setWaterColor(4159204));
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.decorator.treesPerChunk = -999;
        this.decorator.flowersPerChunk = 0;
        this.decorator.grassPerChunk = 0;
    }

    @Override
    public int getSkyColorByTemp(float currentTemperature) {
        return 0x111111;
    }

    @Override
    public float getSpawningChance() {
        return 0;
    }
}
