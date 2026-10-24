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
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Backport of Minecraft's UseOnContext (the arguments of Item#onItemUse bundled together).
 */
public class UseOnContext {
    @Nullable
    private final EntityPlayer player;
    private final EnumHand hand;
    private final RayTraceResult hitResult;
    private final World level;
    private final ItemStack itemStack;

    public UseOnContext(EntityPlayer player, EnumHand hand, RayTraceResult hitResult) {
        this(player.world, player, hand, player.getHeldItem(hand), hitResult);
    }

    public UseOnContext(World level, @Nullable EntityPlayer player, EnumHand hand, ItemStack itemStack,
            RayTraceResult hitResult) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hitResult;
        this.itemStack = itemStack;
        this.level = level;
    }

    public static UseOnContext of(EntityPlayer player, World level, BlockPos pos, EnumHand hand, EnumFacing side,
            float hitX, float hitY, float hitZ) {
        var hit = new RayTraceResult(new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), side, pos);
        return new UseOnContext(level, player, hand, player.getHeldItem(hand), hit);
    }

    public RayTraceResult getHitResult() {
        return hitResult;
    }

    public BlockPos getClickedPos() {
        return hitResult.getBlockPos();
    }

    public EnumFacing getClickedFace() {
        return hitResult.sideHit;
    }

    public Vec3d getClickLocation() {
        return hitResult.hitVec;
    }

    public boolean isInside() {
        return false;
    }

    public ItemStack getItemInHand() {
        return itemStack;
    }

    @Nullable
    public EntityPlayer getPlayer() {
        return player;
    }

    public EnumHand getHand() {
        return hand;
    }

    public World getLevel() {
        return level;
    }

    public EnumFacing getHorizontalDirection() {
        return player == null ? EnumFacing.NORTH : player.getHorizontalFacing();
    }

    public boolean isSecondaryUseActive() {
        return player != null && player.isSneaking();
    }

    public float getRotation() {
        return player == null ? 0.0F : player.rotationYaw;
    }
}
