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

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.SpatialSkyRender;

/**
 * The spatial storage dimension: like the 1.21 dimension type (fixed time, no sky light, full ambient light, no
 * weather, no mobs) and its sky effects (no clouds, no fog, custom sky).
 */
public class SpatialStorageWorldProvider extends WorldProvider {

    @Override
    protected void init() {
        this.hasSkyLight = false;
        this.biomeProvider = new BiomeProviderSingle(getBiome());
    }

    private static Biome getBiome() {
        var biome = ForgeRegistries.BIOMES.getValue(SpatialStorageDimensionIds.BIOME_KEY);
        return biome != null ? biome : Biomes.VOID;
    }

    @Override
    protected void generateLightBrightnessTable() {
        // ambientLight = 1.0
        for (int i = 0; i < this.lightBrightnessTable.length; i++) {
            this.lightBrightnessTable[i] = 1.0F;
        }
    }

    @Override
    public DimensionType getDimensionType() {
        return SpatialStorageDimensionIds.getDimensionType();
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new SpatialStorageChunkGenerator(this.world);
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        // fixedTime 12000
        return 0.5F;
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean isDaytime() {
        return false;
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    @Override
    public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return Vec3d.ZERO;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
        return Vec3d.ZERO;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isSkyColored() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getCloudHeight() {
        // disables clouds
        return Float.NaN;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getStarBrightness(float partialTicks) {
        return 0;
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    @Override
    public IRenderHandler getSkyRenderer() {
        return SpatialSkyRender.getInstance();
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    @Override
    public IRenderHandler getCloudRenderer() {
        return NoopRenderHandler.INSTANCE;
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    @Override
    public IRenderHandler getWeatherRenderer() {
        return NoopRenderHandler.INSTANCE;
    }

    @Override
    public boolean canSnowAt(BlockPos pos, boolean checkLight) {
        return false;
    }

    @Override
    public boolean canDoLightning(Chunk chunk) {
        return false;
    }

    @Override
    public boolean canDoRainSnowIce(Chunk chunk) {
        return false;
    }

    @Override
    public BlockPos getSpawnCoordinate() {
        return BlockPos.ORIGIN;
    }

    @Override
    public boolean isBlockHighHumidity(BlockPos pos) {
        return false;
    }

    @Override
    public Biome getBiomeForCoords(BlockPos pos) {
        return getBiome();
    }

    @SideOnly(Side.CLIENT)
    private static final class NoopRenderHandler extends IRenderHandler {
        static final NoopRenderHandler INSTANCE = new NoopRenderHandler();

        @Override
        public void render(float partialTicks, net.minecraft.client.multiplayer.WorldClient world,
                net.minecraft.client.Minecraft mc) {
        }
    }
}
