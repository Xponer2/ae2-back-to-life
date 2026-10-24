/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.definitions;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Stand-in for Minecraft's BlockEntityType: 1.12.2 tile entities are identified by class. Several types may share a
 * class (e.g. crafting unit/storage), the type of a tile entity is then determined by its block.
 */
public final class BlockEntityType<T extends TileEntity> {
    private final ResourceLocation id;
    private final Class<T> blockEntityClass;
    private final Supplier<T> factory;
    private final Set<Block> validBlocks = new LinkedHashSet<>();

    public BlockEntityType(ResourceLocation id, Class<T> blockEntityClass, Supplier<T> factory) {
        this.id = id;
        this.blockEntityClass = blockEntityClass;
        this.factory = factory;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    public T create() {
        return factory.get();
    }

    public void addValidBlock(Block block) {
        validBlocks.add(block);
    }

    public Set<Block> getValidBlocks() {
        return Collections.unmodifiableSet(validBlocks);
    }

    public boolean isValid(IBlockState state) {
        return validBlocks.contains(state.getBlock());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T getTileEntity(IBlockAccess level, BlockPos pos) {
        TileEntity be = level.getTileEntity(pos);
        if (be != null && blockEntityClass.isInstance(be) && validBlocks.contains(level.getBlockState(pos).getBlock())) {
            return (T) be;
        }
        return null;
    }

    @Override
    public String toString() {
        return "BlockEntityType[" + id + "]";
    }
}
