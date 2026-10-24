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

import javax.vecmath.Vector3f;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CraftingFx extends AEParticle {

    // Offset relative to center of block, is the starting point of the particle
    // movement
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    public CraftingFx(World level, double x, double y, double z) {
        super(level, x, y, z);

        // Pick a random normal, offset it by 0.35 and use that as the particle origin
        Vector3f off = new Vector3f(rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f);
        off.normalize();
        off.scale(0.35f);
        offsetX = off.x;
        offsetY = off.y;
        offsetZ = off.z;

        this.particleGravity = 0;
        this.particleBlue = 1;
        this.particleGreen = 0.9f;
        this.particleRed = 1;
        this.setSprite(AEParticles.getEnergySprite());
        this.particleMaxAge /= 1.2;
        this.canCollide = false; // we're INSIDE the block anyway
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX,
            float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float f = (this.particleAge + partialTicks) / this.particleMaxAge;
        float offX = (float) posX + lerp(f, offsetX, 0);
        float offY = (float) posY + lerp(f, offsetY, 0);
        float offZ = (float) posZ + lerp(f, offsetZ, 0);
        float alpha = clamp(lerp(easeOutCirc(f), 1.3f, 0.1f));
        float scale = lerp(easeOutCirc(f), 0.13f, 0.0f);

        // I believe this particle is same as breaking particle, but should not exit the
        // original block it was
        // spawned in (which is encased in glass)
        offX -= (float) interpPosX;
        offY -= (float) interpPosY;
        offZ -= (float) interpPosZ;

        // The camera-facing corners (-1,-1), (-1,1), (1,1), (1,-1), like Particle.renderParticle
        float[][] corners = new float[][] {
                { -rotationX - rotationXY, -rotationZ, -rotationYZ - rotationXZ },
                { -rotationX + rotationXY, rotationZ, -rotationYZ + rotationXZ },
                { rotationX + rotationXY, rotationZ, rotationYZ + rotationXZ },
                { rotationX - rotationXY, -rotationZ, rotationYZ - rotationXZ } };
        for (float[] corner : corners) {
            corner[0] = corner[0] * scale + offX;
            corner[1] = corner[1] * scale + offY;
            corner[2] = corner[2] * scale + offZ;
        }

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();

        // full brightness
        int j = 240;
        int k = 240;
        // 1.21 mixes the z coordinates of the corners in this order as well
        buffer.pos(corners[3][0], corners[3][1], corners[0][2]).tex(maxU, maxV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, alpha).lightmap(j, k).endVertex();
        buffer.pos(corners[2][0], corners[2][1], corners[1][2]).tex(maxU, minV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, alpha).lightmap(j, k).endVertex();
        buffer.pos(corners[1][0], corners[1][1], corners[2][2]).tex(minU, minV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, alpha).lightmap(j, k).endVertex();
        buffer.pos(corners[0][0], corners[0][1], corners[3][2]).tex(minU, maxV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, alpha).lightmap(j, k).endVertex();
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    // https://easings.net/#easeOutCirc
    private static float easeOutCirc(float x) {
        return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    @Override
    public void onUpdate() {
        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }
    }

    @SideOnly(Side.CLIENT)
    public static final class Factory {
        private Factory() {
        }

        public static Particle createParticle(SimpleParticleType data, World level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new CraftingFx(level, x, y, z);
        }
    }
}
