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

package appeng.block.spatial;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import appeng.block.BlockProperties;

import appeng.core.CreativeTabOutput;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import appeng.block.RenderShape;

import appeng.block.AEBaseBlock;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

/**
 * This block is used to fill empty space in spatial dimensions and delinates the border of a spatial dimensions's
 * usable space.
 */
public class MatrixFrameBlock extends AEBaseBlock {

    public MatrixFrameBlock() {
        super(BlockProperties.of().strength(-1.0F, 6000000.0F).noOcclusion().noLootTable());
    }

    @Override
    public RenderShape getRenderShape(IBlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // do nothing
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos,
            CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        // This also prevents any blocks from being placed on this block!
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        return false;
    }

    @Override
    public void wasExploded(World level, BlockPos pos, Explosion explosion) {
        // Don't explode.
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getAmbientOcclusionLightValue(IBlockState state) {
        return 1.0f;
    }

    @Override
    public boolean canEntityDestroy(final IBlockState state, final IBlockAccess level, final BlockPos pos,
            final Entity entity) {
        return false;
    }
}
