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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import appeng.api.util.StringRepresentable;
import appeng.block.AEBaseBlock;
import appeng.block.BlockProperties;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

/**
 * Modern style wall (up + none/low/tall sides) for AE2's decorative blocks. The connections are computed in
 * {@link #getActualState}, nothing is stored in the metadata.
 */
public class AEWallBlock extends AEBaseBlock {
    public enum WallSide implements StringRepresentable {
        NONE("none"),
        LOW("low"),
        TALL("tall");

        private final String name;

        WallSide(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyEnum<WallSide> NORTH = PropertyEnum.create("north", WallSide.class);
    public static final PropertyEnum<WallSide> EAST = PropertyEnum.create("east", WallSide.class);
    public static final PropertyEnum<WallSide> SOUTH = PropertyEnum.create("south", WallSide.class);
    public static final PropertyEnum<WallSide> WEST = PropertyEnum.create("west", WallSide.class);

    private static final VoxelShape POST = Shapes.box16(4, 0, 4, 12, 16, 12);
    private static final VoxelShape POST_COLLISION = Shapes.box16(4, 0, 4, 12, 24, 12);

    public AEWallBlock(BlockProperties props) {
        super(props.copy().noOcclusion()); // the properties are shared with other blocks of the family
        setDefaultState(this.blockState.getBaseState().withProperty(UP, true).withProperty(NORTH, WallSide.NONE)
                .withProperty(EAST, WallSide.NONE).withProperty(SOUTH, WallSide.NONE)
                .withProperty(WEST, WallSide.NONE));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected List<IProperty<?>> getMetaProperties() {
        return Collections.emptyList();
    }

    private static PropertyEnum<WallSide> property(EnumFacing side) {
        return switch (side) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    private boolean connectsTo(IBlockAccess level, BlockPos pos, EnumFacing side) {
        var other = pos.offset(side);
        var state = level.getBlockState(other);
        var block = state.getBlock();
        if (block instanceof AEWallBlock || block instanceof BlockWall) {
            return true;
        }
        if (block instanceof BlockFenceGate) {
            return true;
        }
        var shape = state.getBlockFaceShape(level, other, side.getOpposite());
        return shape == BlockFaceShape.SOLID && !isExceptBlockForAttachWithPiston(block);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        var above = worldIn.getBlockState(pos.up());
        var aboveIsWall = above.getBlock() instanceof AEWallBlock;
        int connections = 0;
        boolean north = false, south = false, east = false, west = false;
        for (var side : EnumFacing.Plane.HORIZONTAL) {
            boolean c = connectsTo(worldIn, pos, side);
            if (c) {
                connections++;
            }
            switch (side) {
                case NORTH -> north = c;
                case SOUTH -> south = c;
                case EAST -> east = c;
                default -> west = c;
            }
        }
        boolean straight = connections == 2 && (north && south || east && west);
        // Like modern walls: no post for straight sections unless something is placed on top
        boolean up = !straight || !worldIn.isAirBlock(pos.up()) && !aboveIsWall;
        var tallAbove = !worldIn.isAirBlock(pos.up())
                && (aboveIsWall || above.getBlockFaceShape(worldIn, pos.up(), EnumFacing.DOWN) == BlockFaceShape.SOLID);
        for (var side : EnumFacing.Plane.HORIZONTAL) {
            boolean c = switch (side) {
                case NORTH -> north;
                case SOUTH -> south;
                case EAST -> east;
                default -> west;
            };
            state = state.withProperty(property(side), !c ? WallSide.NONE : tallAbove ? WallSide.TALL : WallSide.LOW);
        }
        return state.withProperty(UP, up);
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return computeShape(state, 14, 16);
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos,
            CollisionContext context) {
        return computeShape(state, 24, 24);
    }

    private static VoxelShape computeShape(IBlockState state, double lowHeight, double tallHeight) {
        var shapes = new ArrayList<VoxelShape>();
        if (state.getValue(UP)) {
            shapes.add(lowHeight > 16 ? POST_COLLISION : POST);
        }
        for (var side : EnumFacing.Plane.HORIZONTAL) {
            var value = state.getValue(property(side));
            if (value == WallSide.NONE) {
                continue;
            }
            double h = value == WallSide.TALL ? tallHeight : lowHeight;
            shapes.add(switch (side) {
                case NORTH -> Shapes.box16(5, 0, 0, 11, h, 11);
                case SOUTH -> Shapes.box16(5, 0, 5, 11, h, 16);
                case WEST -> Shapes.box16(0, 0, 5, 11, h, 11);
                default -> Shapes.box16(5, 0, 5, 16, h, 11);
            });
        }
        return shapes.isEmpty() ? POST : Shapes.or(shapes);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face != EnumFacing.UP && face != EnumFacing.DOWN ? BlockFaceShape.MIDDLE_POLE_THICK
                : BlockFaceShape.CENTER_BIG;
    }

    @Override
    public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        Block other = world.getBlockState(pos.offset(facing)).getBlock();
        return other instanceof AEWallBlock || other instanceof BlockWall;
    }
}
