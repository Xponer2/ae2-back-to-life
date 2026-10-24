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
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EnergyFx extends AEParticle {

    private final int startBlkX;
    private final int startBlkY;
    private final int startBlkZ;

    public EnergyFx(World level, double par2, double par4, double par6) {
        super(level, par2, par4, par6);
        this.particleGravity = 0;
        this.particleBlue = 1;
        this.particleGreen = 1;
        this.particleRed = 1;
        this.particleAlpha = 1.4f;
        this.particleScale = 3.5f;
        this.setSprite(AEParticles.getEnergySprite());

        this.startBlkX = MathHelper.floor(this.posX);
        this.startBlkY = MathHelper.floor(this.posY);
        this.startBlkZ = MathHelper.floor(this.posZ);
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX,
            float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float x = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks);
        float y = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks);
        float z = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks);

        final int blkX = MathHelper.floor(x);
        final int blkY = MathHelper.floor(y);
        final int blkZ = MathHelper.floor(z);

        if (blkX == this.startBlkX && blkY == this.startBlkY && blkZ == this.startBlkZ) {
            super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY,
                    rotationXZ);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.onGround = false;
        this.particleScale *= 0.89f;
        this.particleAlpha *= 0.89f;
    }

    public void setMotionX(float motionX) {
        this.motionX = motionX;
    }

    public void setMotionY(float motionY) {
        this.motionY = motionY;
    }

    public void setMotionZ(float motionZ) {
        this.motionZ = motionZ;
    }

    @SideOnly(Side.CLIENT)
    public static final class Factory {
        private Factory() {
        }

        public static Particle createParticle(EnergyParticleData data, World level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            EnergyFx result = new EnergyFx(level, x, y, z);
            result.setMotionX((float) xSpeed);
            result.setMotionY((float) ySpeed);
            result.setMotionZ((float) zSpeed);
            if (data.forItem()) {
                result.posX += -0.2 * data.direction().getXOffset();
                result.posY += -0.2 * data.direction().getYOffset();
                result.posZ += -0.2 * data.direction().getZOffset();
                result.particleScale *= 0.8f;
            }
            return result;
        }
    }
}
