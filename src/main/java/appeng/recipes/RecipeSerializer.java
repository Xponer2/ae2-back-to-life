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

import com.mojang.serialization.MapCodec;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Reads a recipe from its JSON file (1.21 {@code RecipeSerializer}). Recipes are loaded from the mod files on both
 * sides, the stream codec is there for code that sends recipes in packets.
 */
public interface RecipeSerializer<T> {
    MapCodec<T> codec();

    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
}
