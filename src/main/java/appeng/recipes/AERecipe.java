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

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/**
 * An AE2 recipe that isn't a crafting table recipe (1.21 {@code Recipe}): inscriber, charger, in-world transformation,
 * ... These are not registered with the game, {@link RecipeManager} keeps them.
 */
public interface AERecipe<I extends RecipeInput> {
    boolean matches(I input, World level);

    ItemStack assemble(I input);

    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    ItemStack getResultItem();

    RecipeSerializer<?> getSerializer();

    RecipeType<?> getType();

    default NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    /**
     * Special recipes are not shown in recipe books.
     */
    default boolean isSpecial() {
        return false;
    }
}
