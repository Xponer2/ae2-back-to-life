/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.worldgen.meteorite;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.math.StatsAccumulator;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import appeng.core.AppEng;
import appeng.worldgen.meteorite.fallout.FalloutMode;

/**
 * 1.12.2 has no data driven structures: this keeps the meteorite structure rules of AE2 for 1.21 (structure set
 * {@code ae2:meteorite}: random spread with spacing 32, separation 8, salt 124895654, then a 50% chance per candidate
 * chunk) and computes the {@link PlacedMeteoriteSettings} like the 1.21 structure did. {@link MeteoriteWorldGen} places
 * the blocks chunk by chunk.
 */
public final class MeteoriteStructure {

    public static final ResourceLocation ID = AppEng.makeId("meteorite");

    public static final int SPACING = 32;
    public static final int SEPARATION = 8;
    public static final int SALT = 124895654;

    /**
     * Blocks of a meteorite are placed in the chunks within this distance (in chunks) of its start chunk (1.21: 4 * 16
     * blocks around the start chunk).
     */
    public static final int RANGE_CHUNKS = 4;

    private MeteoriteStructure() {
    }

    /**
     * 1.21 biome tag {@code ae2:has_meteorites} (= {@code #minecraft:is_overworld}): the overworld dimension type.
     */
    public static boolean isAllowedDimension(World level) {
        return level.provider.getDimensionType() == DimensionType.OVERWORLD;
    }

    /**
     * The candidate chunk of the random spread placement for the region that contains the chunk (1.21
     * {@code RandomSpreadStructurePlacement.getPotentialStructureChunk} with the linear spread type).
     */
    public static ChunkPos getPotentialStartChunk(long seed, int chunkX, int chunkZ) {
        int regionX = Math.floorDiv(chunkX, SPACING);
        int regionZ = Math.floorDiv(chunkZ, SPACING);
        var random = new Random(0L);
        // WorldgenRandom.setLargeFeatureWithSalt
        random.setSeed(regionX * 341873128712L + regionZ * 132897987541L + seed + SALT);
        int range = SPACING - SEPARATION;
        int x = random.nextInt(range);
        int z = random.nextInt(range);
        return new ChunkPos(regionX * SPACING + x, regionZ * SPACING + z);
    }

    /**
     * All potential start chunks whose meteorite area ({@link #RANGE_CHUNKS}) contains the given chunk.
     */
    public static List<ChunkPos> getPotentialStartsAround(long seed, ChunkPos chunk) {
        var result = new ArrayList<ChunkPos>(1);
        int minRegionX = Math.floorDiv(chunk.x - RANGE_CHUNKS, SPACING);
        int maxRegionX = Math.floorDiv(chunk.x + RANGE_CHUNKS, SPACING);
        int minRegionZ = Math.floorDiv(chunk.z - RANGE_CHUNKS, SPACING);
        int maxRegionZ = Math.floorDiv(chunk.z + RANGE_CHUNKS, SPACING);
        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                var start = getPotentialStartChunk(seed, rx * SPACING, rz * SPACING);
                if (Math.abs(start.x - chunk.x) <= RANGE_CHUNKS && Math.abs(start.z - chunk.z) <= RANGE_CHUNKS) {
                    result.add(start);
                }
            }
        }
        return result;
    }

    /**
     * 1.21 {@code WorldgenRandom.setLargeFeatureSeed}.
     */
    private static Random largeFeatureRandom(long seed, ChunkPos chunkPos) {
        var random = new Random(seed);
        long i = random.nextLong();
        long j = random.nextLong();
        random.setSeed(chunkPos.x * i ^ chunkPos.z * j ^ seed);
        return random;
    }

    /**
     * Decides whether a meteorite spawns at a potential start chunk and where (1.21
     * {@code findGenerationPoint} + {@code generatePieces}). Must be called once the start chunk is generated; terrain
     * heights are only read from loaded chunks, so this never causes chunks to generate.
     *
     * @return null if there is no meteorite at this start chunk
     */
    @Nullable
    public static PlacedMeteoriteSettings createSettings(World level, ChunkPos chunkPos) {
        if (!isAllowedDimension(level)) {
            return null;
        }

        if (!largeFeatureRandom(level.getSeed(), chunkPos).nextBoolean()) {
            return null;
        }

        var random = largeFeatureRandom(level.getSeed(), chunkPos);
        final int centerX = chunkPos.getXStart() + random.nextInt(16);
        final int centerZ = chunkPos.getZStart() + random.nextInt(16);
        final float meteoriteRadius = random.nextFloat() * 6.0f + 2;
        final int yOffset = (int) Math.ceil(meteoriteRadius) + 1;

        var spawnBiome = level.getBiome(new BlockPos(centerX, level.getSeaLevel(), centerZ));

        final boolean isOcean = BiomeDictionary.hasType(spawnBiome, Type.OCEAN);

        // Accumulate stats about the surrounding heightmap
        StatsAccumulator stats = new StatsAccumulator();
        int scanRadius = (int) Math.max(1, meteoriteRadius * 2);
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                int h = getHeight(level, centerX + x, centerZ + z, isOcean);
                if (h >= 0) {
                    stats.add(h);
                }
            }
        }
        if (stats.count() == 0) {
            return null; // the start chunk itself is not loaded, should not happen
        }

        int centerY = (int) stats.mean();
        // Spawn it down a bit further with a high variance.
        if (stats.populationVariance() > 5) {
            centerY -= (stats.mean() - stats.min()) * .75;
        }

        // Offset caused by the meteorsize
        centerY -= yOffset;

        // If we seemingly don't have enough space to spawn (as can happen in flat chunks generators)
        // we snugly generate it on bedrock.
        centerY = Math.max(yOffset, centerY);

        BlockPos actualPos = new BlockPos(centerX, centerY, centerZ);
        boolean craterLake = locateWaterAroundTheCrater(level, actualPos, meteoriteRadius);
        CraterType craterType = determineCraterType(actualPos, spawnBiome, random);
        boolean pureCrater = random.nextFloat() > .9f;

        var fallout = FalloutMode.fromBiome(spawnBiome);

        return new PlacedMeteoriteSettings(actualPos, meteoriteRadius, craterType, fallout, pureCrater, craterLake);
    }

    /**
     * Height of the terrain at a column in a loaded chunk, -1 if the chunk is not loaded. {@code oceanFloor} skips
     * liquids like the 1.21 OCEAN_FLOOR heightmap, otherwise it is the first non-air block like WORLD_SURFACE.
     */
    private static int getHeight(World level, int x, int z, boolean oceanFloor) {
        Chunk chunk = level.getChunkProvider().getLoadedChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return -1;
        }
        int y = chunk.getHeightValue(x & 15, z & 15);
        if (oceanFloor) {
            var pos = new BlockPos.MutableBlockPos(x, y, z);
            while (y > 0) {
                pos.setPos(x, y - 1, z);
                var material = chunk.getBlockState(pos).getMaterial();
                if (material.blocksMovement() && !material.isLiquid()) {
                    break;
                }
                y--;
            }
        }
        return y;
    }

    /**
     * Scan for water about 1 block further than the crater radius 333 1174
     *
     * @return true, if it found a single block of water
     */
    private static boolean locateWaterAroundTheCrater(World level, BlockPos pos, float radius) {
        final int seaLevel = level.getSeaLevel();
        final int maxY = seaLevel - 1;

        for (int i = pos.getX() - 32; i <= pos.getX() + 32; i++) {
            for (int k = pos.getZ() - 32; k <= pos.getZ() + 32; k++) {
                final double dx = i - pos.getX();
                final double dz = k - pos.getZ();
                final double h = pos.getY() - radius + 1;

                final double distanceFrom = dx * dx + dz * dz;

                if (maxY > h + distanceFrom * 0.0175 && maxY < h + distanceFrom * 0.02) {
                    int height = getHeight(level, i, k, true);
                    if (height >= 0 && height < seaLevel) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static CraterType determineCraterType(BlockPos pos, Biome biome, Random random) {
        // The temperature thresholds below are taken from older Vanilla code
        // (temperature categories)
        final float temp = biome.getDefaultTemperature();

        // No craters in oceans
        if (BiomeDictionary.hasType(biome, Type.OCEAN)) {
            return CraterType.NONE;
        }

        // 50% chance for a special meteor
        final boolean specialMeteor = random.nextFloat() > .5f;

        // Just a normal one
        if (!specialMeteor) {
            return CraterType.NORMAL;
        }

        // 1.21 Biome.coldEnoughToSnow / hasPrecipitation
        boolean canSnow = biome.getTemperature(pos) < 0.15F;
        boolean hasPrecipitation = biome.canRain() || biome.getEnableSnow();

        // Warm biomes, higher chance for lava
        if (temp >= 1) {

            // 50% chance to actually spawn as lava
            final boolean lava = random.nextFloat() > .5f;

            if (!hasPrecipitation) {
                return lava ? CraterType.LAVA : CraterType.NORMAL;
            } else if (!canSnow) {
                // 25% chance to convert a lava to obsidian
                final boolean obsidian = random.nextFloat() > .75f;
                final CraterType alternativObsidian = obsidian ? CraterType.OBSIDIAN : CraterType.LAVA;
                return lava ? alternativObsidian : CraterType.NORMAL;
            } else {
                // Nothing for now.
            }
        }

        // Temperate biomes. Water or maybe lava
        if (temp < 1 && temp >= 0.2) {
            // 75% chance to actually spawn with a crater lake
            final boolean lake = random.nextFloat() > .25f;
            // 20% to spawn with lava
            final boolean lava = random.nextFloat() > .8f;

            if (!hasPrecipitation) {
                // No rainfall, water how?
                return lava ? CraterType.LAVA : CraterType.NORMAL;
            } else if (!canSnow) {
                // Rainfall, can also turn lava to obsidian
                final boolean obsidian = random.nextFloat() > .75f;
                final CraterType alternativObsidian = obsidian ? CraterType.OBSIDIAN : CraterType.LAVA;
                final CraterType craterLake = lake ? CraterType.WATER : CraterType.NORMAL;
                return lava ? alternativObsidian : craterLake;
            } else {
                // No lava, but snow
                final boolean snow = random.nextFloat() > .75f;
                final CraterType water = lake ? CraterType.WATER : CraterType.NORMAL;
                return snow ? CraterType.SNOW : water;
            }
        }

        // Cold biomes, Snow or Ice, maybe water and very rarely lava.
        if (temp < 0.2) {
            // 75% chance to actually spawn with a crater lake
            final boolean lake = random.nextFloat() > .25f;
            // 5% to spawn with lava
            final boolean lava = random.nextFloat() > .95f;
            // 75% chance to freeze
            final boolean frozen = random.nextFloat() > .25f;

            if (!hasPrecipitation) {
                // No rainfall, water how?
                return lava ? CraterType.LAVA : CraterType.NORMAL;
            } else if (!canSnow) {
                final CraterType frozenLake = frozen ? CraterType.ICE : CraterType.WATER;
                final CraterType craterLake = lake ? frozenLake : CraterType.NORMAL;
                return lava ? CraterType.LAVA : craterLake;
            } else {
                final CraterType snowCovered = lake ? CraterType.SNOW : CraterType.NORMAL;
                return lava ? CraterType.LAVA : snowCovered;
            }
        }

        return CraterType.NORMAL;
    }
}
