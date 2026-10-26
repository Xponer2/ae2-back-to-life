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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockStateProperties;
import appeng.blockentity.misc.LightDetectorBlockEntity;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class LightDetectorBlock extends AEBaseEntityBlock<LightDetectorBlockEntity> {

    // Cache VoxelShapes for each facing
    private static final Map<EnumFacing, VoxelShape> SHAPES;

    static {
        SHAPES = new EnumMap<>(EnumFacing.class);

        for (var facing : EnumFacing.VALUES) {
            var xOff = -0.3 * facing.getXOffset();
            var yOff = -0.3 * facing.getYOffset();
            var zOff = -0.3 * facing.getZOffset();
            VoxelShape shape = Shapes
                    .create(xOff + 0.3, yOff + 0.3, zOff + 0.3, xOff + 0.7, yOff + 0.7, zOff + 0.7);
            SHAPES.put(facing, shape);
        }
    }

    public static final PropertyDirection FACING = BlockStateProperties.FACING;

    // Used to alternate between two variants of the fixture on adjacent blocks
    public static final PropertyBool ODD = PropertyBool.create("odd");


    public LightDetectorBlock() {
        super(fixtureProps());

        this.setDefaultState(
                this.getDefaultState().withProperty(BlockStateProperties.FACING, EnumFacing.UP).withProperty(ODD, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING, ODD);
    }

    /**
     * Facing and odd placement are set when placed and aren't kept by the block entity, so they are stored in the
     * metadata (6 x 2 values).
     */
    @Override
    protected List<IProperty<?>> getMetaProperties() {
        return Arrays.asList(FACING, ODD);
    }

    @Override
    public int getSignal(IBlockState state, IBlockAccess level, BlockPos pos, EnumFacing side) {
        if (level instanceof World && this.getTileEntity(level, pos).isExposedToLight()) {
            // FIXME: This is ... uhm... fishy
            return ((World) level).getLightFromNeighbors(pos) - 6;
        }

        return 0;
    }

    @Override
    public void onNeighborChange(IBlockState state, IBlockAccess level, BlockPos pos, BlockPos neighbor) {
        var tld = this.getTileEntity(level, pos);
        if (tld != null) {
            tld.updateLight();
        }
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    private void dropTorch(World level, BlockPos pos) {
        final IBlockState prev = level.getBlockState(pos);
        level.destroyBlock(pos, true);
        level.notifyBlockUpdate(pos, prev, level.getBlockState(pos), 3);
    }

    @Override
    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        var facing = state.getValue(FACING);
        var blockPos = pos.offset(facing.getOpposite());
        return canSupportCenter(level, blockPos, facing);
    }

    @Override
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        IBlockState state = super.getStateForPlacement(context);
        BlockPos pos = context.getClickedPos();

        // Set the even/odd property
        boolean oddPlacement = (pos.getX() + pos.getY() + pos.getZ()) % 2 != 0;
        state = state.withProperty(ODD, oddPlacement);

        var levelReader = context.getLevel();
        var adirection = context.getNearestLookingDirections();

        for (var direction : adirection) {
            var placedState = state.withProperty(FACING, direction.getOpposite());
            if (canSurvive(placedState, levelReader, pos)) {
                return placedState;
            }
        }

        return null;
    }


    @Override
    public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {

        if (direction.getOpposite() == state.getValue(FACING) && !canSurvive(state, level, currentPos)) {
            return Blocks.AIR.getDefaultState();
        }

        return state;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
