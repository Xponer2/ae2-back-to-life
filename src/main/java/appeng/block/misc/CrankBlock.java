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

import appeng.block.BlockProperties;
import java.util.Arrays;

import net.minecraftforge.common.util.FakePlayer;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import appeng.block.RenderShape;

import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.api.orientation.RelativeSide;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.CrankBlockEntity;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class CrankBlock extends AEBaseEntityBlock<CrankBlockEntity> {

    private static final VoxelShape[] SHAPES = Arrays.stream(EnumFacing.VALUES)
            .map(CrankBlock::createShape)
            .toArray(VoxelShape[]::new);

    public CrankBlock(BlockProperties props) {
        super(props);
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (player instanceof FakePlayer) {
            this.dropCrank(level, pos);
            return EnumActionResult.SUCCESS;
        }

        var crank = this.getTileEntity(level, pos);
        if (crank != null) {
            crank.power();
            return EnumActionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    private void dropCrank(World level, BlockPos pos) {
        level.destroyBlock(pos, true);
        level.notifyBlockUpdate(pos, getDefaultState(), level.getBlockState(pos), 3);
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(IBlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos,
            boolean isMoving) {
        // Does the change originate from the block we're attached to?
        if (getAttachedToPos(state, pos).equals(fromPos)) {
            if (getCrankable(state, level, pos) == null) {
                dropCrank(level, pos);
            }
        }
    }

    @Override
    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        return getCrankable(state, level, pos) != null;
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        var top = getOrientationStrategy().getSide(state, RelativeSide.FRONT);
        return SHAPES[top.ordinal()];
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facing();
    }

    public ICrankable getCrankable(IBlockState state, World level, BlockPos pos) {
        // This is facing away from the block the crank is attached to
        var facing = getOrientationStrategy().getFacing(state);
        var attachedToPos = getAttachedToPos(state, pos);
        return ICrankable.get(level, attachedToPos, facing);
    }

    private BlockPos getAttachedToPos(IBlockState state, BlockPos pos) {
        var attachedToSide = getOrientationStrategy().getFacing(state).getOpposite();
        return pos.offset(attachedToSide);
    }

    private static VoxelShape createShape(EnumFacing forward) {
        var xOff = -0.15 * forward.getXOffset();
        var yOff = -0.15 * forward.getYOffset();
        var zOff = -0.15 * forward.getZOffset();
        return Shapes.create(xOff + 0.15, yOff + 0.15, zOff + 0.15,
                xOff + 0.85, yOff + 0.85, zOff + 0.85);
    }
}
