/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import appeng.api.parts.IPartHost;
import appeng.decorative.solid.GlassState;
import appeng.decorative.solid.QuartzGlassBlock;

/**
 * The connected texture state of quartz glass. The 1.21 GlassBakedModel computed it in {@code getModelData}, in
 * 1.12.2 {@link QuartzGlassBlock#getExtendedState} puts {@link #create} into {@link AEModelData#MODEL_DATA}.
 * <p/>
 * Not client-only, the block references it.
 */
public final class GlassModelData {

    // This unlisted property is used to determine the actual block that should be
    // rendered
    public static final ModelProperty<GlassState> GLASS_STATE = new ModelProperty<>();

    private GlassModelData() {
    }

    public static ModelData create(IBlockAccess level, IBlockState state, BlockPos pos) {
        return ModelData.builder().with(GLASS_STATE, getGlassState(level, state, pos)).build();
    }

    public static GlassState getGlassState(IBlockAccess level, IBlockState state, BlockPos pos) {
        /*
         * This needs some explanation: The bit-field contains 4-bits, one for each direction that a frame may be drawn.
         * Converted to a number, the bit-field is then used as an index into the list of frame textures, which have
         * been created in such a way that their filenames indicate, in which directions they contain borders. i.e.
         * bitmask = 0101 means a border should be drawn up and down (in terms of u,v space). Converted to a number,
         * this bitmask is 5. So the texture at index 5 is used. That texture had "0101" in its filename to indicate
         * this.
         */
        int[] masks = new int[6];
        for (EnumFacing facing : EnumFacing.values()) {
            masks[facing.getIndex()] = makeBitmask(level, state, pos, facing);
        }
        boolean[] adjacentGlassBlocks = new boolean[6];
        for (EnumFacing facing : EnumFacing.values()) {
            adjacentGlassBlocks[facing.getIndex()] = isGlassBlock(level, state, pos, facing,
                    facing, facing.getOpposite());
        }
        return new GlassState(masks, adjacentGlassBlocks);
    }

    /**
     * Creates the bitmask that indicates, in which directions (in terms of u,v space) a border should be drawn.
     */
    private static int makeBitmask(IBlockAccess level, IBlockState state, BlockPos pos, EnumFacing side) {
        return switch (side) {
            case DOWN -> makeBitmask(level, state, pos, side, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.NORTH,
                    EnumFacing.WEST);
            case UP -> makeBitmask(level, state, pos, side, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH,
                    EnumFacing.EAST);
            case NORTH -> makeBitmask(level, state, pos, side, EnumFacing.UP, EnumFacing.WEST, EnumFacing.DOWN,
                    EnumFacing.EAST);
            case SOUTH -> makeBitmask(level, state, pos, side, EnumFacing.UP, EnumFacing.EAST, EnumFacing.DOWN,
                    EnumFacing.WEST);
            case WEST -> makeBitmask(level, state, pos, side, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.DOWN,
                    EnumFacing.NORTH);
            case EAST -> makeBitmask(level, state, pos, side, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.DOWN,
                    EnumFacing.SOUTH);
        };
    }

    private static int makeBitmask(IBlockAccess level, IBlockState state, BlockPos pos, EnumFacing face,
            EnumFacing up, EnumFacing right, EnumFacing down, EnumFacing left) {

        int bitmask = 0;

        if (!isGlassBlock(level, state, pos, face, up, face)) {
            bitmask |= 1;
        }
        if (!isGlassBlock(level, state, pos, face, right, face)) {
            bitmask |= 2;
        }
        if (!isGlassBlock(level, state, pos, face, down, face)) {
            bitmask |= 4;
        }
        if (!isGlassBlock(level, state, pos, face, left, face)) {
            bitmask |= 8;
        }
        return bitmask;
    }

    /**
     * Checks if the given block is a glass block.
     *
     * @param queryingFace Face of the glass that is currently performing the check.
     * @param adjFace      Face of the glass that we are currently checking for.
     * @param adjDir       Direction in which to check.
     */
    private static boolean isGlassBlock(IBlockAccess level, IBlockState state, BlockPos pos,
            EnumFacing queryingFace, EnumFacing adjDir, EnumFacing adjFace) {
        var adjacentPos = pos.offset(adjDir);

        // Checks that the adjacent block is indeed glass
        if (!(getAppearance(level, adjacentPos, adjFace).getBlock() instanceof QuartzGlassBlock)) {
            return false;
        }

        // Checks that the current block is also glass, in other words that the adjacent block would connect to us.
        // This ensures consistency between this block and the adjacent block deciding to connect or not.
        return state.getBlock() instanceof QuartzGlassBlock;
    }

    /**
     * Replacement for the 1.21 {@code BlockState.getAppearance}: a cable bus looks like its facade on the given side.
     */
    private static IBlockState getAppearance(IBlockAccess level, BlockPos pos, EnumFacing side) {
        var te = level.getTileEntity(pos);
        if (te instanceof IPartHost partHost) {
            var facade = partHost.getFacadeContainer().getFacade(side);
            if (facade != null) {
                return facade.getBlockState();
            }
        }
        return level.getBlockState(pos);
    }
}
