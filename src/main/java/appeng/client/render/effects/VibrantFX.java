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

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VibrantFX extends AEParticle {

    public VibrantFX(World level, double x, double y, double z, double par8,
            double par10, double par12) {
        super(level, x, y, z, par8, par10, par12);
        final float f = this.rand.nextFloat() * 0.1F + 0.8F;
        this.particleRed = f * 0.7f;
        this.particleGreen = f * 0.89f;
        this.particleBlue = f * 0.9f;
        // Sprite "minecraft:generic_0" = index 0 of the vanilla particle sheet
        this.setParticleTextureIndex(0);
        this.setSize(0.04F, 0.04F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 1.9F;
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.particleMaxAge = (int) (20.0D / (Math.random() * 0.8D + 0.1D));
    }

    @Override
    public int getBrightnessForRender(float par1) {
        // This just means full brightness
        return 15 << 20 | 15 << 4;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        // this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.particleScale *= 0.95;

        // 1.21: quadSize < 0.1, quadSize is 0.1 * particleScale
        if (this.particleMaxAge <= 0 || this.particleScale < 1.0) {
            this.setExpired();
        }
        this.particleMaxAge--;
    }

    @SideOnly(Side.CLIENT)
    public static final class Factory {
        private Factory() {
        }

        public static Particle createParticle(SimpleParticleType typeIn, World level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new VibrantFX(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
