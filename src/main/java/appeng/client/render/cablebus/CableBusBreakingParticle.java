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

package appeng.client.render.cablebus;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.effects.AEParticle;

// Derived from Vanilla's BreakingParticle, but allows
// a texture to be specified directly rather than via an itemstack
@SideOnly(Side.CLIENT)
public class CableBusBreakingParticle extends AEParticle {

    private final float uCoord;
    private final float vCoord;

    public CableBusBreakingParticle(World level, double x, double y, double z, double speedX, double speedY,
            double speedZ, TextureAtlasSprite sprite) {
        super(level, x, y, z, speedX, speedY, speedZ);
        this.setSprite(sprite);
        this.particleGravity = 1.0F;
        this.particleScale /= 2.0F;
        this.uCoord = this.rand.nextFloat() * 3.0F;
        this.vCoord = this.rand.nextFloat() * 3.0F;
    }

    public CableBusBreakingParticle(World level, double x, double y, double z, TextureAtlasSprite sprite) {
        this(level, x, y, z, 0, 0, 0, sprite);
    }

    @Override
    protected float getU0() {
        return this.particleTexture.getInterpolatedU((this.uCoord + 1.0F) / 4.0F * 16.0F);
    }

    @Override
    protected float getU1() {
        return this.particleTexture.getInterpolatedU(this.uCoord / 4.0F * 16.0F);
    }

    @Override
    protected float getV0() {
        return this.particleTexture.getInterpolatedV(this.vCoord / 4.0F * 16.0F);
    }

    @Override
    protected float getV1() {
        return this.particleTexture.getInterpolatedV((this.vCoord + 1.0F) / 4.0F * 16.0F);
    }
}
