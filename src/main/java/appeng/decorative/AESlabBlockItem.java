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

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.block.AEBaseBlockItem;
import appeng.util.ItemProperties;

/**
 * Item of {@link AESlabBlock}: merges a slab with an existing one into a double slab.
 */
public class AESlabBlockItem extends AEBaseBlockItem {
    public AESlabBlockItem(Block block, ItemProperties props) {
        super(block, props);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        var stack = player.getHeldItem(hand);
        if (!stack.isEmpty() && player.canPlayerEdit(pos.offset(facing), facing, stack)) {
            var state = worldIn.getBlockState(pos);
            if (tryMerge(player, worldIn, pos, state, facing, hitY, stack)) {
                return EnumActionResult.SUCCESS;
            }
            var other = pos.offset(facing);
            var otherState = worldIn.getBlockState(other);
            if (otherState.getBlock() == block && otherState.getValue(AESlabBlock.TYPE) != AESlabBlock.SlabType.DOUBLE
                    && mergeInto(player, worldIn, other, otherState, stack)) {
                return EnumActionResult.SUCCESS;
            }
        }
        return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    private boolean tryMerge(EntityPlayer player, World world, BlockPos pos, IBlockState state, EnumFacing facing,
            float hitY, ItemStack stack) {
        if (state.getBlock() != block) {
            return false;
        }
        var type = state.getValue(AESlabBlock.TYPE);
        if (type == AESlabBlock.SlabType.BOTTOM && facing == EnumFacing.UP
                || type == AESlabBlock.SlabType.TOP && facing == EnumFacing.DOWN) {
            return mergeInto(player, world, pos, state, stack);
        }
        return false;
    }

    private boolean mergeInto(EntityPlayer player, World world, BlockPos pos, IBlockState state, ItemStack stack) {
        var doubleState = state.withProperty(AESlabBlock.TYPE, AESlabBlock.SlabType.DOUBLE);
        var aabb = doubleState.getCollisionBoundingBox(world, pos);
        if (aabb != Block.NULL_AABB && !world.checkNoEntityCollision(aabb.offset(pos))) {
            return false;
        }
        if (world.setBlockState(pos, doubleState, 11)) {
            SoundType soundtype = block.getSoundType(doubleState, world, pos, player);
            world.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
                    (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
        return true;
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side, EntityPlayer player,
            ItemStack stack) {
        var state = worldIn.getBlockState(pos);
        if (state.getBlock() == block) {
            var type = state.getValue(AESlabBlock.TYPE);
            if (type == AESlabBlock.SlabType.BOTTOM && side == EnumFacing.UP
                    || type == AESlabBlock.SlabType.TOP && side == EnumFacing.DOWN) {
                return true;
            }
        }
        var other = worldIn.getBlockState(pos.offset(side));
        return other.getBlock() == block && other.getValue(AESlabBlock.TYPE) != AESlabBlock.SlabType.DOUBLE
                || super.canPlaceBlockOnSide(worldIn, pos, side, player, stack);
    }
}
