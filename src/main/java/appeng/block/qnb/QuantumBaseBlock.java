/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.block.qnb;

import appeng.block.BlockProperties;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockStateProperties;
import appeng.blockentity.qnb.QuantumBridgeBlockEntity;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public abstract class QuantumBaseBlock extends AEBaseEntityBlock<QuantumBridgeBlockEntity> {

    public static final PropertyBool FORMED = PropertyBool.create("formed");

    private static final VoxelShape SHAPE;

    static {
        final float shave = 2.0f / 16.0f;
        SHAPE = Shapes.create(new AxisAlignedBB(shave, shave, shave, 1.0f - shave, 1.0f - shave, 1.0f - shave));
    }

    public QuantumBaseBlock(BlockProperties props) {
        super(props);
        this.setDefaultState(this.getDefaultState().withProperty(FORMED, false));
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
            }

    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, QuantumBridgeBlockEntity be) {
        return currentState.withProperty(FORMED, be.isFormed());
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos,
            boolean isMoving) {
        final QuantumBridgeBlockEntity bridge = this.getTileEntity(level, pos);
        if (bridge != null) {
            bridge.neighborUpdate(fromPos);
        }
    }

    @Override
    public void onRemove(IBlockState state, World level, BlockPos pos, IBlockState newState, boolean isMoving) {
        if (newState.getBlock() == state.getBlock()) {
            return; // Just a block state change
        }

        final QuantumBridgeBlockEntity bridge = this.getTileEntity(level, pos);
        if (bridge != null) {
            bridge.breakClusterOnRemove();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }


    @Override
    public IBlockState updateShape(IBlockState blockState, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {

        return super.updateShape(blockState, facing, facingState, level, currentPos, facingPos);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * The shape is smaller than the block (1.21 only occludes with the shape), so the neighbors' faces must not be
     * culled and light passes through.
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }
}
