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

package appeng.block.misc;

import appeng.client.render.effects.AEParticles;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.GrowthAcceleratorBlockEntity;
import appeng.client.render.effects.ParticleTypes;
import appeng.core.AEConfig;
import appeng.core.AppEngClient;
import appeng.util.Platform;

public class GrowthAcceleratorBlock extends AEBaseEntityBlock<GrowthAcceleratorBlockEntity> {

    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public GrowthAcceleratorBlock() {
        super(metalProps());
        this.setDefaultState(this.getDefaultState().withProperty(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facing();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void animateTick(IBlockState state, World level, BlockPos pos, Random r) {
        if (!AEConfig.instance().isEnableEffects()) {
            return;
        }

        final GrowthAcceleratorBlockEntity cga = this.getTileEntity(level, pos);

        if (cga != null && cga.isPowered() && AppEngClient.instance().shouldAddParticles(r)) {
            final double d0 = r.nextFloat() - 0.5F;
            final double d1 = r.nextFloat() - 0.5F;

            var up = cga.getTop();
            var forward = cga.getFront();
            var west = Platform.crossProduct(forward, up);

            double rx = 0.5 + pos.getX();
            double ry = 0.5 + pos.getY();
            double rz = 0.5 + pos.getZ();

            rx += up.getXOffset() * d0;
            ry += up.getYOffset() * d0;
            rz += up.getZOffset() * d0;

            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();

            double dz = 0;
            double dx = 0;
            BlockPos pt = null;

            switch (r.nextInt(4)) {
                case 0 -> {
                    dx = 0.6;
                    dz = d1;
                    pt = new BlockPos(x + west.getXOffset(), y + west.getYOffset(), z + west.getZOffset());
                }
                case 1 -> {
                    dx = d1;
                    dz += 0.6;
                    pt = new BlockPos(x + forward.getXOffset(), y + forward.getYOffset(), z + forward.getZOffset());
                }
                case 2 -> {
                    dx = d1;
                    dz = -0.6;
                    pt = new BlockPos(x - forward.getXOffset(), y - forward.getYOffset(), z - forward.getZOffset());
                }
                case 3 -> {
                    dx = -0.6;
                    dz = d1;
                    pt = new BlockPos(x - west.getXOffset(), y - west.getYOffset(), z - west.getZOffset());
                }
            }

            if (!Platform.isAir(level.getBlockState(pt))) {
                return;
            }

            rx += dx * west.getXOffset();
            ry += dx * west.getYOffset();
            rz += dx * west.getZOffset();

            rx += dz * forward.getXOffset();
            ry += dz * forward.getYOffset();
            rz += dz * forward.getZOffset();

            AEParticles.add(ParticleTypes.LIGHTNING, rx, ry, rz, 0.0D, 0.0D,
                    0.0D);
        }
    }

}
