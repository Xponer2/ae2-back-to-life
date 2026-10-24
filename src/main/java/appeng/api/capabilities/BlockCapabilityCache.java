/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 * Copyright (c) 2026 the AE2: Back to Life contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.capabilities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Backport of NeoForge's BlockCapabilityCache. 1.12.2 has no capability invalidation events, so the target tile
 * entity is cached and re-validated on every access (cheap), the capability itself is queried each time.
 */
public final class BlockCapabilityCache<T, C> {
    private final BlockCapability<T, C> capability;
    private final World level;
    private final BlockPos pos;
    @Nullable
    private final C context;
    @Nullable
    private TileEntity cachedBlockEntity;

    private BlockCapabilityCache(BlockCapability<T, C> capability, World level, BlockPos pos, @Nullable C context) {
        this.capability = capability;
        this.level = level;
        this.pos = pos.toImmutable();
        this.context = context;
    }

    public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, World level,
            BlockPos pos, @Nullable C context) {
        return new BlockCapabilityCache<>(capability, level, pos, context);
    }

    public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, World level,
            BlockPos pos, @Nullable C context, java.util.function.BooleanSupplier isValid, Runnable listener) {
        return new BlockCapabilityCache<>(capability, level, pos, context);
    }

    public World level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public C context() {
        return context;
    }

    @Nullable
    public T getCapability() {
        if (!level.isBlockLoaded(pos)) {
            return null;
        }
        var be = cachedBlockEntity;
        if (be == null || be.isInvalid() || be.getWorld() != level) {
            be = level.getTileEntity(pos);
            cachedBlockEntity = be;
        }
        return capability.getCapability(be, context);
    }
}
