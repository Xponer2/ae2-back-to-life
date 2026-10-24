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

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Backport of NeoForge's BlockCapability on top of Forge 1.12.2 {@link Capability capabilities}. The context is
 * either {@link EnumFacing} (sided, nullable) or {@link Void}.
 */
public final class BlockCapability<T, C> {
    private final ResourceLocation name;
    private final Class<T> typeClass;
    private final boolean sided;
    private final Supplier<Capability<T>> forgeCapability;

    private BlockCapability(ResourceLocation name, Class<T> typeClass, boolean sided,
            Supplier<Capability<T>> forgeCapability) {
        this.name = name;
        this.typeClass = typeClass;
        this.sided = sided;
        this.forgeCapability = forgeCapability;
    }

    /**
     * A capability defined by AE2, registered with Forge's CapabilityManager by {@link AECapabilityRegistration}.
     */
    public static <T> BlockCapability<T, EnumFacing> createSided(ResourceLocation name, Class<T> typeClass) {
        return new BlockCapability<>(name, typeClass, true, () -> AECapabilityRegistration.get(typeClass));
    }

    public static <T> BlockCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
        return new BlockCapability<>(name, typeClass, false, () -> AECapabilityRegistration.get(typeClass));
    }

    /**
     * Wraps an existing Forge capability (item handler, fluid handler, energy, ...).
     */
    public static <T> BlockCapability<T, EnumFacing> forge(ResourceLocation name, Class<T> typeClass,
            Supplier<Capability<T>> capability) {
        return new BlockCapability<>(name, typeClass, true, capability);
    }

    public ResourceLocation name() {
        return name;
    }

    public Class<T> typeClass() {
        return typeClass;
    }

    public boolean isSided() {
        return sided;
    }

    public Capability<T> forgeCapability() {
        return Objects.requireNonNull(forgeCapability.get(), () -> "Capability " + name + " is not registered");
    }

    @Nullable
    private static EnumFacing side(@Nullable Object context) {
        return context instanceof EnumFacing f ? f : null;
    }

    @Nullable
    public T getCapability(World level, BlockPos pos, @Nullable C context) {
        if (!level.isBlockLoaded(pos)) {
            return null;
        }
        return getCapability(level.getTileEntity(pos), context);
    }

    /**
     * NeoForge style lookup with a known state/tile entity.
     */
    @Nullable
    public T getCapability(World level, BlockPos pos, @Nullable net.minecraft.block.state.IBlockState state,
            @Nullable TileEntity blockEntity, @Nullable C context) {
        if (blockEntity == null) {
            return getCapability(level, pos, context);
        }
        return getCapability(blockEntity, context);
    }

    @Nullable
    public T getCapability(@Nullable TileEntity blockEntity, @Nullable C context) {
        if (blockEntity == null || blockEntity.isInvalid()) {
            return null;
        }
        var cap = forgeCapability.get();
        if (cap == null) {
            return null;
        }
        var side = side(context);
        if (!blockEntity.hasCapability(cap, side)) {
            return null;
        }
        return blockEntity.getCapability(cap, side);
    }

    /**
     * Checks if the given Forge capability is the one wrapped by this object.
     */
    public boolean is(@Nullable Capability<?> capability) {
        return capability != null && capability == forgeCapability.get();
    }

    @Override
    public String toString() {
        return "BlockCapability[" + name + "]";
    }
}
