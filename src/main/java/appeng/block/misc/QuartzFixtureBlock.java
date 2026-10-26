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

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.RelativeSide;
import appeng.block.AEBaseBlock;
import appeng.block.BlockStateProperties;
import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.ParticleTypes;
import appeng.core.AEConfig;
import appeng.core.AppEngClient;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class QuartzFixtureBlock extends AEBaseBlock {

    // Cache VoxelShapes for each facing
    private static final Map<EnumFacing, VoxelShape> SHAPES;

    static {
        SHAPES = new EnumMap<>(EnumFacing.class);

        for (EnumFacing facing : EnumFacing.VALUES) {
            final double xOff = -0.3 * facing.getXOffset();
            final double yOff = -0.3 * facing.getYOffset();
            final double zOff = -0.3 * facing.getZOffset();
            VoxelShape shape = Shapes
                    .create(new AxisAlignedBB(xOff + 0.3, yOff + 0.3, zOff + 0.3, xOff + 0.7, yOff + 0.7, zOff + 0.7));
            SHAPES.put(facing, shape);
        }
    }

    public static final PropertyDirection FACING = BlockStateProperties.FACING;

    // Used to alternate between two variants of the fixture on adjacent blocks
    public static final PropertyBool ODD = PropertyBool.create("odd");


    public QuartzFixtureBlock() {
        super(fixtureProps().lightLevel(b -> 15));

        this.setDefaultState(
                getDefaultState().withProperty(FACING, EnumFacing.UP).withProperty(ODD, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        builder.add(FACING, ODD);
    }

    // For reference, see WallTorchBlock
    @Override
    @Nullable
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

    // Break the fixture if the block it is attached to is changed so that it could
    // no longer be placed
    @Override
    public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {

        if (direction.getOpposite() == state.getValue(FACING) && !canSurvive(state, level, currentPos)) {
            return Blocks.AIR.getDefaultState();
        }

        return state;
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        EnumFacing facing = state.getValue(FACING);
        return SHAPES.get(facing);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void animateTick(IBlockState state, World level, BlockPos pos, Random r) {
        if (!AEConfig.instance().isEnableEffects()) {
            return;
        }

        if (r.nextFloat() < 0.98) {
            return;
        }

        var orientation = getOrientation(state);

        var top = orientation.getSide(RelativeSide.TOP);
        final double xOff = -0.3 * top.getXOffset();
        final double yOff = -0.3 * top.getYOffset();
        final double zOff = -0.3 * top.getZOffset();
        for (int bolts = 0; bolts < 3; bolts++) {
            if (AppEngClient.instance().shouldAddParticles(r)) {
                AEParticles.add(ParticleTypes.LIGHTNING, xOff + 0.5 + pos.getX(), yOff + 0.5 + pos.getY(),
                        zOff + 0.5 + pos.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        var facing = state.getValue(FACING);
        var blockPos = pos.offset(facing.getOpposite());
        return canSupportCenter(level, blockPos, facing);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
