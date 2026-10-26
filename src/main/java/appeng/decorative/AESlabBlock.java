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
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import appeng.api.util.StringRepresentable;
import appeng.block.AEBaseBlock;
import appeng.block.BlockProperties;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

/**
 * Modern style slab: a single block with type = bottom|top|double (1.12.2 vanilla needs two blocks for that).
 */
public class AESlabBlock extends AEBaseBlock {
    public enum SlabType implements StringRepresentable {
        TOP("top"),
        BOTTOM("bottom"),
        DOUBLE("double");

        private final String name;

        SlabType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final PropertyEnum<SlabType> TYPE = PropertyEnum.create("type", SlabType.class);

    private static final VoxelShape BOTTOM_AABB = Shapes.box16(0, 0, 0, 16, 8, 16);
    private static final VoxelShape TOP_AABB = Shapes.box16(0, 8, 0, 16, 16, 16);

    public AESlabBlock(BlockProperties props) {
        super(props);
        setDefaultState(this.blockState.getBaseState().withProperty(TYPE, SlabType.BOTTOM));
        this.useNeighborBrightness = true;
        setLightOpacity(255); // like the vanilla BlockSlab
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        builder.add(TYPE);
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            case DOUBLE -> Shapes.block();
            case TOP -> TOP_AABB;
            default -> BOTTOM_AABB;
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return state.getValue(TYPE) == SlabType.DOUBLE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(IBlockState state) {
        return state.getValue(TYPE) == SlabType.DOUBLE;
    }

    @Override
    public boolean isTopSolid(IBlockState state) {
        return state.getValue(TYPE) != SlabType.BOTTOM;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        var type = state.getValue(TYPE);
        return type == SlabType.DOUBLE || type == SlabType.TOP && face == EnumFacing.UP
                || type == SlabType.BOTTOM && face == EnumFacing.DOWN;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        var type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE) {
            return BlockFaceShape.SOLID;
        } else if (face == EnumFacing.UP && type == SlabType.TOP) {
            return BlockFaceShape.SOLID;
        } else {
            return face == EnumFacing.DOWN && type == SlabType.BOTTOM ? BlockFaceShape.SOLID
                    : BlockFaceShape.UNDEFINED;
        }
    }

    @Override
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        var pos = context.getClickedPos();
        var existing = context.getLevel().getBlockState(pos);
        if (existing.getBlock() == this) {
            return existing.withProperty(TYPE, SlabType.DOUBLE);
        }
        var face = context.getClickedFace();
        double hitY = context.getClickLocation().y - pos.getY();
        boolean top = face == EnumFacing.DOWN || face != EnumFacing.UP && hitY > 0.5;
        return getDefaultState().withProperty(TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, java.util.Random random) {
        return state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1;
    }

    @Override
    public java.util.List<net.minecraft.item.ItemStack> getDrops(IBlockState state, IBlockAccess level,
            BlockPos pos, int fortune) {
        java.util.List<net.minecraft.item.ItemStack> result = new java.util.ArrayList<>();
        result.add(new net.minecraft.item.ItemStack(this, state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1));
        return result;
    }
}
