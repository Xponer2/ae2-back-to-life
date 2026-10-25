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

package appeng.decorative.solid;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseBlock;
import appeng.block.BlockProperties;
import appeng.client.render.model.AEModelData;
import appeng.client.render.model.GlassModelData;

/**
 * Quartz glass (1.21 TransparentBlock). The connected textures of its model come from the model data in the extended
 * state.
 */
public class QuartzGlassBlock extends AEBaseBlock {

    public QuartzGlassBlock(BlockProperties props) {
        super(props);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[] { AEModelData.MODEL_DATA });
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state instanceof IExtendedBlockState extendedState) {
            return extendedState.withProperty(AEModelData.MODEL_DATA, GlassModelData.create(world, state, pos));
        }
        return state;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        var adjacentBlockState = world.getBlockState(pos.offset(side));
        if (adjacentBlockState.getBlock() instanceof QuartzGlassBlock
                && adjacentBlockState.getRenderType() == state.getRenderType()) {
            return false;
        }

        return super.shouldSideBeRendered(state, world, pos, side);
    }

}
