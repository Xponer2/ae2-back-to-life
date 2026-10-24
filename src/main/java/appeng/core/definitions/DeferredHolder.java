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

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;

/**
 * Replacement for NeoForge's DeferredHolder: the object is created on first access (at the latest when AE2 registers
 * its content in the corresponding Forge registry event).
 */
public final class DeferredHolder<T> implements Supplier<T> {
    private final ResourceLocation id;
    private final Supplier<? extends T> factory;
    @Nullable
    private T value;
    private boolean creating;

    public DeferredHolder(ResourceLocation id, Supplier<? extends T> factory) {
        this.id = Objects.requireNonNull(id);
        this.factory = Objects.requireNonNull(factory);
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public synchronized T get() {
        if (value == null) {
            if (creating) {
                throw new IllegalStateException("Recursive creation of " + id);
            }
            creating = true;
            try {
                value = Objects.requireNonNull(factory.get(), () -> "Factory for " + id + " returned null");
            } finally {
                creating = false;
            }
        }
        return value;
    }

    public boolean isBound() {
        return value != null;
    }

    @Override
    public String toString() {
        return "DeferredHolder[" + id + "]";
    }
}
