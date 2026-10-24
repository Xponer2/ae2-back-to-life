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

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Base for the AE particles, stands in for the 1.21 TextureSheetParticle.
 * <p/>
 * The 1.21 particles use alpha values above 1 (which 1.21 clamps), but the 1.12.2 buffer builder would overflow the
 * color byte, so the billboard is drawn here with clamped colors. A sprite set with {@link #setSprite} uses the block
 * atlas (FX layer 1), otherwise the particle uses the vanilla particle sheet (FX layer 0).
 */
@SideOnly(Side.CLIENT)
public abstract class AEParticle extends Particle {

    private boolean usesAtlas;

    protected AEParticle(World level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected AEParticle(World level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    /**
     * Use a sprite from the block atlas (1.21 {@code pickSprite}).
     */
    protected void setSprite(TextureAtlasSprite sprite) {
        this.usesAtlas = true;
        this.setParticleTexture(sprite);
    }

    @Override
    public int getFXLayer() {
        return usesAtlas ? 1 : 0;
    }

    protected float getU0() {
        return particleTexture != null ? particleTexture.getMinU() : particleTextureIndexX / 16.0F;
    }

    protected float getU1() {
        return particleTexture != null ? particleTexture.getMaxU() : getU0() + 0.0624375F;
    }

    protected float getV0() {
        return particleTexture != null ? particleTexture.getMinV() : particleTextureIndexY / 16.0F;
    }

    protected float getV1() {
        return particleTexture != null ? particleTexture.getMaxV() : getV0() + 0.0624375F;
    }

    /**
     * 1.21 {@code getQuadSize}: half the edge length of the billboard.
     */
    protected float getQuadSize(float partialTicks) {
        return 0.1F * this.particleScale;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX,
            float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float size = getQuadSize(partialTicks);
        float x = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float y = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float z = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
        int light = this.getBrightnessForRender(partialTicks);
        int skyLight = light >> 16 & 65535;
        int blockLight = light & 65535;

        float r = clamp(this.particleRed);
        float g = clamp(this.particleGreen);
        float b = clamp(this.particleBlue);
        float a = clamp(this.particleAlpha);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();

        buffer.pos(x - rotationX * size - rotationXY * size, y - rotationZ * size,
                z - rotationYZ * size - rotationXZ * size)
                .tex(u1, v1).color(r, g, b, a).lightmap(skyLight, blockLight).endVertex();
        buffer.pos(x - rotationX * size + rotationXY * size, y + rotationZ * size,
                z - rotationYZ * size + rotationXZ * size)
                .tex(u1, v0).color(r, g, b, a).lightmap(skyLight, blockLight).endVertex();
        buffer.pos(x + rotationX * size + rotationXY * size, y + rotationZ * size,
                z + rotationYZ * size + rotationXZ * size)
                .tex(u0, v0).color(r, g, b, a).lightmap(skyLight, blockLight).endVertex();
        buffer.pos(x + rotationX * size - rotationXY * size, y - rotationZ * size,
                z + rotationYZ * size - rotationXZ * size)
                .tex(u0, v1).color(r, g, b, a).lightmap(skyLight, blockLight).endVertex();
    }

    protected static float clamp(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }
}
