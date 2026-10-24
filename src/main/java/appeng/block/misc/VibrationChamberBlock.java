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

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.VibrationChamberBlockEntity;
import appeng.core.AEConfig;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.VibrationChamberMenu;
import appeng.menu.locator.MenuLocators;

public final class VibrationChamberBlock extends AEBaseEntityBlock<VibrationChamberBlockEntity> {

    // Indicates that the vibration chamber is currently working
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public VibrationChamberBlock() {
        super(metalProps().strength(4.2F));
        this.setDefaultState(this.getDefaultState().withProperty(ACTIVE, false));
    }

    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, VibrationChamberBlockEntity be) {
        return currentState.withProperty(ACTIVE, be.isOn);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, IBlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.full();
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (level.getTileEntity(pos) instanceof VibrationChamberBlockEntity be) {
            if (!level.isRemote) {
                MenuOpener.open(VibrationChamberMenu.TYPE, player, MenuLocators.forBlockEntity(be));
            }
            return EnumActionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void animateTick(IBlockState state, World level, BlockPos pos, Random r) {
        if (!AEConfig.instance().isEnableEffects()) {
            return;
        }

        var tc = this.getTileEntity(level, pos);
        if (tc != null && tc.isOn) {
            double f1 = pos.getX() + 0.5F;
            double f2 = pos.getY() + 0.5F;
            double f3 = pos.getZ() + 0.5F;

            var front = tc.getFront();
            var top = tc.getTop();

            // Cross-Product of forward/up directional vector
            final int west_x = front.getYOffset() * top.getZOffset() - front.getZOffset() * top.getYOffset();
            final int west_y = front.getZOffset() * top.getXOffset() - front.getXOffset() * top.getZOffset();
            final int west_z = front.getXOffset() * top.getYOffset() - front.getYOffset() * top.getXOffset();

            f1 += front.getXOffset() * 0.6;
            f2 += front.getYOffset() * 0.6;
            f3 += front.getZOffset() * 0.6;

            final double ox = r.nextDouble();
            final double oy = r.nextDouble() * 0.2f;

            f1 += top.getXOffset() * (-0.3 + oy);
            f2 += top.getYOffset() * (-0.3 + oy);
            f3 += top.getZOffset() * (-0.3 + oy);

            f1 += west_x * (0.3 * ox - 0.15);
            f2 += west_y * (0.3 * ox - 0.15);
            f3 += west_z * (0.3 * ox - 0.15);

            level.addParticle(ParticleTypes.SMOKE, f1, f2, f3, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.FLAME, f1, f2, f3, 0.0D, 0.0D, 0.0D);
        }
    }
}
