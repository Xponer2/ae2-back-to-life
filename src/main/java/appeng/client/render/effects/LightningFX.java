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

package appeng.client.render.effects;

import java.util.Random;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LightningFX extends AEParticle {

    private static final Random RANDOM_GENERATOR = new Random();
    private static final int STEPS = 5;
    private static final int BRIGHTNESS = 13 << 4;

    private final float[][] precomputedSteps;
    private final float[] vertices = new float[3];
    private final float[] verticesWithUV = new float[3];
    private boolean hasData = false;

    private LightningFX(World level, double x, double y, double z, double r,
            double g, double b) {
        this(level, x, y, z, r, g, b, 6);
        this.regen();
    }

    protected LightningFX(World level, double x, double y, double z, double r,
            double g, double b, int maxAge) {
        super(level, x, y, z, r, g, b);
        this.precomputedSteps = new float[LightningFX.STEPS][3];
        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        this.particleMaxAge = maxAge;
        // Sprite "minecraft:generic_0" = index 0 of the vanilla particle sheet
        this.setParticleTextureIndex(0);
    }

    protected void regen() {
        float lastDirectionX = (RANDOM_GENERATOR.nextFloat() - 0.5f) * 0.9f;
        float lastDirectionY = (RANDOM_GENERATOR.nextFloat() - 0.5f) * 0.9f;
        float lastDirectionZ = (RANDOM_GENERATOR.nextFloat() - 0.5f) * 0.9f;
        for (int s = 0; s < LightningFX.STEPS; s++) {
            this.precomputedSteps[s][0] = lastDirectionX = (lastDirectionX
                    + (RANDOM_GENERATOR.nextFloat() - 0.5f) * 0.9f) / 2.0f;
            this.precomputedSteps[s][1] = lastDirectionY = (lastDirectionY
                    + (RANDOM_GENERATOR.nextFloat() - 0.5f) * 0.9f) / 2.0f;
            this.precomputedSteps[s][2] = lastDirectionZ = (lastDirectionZ
                    + (RANDOM_GENERATOR.nextFloat() - 0.5f) * 0.9f) / 2.0f;
        }
    }

    protected int getSteps() {
        return LightningFX.STEPS;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }

        this.motionY -= 0.04D * this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX,
            float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float centerX = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float centerY = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float centerZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
        final float j = 1.0f;
        float red = this.particleRed * j * 0.9f;
        float green = this.particleGreen * j * 0.95f;
        float blue = this.particleBlue * j;

        if (this.particleAge == 3) {
            this.regen();
        }

        float u = this.getU0() + (this.getU1() - this.getU0()) / 2;
        float v = this.getV0() + (this.getV1() - this.getV0()) / 2;

        float scale = 0.02f;// 0.02F * this.particleScale;

        final float[] a = new float[3];
        final float[] b = new float[3];

        float ox = 0;
        float oy = 0;
        float oz = 0;

        for (int layer = 0; layer < 2; layer++) {
            if (layer == 0) {
                scale = 0.04f;
                red = this.particleRed * j * 0.4f;
                green = this.particleGreen * j * 0.25f;
                blue = this.particleBlue * j * 0.45f;
            } else {
                scale = 0.02f;
                red = this.particleRed * j * 0.9f;
                green = this.particleGreen * j * 0.65f;
                blue = this.particleBlue * j * 0.85f;
            }

            for (int cycle = 0; cycle < 3; cycle++) {
                this.clear();

                float x = centerX;
                float y = centerY;
                float z = centerZ;

                for (int s = 0; s < LightningFX.STEPS; s++) {
                    final float xN = x + this.precomputedSteps[s][0];
                    final float yN = y + this.precomputedSteps[s][1];
                    final float zN = z + this.precomputedSteps[s][2];

                    final float xD = xN - x;
                    final float yD = yN - y;
                    final float zD = zN - z;

                    if (cycle == 0) {
                        ox = yD * 0 - 1 * zD;
                        oy = zD * 0 - 0 * xD;
                        oz = xD * 1 - 0 * yD;
                    }
                    if (cycle == 1) {
                        ox = yD * 1 - 0 * zD;
                        oy = zD * 0 - 1 * xD;
                        oz = xD * 0 - 0 * yD;
                    }
                    if (cycle == 2) {
                        ox = yD * 0 - 0 * zD;
                        oy = zD * 1 - 0 * xD;
                        oz = xD * 0 - 1 * yD;
                    }

                    final float ss = MathHelper.sqrt(ox * ox + oy * oy + oz * oz)
                            / (((float) LightningFX.STEPS - (float) s) / LightningFX.STEPS * scale);
                    ox /= ss;
                    oy /= ss;
                    oz /= ss;

                    a[0] = x + ox;
                    a[1] = y + oy;
                    a[2] = z + oz;

                    b[0] = x;
                    b[1] = y;
                    b[2] = z;

                    this.draw(red, green, blue, buffer, a, b, u, v);

                    x = xN;
                    y = yN;
                    z = zN;
                }
            }
        }
    }

    private void clear() {
        this.hasData = false;
    }

    private void draw(float red, float green, float blue, BufferBuilder tess, float[] a, float[] b,
            float u, float v) {
        float alpha = clamp(this.particleAlpha);
        if (this.hasData) {
            tess.pos(a[0], a[1], a[2]).tex(u, v).color(red, green, blue, alpha)
                    .lightmap(BRIGHTNESS, BRIGHTNESS).endVertex();
            tess.pos(this.vertices[0], this.vertices[1], this.vertices[2]).tex(u, v)
                    .color(red, green, blue, alpha).lightmap(BRIGHTNESS, BRIGHTNESS).endVertex();
            tess.pos(this.verticesWithUV[0], this.verticesWithUV[1], this.verticesWithUV[2]).tex(u, v)
                    .color(red, green, blue, alpha).lightmap(BRIGHTNESS, BRIGHTNESS).endVertex();
            tess.pos(b[0], b[1], b[2]).tex(u, v).color(red, green, blue, alpha)
                    .lightmap(BRIGHTNESS, BRIGHTNESS).endVertex();
        }
        this.hasData = true;
        for (int x = 0; x < 3; x++) {
            this.vertices[x] = a[x];
            this.verticesWithUV[x] = b[x];
        }
    }

    protected float[][] getPrecomputedSteps() {
        return this.precomputedSteps;
    }

    @SideOnly(Side.CLIENT)
    public static final class Factory {
        private Factory() {
        }

        public static Particle createParticle(SimpleParticleType typeIn, World level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new LightningFX(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
