/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.effects;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AppEng;

/**
 * Client-side spawning of AE particles. Replaces the 1.21 particle registry and particle providers.
 * <ul>
 * <li>{@link #add} is the replacement for {@code level.addParticle(...)}: it honors the particle setting and the
 * maximum particle distance.</li>
 * <li>{@link #create} is the replacement for {@code particleEngine.createParticle(...)}: it always spawns.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class AEParticles {

    /**
     * Creates the particle for the given options, replacement for the 1.21 ParticleProvider.
     */
    @FunctionalInterface
    public interface Factory<T extends AEParticleOptions> {
        @Nullable
        Particle createParticle(T options, World level, double x, double y, double z, double xSpeed,
                double ySpeed, double zSpeed);
    }

    private static final Map<AEParticleType<?>, Factory<?>> FACTORIES = new IdentityHashMap<>();

    public static final ResourceLocation ENERGY_TEXTURE = AppEng.makeId("particle/energy");
    public static final ResourceLocation MATTER_CANNON_TEXTURE = AppEng.makeId("particle/matter_cannon");

    // Sprites in the block atlas (particle layer 1). The vanilla "generic_0" sprite is index 0 of layer 0.
    private static TextureAtlasSprite energySprite;
    private static TextureAtlasSprite matterCannonSprite;

    static {
        register(ParticleTypes.CRAFTING, CraftingFx.Factory::createParticle);
        register(ParticleTypes.ENERGY, EnergyFx.Factory::createParticle);
        register(ParticleTypes.LIGHTNING_ARC, LightningArcFX.Factory::createParticle);
        register(ParticleTypes.LIGHTNING, LightningFX.Factory::createParticle);
        register(ParticleTypes.MATTER_CANNON, MatterCannonFX.Factory::createParticle);
        register(ParticleTypes.VIBRANT, VibrantFX.Factory::createParticle);
    }

    private AEParticles() {
    }

    public static <T extends AEParticleOptions> void register(AEParticleType<T> type, Factory<T> factory) {
        FACTORIES.put(type, factory);
    }

    /**
     * Registers the particle sprites in the block atlas. Called from {@code TextureStitchEvent.Pre}.
     */
    public static void registerSprites(TextureMap map) {
        energySprite = map.registerSprite(ENERGY_TEXTURE);
        matterCannonSprite = map.registerSprite(MATTER_CANNON_TEXTURE);
    }

    public static TextureAtlasSprite getEnergySprite() {
        return energySprite;
    }

    public static TextureAtlasSprite getMatterCannonSprite() {
        return matterCannonSprite;
    }

    /**
     * Like {@code level.addParticle}: skips the particle if it's too far away from the camera or the particle
     * setting doesn't allow it.
     */
    @Nullable
    public static Particle add(AEParticleOptions options, double x, double y, double z, double xSpeed,
            double ySpeed, double zSpeed) {
        var minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.getRenderViewEntity();
        if (camera == null || minecraft.world == null || minecraft.effectRenderer == null) {
            return null;
        }

        int particleSetting = minecraft.gameSettings.particleSetting;
        if (particleSetting == 1 && minecraft.world.rand.nextInt(3) == 0) {
            particleSetting = 2;
        }

        if (camera.getDistanceSq(x, y, z) > 1024.0D || particleSetting > 1) {
            return null;
        }

        return create(options, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    /**
     * Like {@code particleEngine.createParticle}: always creates and adds the particle.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends AEParticleOptions> Particle create(T options, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed) {
        var minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.effectRenderer == null) {
            return null;
        }

        var factory = (Factory<T>) FACTORIES.get(options.getType());
        if (factory == null) {
            throw new IllegalArgumentException("No particle factory registered for " + options.getType());
        }

        var particle = factory.createParticle(options, minecraft.world, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) {
            minecraft.effectRenderer.addEffect(particle);
        }
        return particle;
    }
}
