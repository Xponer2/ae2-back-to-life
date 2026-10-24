/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.decorative;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.block.AEBaseBlock;
import appeng.block.BlockProperties;
import appeng.block.BlockStateProperties;
import appeng.util.BlockPlaceContext;

/**
 * Replacement for Minecraft's RotatedPillarBlock (same "axis" property).
 */
public class AEPillarBlock extends AEBaseBlock {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockStateProperties.AXIS;

    public AEPillarBlock(BlockProperties props) {
        super(props);
        setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    @Override
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return getDefaultState().withProperty(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot == Rotation.COUNTERCLOCKWISE_90 || rot == Rotation.CLOCKWISE_90) {
            var axis = state.getValue(AXIS);
            if (axis == EnumFacing.Axis.X) {
                return state.withProperty(AXIS, EnumFacing.Axis.Z);
            } else if (axis == EnumFacing.Axis.Z) {
                return state.withProperty(AXIS, EnumFacing.Axis.X);
            }
        }
        return state;
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        var state = world.getBlockState(pos);
        world.setBlockState(pos, state.withProperty(AXIS, axis.getAxis()));
        return true;
    }
}
