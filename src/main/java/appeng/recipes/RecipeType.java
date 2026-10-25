/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes;

import net.minecraft.util.ResourceLocation;

/**
 * Identifies a kind of AE2 recipe (1.21 {@code RecipeType}). The recipes of a type are kept by
 * {@link RecipeManager}.
 */
public final class RecipeType<T> {
    private final ResourceLocation id;

    private RecipeType(ResourceLocation id) {
        this.id = id;
    }

    public static <T> RecipeType<T> simple(ResourceLocation id) {
        return new RecipeType<>(id);
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
