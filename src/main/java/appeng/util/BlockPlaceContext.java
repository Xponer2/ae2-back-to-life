/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Backport of Minecraft's BlockPlaceContext. {@link #getClickedPos()} is the position the block will be placed at.
 */
public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked = true;

    public BlockPlaceContext(World level, @Nullable EntityPlayer player, EnumHand hand, ItemStack itemStack,
            RayTraceResult hitResult) {
        super(level, player, hand, itemStack, hitResult);
        this.relativePos = hitResult.getBlockPos().offset(hitResult.sideHit);
        this.replaceClicked = level.getBlockState(hitResult.getBlockPos()).getBlock()
                .isReplaceable(level, hitResult.getBlockPos());
    }

    public BlockPlaceContext(UseOnContext context) {
        this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(),
                context.getHitResult());
    }

    /**
     * Context for placing at an explicit position (as passed to ItemBlock#placeBlockAt).
     */
    public static BlockPlaceContext at(World level, @Nullable EntityPlayer player, EnumHand hand, ItemStack stack,
            BlockPos placePos, EnumFacing side, float hitX, float hitY, float hitZ) {
        var hit = new RayTraceResult(new Vec3d(placePos.getX() + hitX, placePos.getY() + hitY,
                placePos.getZ() + hitZ), side, placePos);
        var ctx = new BlockPlaceContext(level, player, hand, stack, hit);
        ctx.replaceClicked = true;
        return ctx;
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public boolean canPlace() {
        return getLevel().getBlockState(getClickedPos()).getBlock().isReplaceable(getLevel(), getClickedPos());
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public EnumFacing getNearestLookingDirection() {
        return getNearestLookingDirections()[0];
    }

    public EnumFacing getNearestLookingVerticalDirection() {
        var player = getPlayer();
        return player != null && player.rotationPitch < 0 ? EnumFacing.UP : EnumFacing.DOWN;
    }

    /**
     * Port of Direction.orderedByNearest(entity).
     */
    public EnumFacing[] getNearestLookingDirections() {
        var player = getPlayer();
        if (player == null) {
            return new EnumFacing[] { EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH,
                    EnumFacing.WEST, EnumFacing.UP };
        }
        float pitch = player.rotationPitch * ((float) Math.PI / 180F);
        float yaw = -player.rotationYaw * ((float) Math.PI / 180F);
        float sinPitch = MathHelper.sin(pitch);
        float cosPitch = MathHelper.cos(pitch);
        float sinYaw = MathHelper.sin(yaw);
        float cosYaw = MathHelper.cos(yaw);
        boolean east = sinYaw > 0.0F;
        boolean up = -sinPitch > 0.0F;
        boolean south = cosYaw > 0.0F;
        float x = east ? sinYaw : -sinYaw;
        float y = up ? -sinPitch : sinPitch;
        float z = south ? cosYaw : -cosYaw;
        float hx = x * cosPitch;
        float hz = z * cosPitch;
        EnumFacing dx = east ? EnumFacing.EAST : EnumFacing.WEST;
        EnumFacing dy = up ? EnumFacing.UP : EnumFacing.DOWN;
        EnumFacing dz = south ? EnumFacing.SOUTH : EnumFacing.NORTH;
        EnumFacing[] result;
        if (x > z) {
            if (y > hx) {
                result = makeDirectionArray(dy, dx, dz);
            } else {
                result = hz > y ? makeDirectionArray(dx, dz, dy) : makeDirectionArray(dx, dy, dz);
            }
        } else if (y > hz) {
            result = makeDirectionArray(dy, dz, dx);
        } else {
            result = hx > y ? makeDirectionArray(dz, dx, dy) : makeDirectionArray(dz, dy, dx);
        }
        // Modern MC's BlockPlaceContext moves the clicked face's opposite to the front if placing against a block
        if (!replacingClickedOnBlock()) {
            var face = getClickedFace();
            int i;
            for (i = 0; i < result.length && result[i] != face.getOpposite(); i++) {
            }
            if (i > 0 && i < result.length) {
                System.arraycopy(result, 0, result, 1, i);
                result[0] = face.getOpposite();
            }
        }
        return result;
    }

    private static EnumFacing[] makeDirectionArray(EnumFacing a, EnumFacing b, EnumFacing c) {
        return new EnumFacing[] { a, b, c, c.getOpposite(), b.getOpposite(), a.getOpposite() };
    }
}
