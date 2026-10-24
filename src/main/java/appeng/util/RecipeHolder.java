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

import java.util.Objects;

import net.minecraft.util.ResourceLocation;

/**
 * Backport of Minecraft's RecipeHolder: a recipe together with its id.
 */
public final class RecipeHolder<T> {
    private final ResourceLocation id;
    private final T value;

    public RecipeHolder(ResourceLocation id, T value) {
        this.id = Objects.requireNonNull(id);
        this.value = Objects.requireNonNull(value);
    }

    public ResourceLocation id() {
        return id;
    }

    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof RecipeHolder<?> other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
