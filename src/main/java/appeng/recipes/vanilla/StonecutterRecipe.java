/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes.vanilla;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/**
 * 1.12.2 has no stonecutter: this is the 1.21 {@code StonecutterRecipe} (one ingredient, one result), loaded from the
 * {@code minecraft:stonecutting} recipe JSON and kept in {@link VanillaRecipes}. AE2 uses it for stonecutting patterns.
 */
public final class StonecutterRecipe {
    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;

    public StonecutterRecipe(String group, Ingredient ingredient, ItemStack result) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
    }

    public String getGroup() {
        return group;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(1, ingredient);
    }

    public boolean matches(SingleRecipeInput input, World level) {
        return ingredient.apply(input.item());
    }

    public ItemStack assemble(SingleRecipeInput input) {
        return result.copy();
    }

    public ItemStack getResultItem() {
        return result.copy();
    }

    @Override
    public String toString() {
        return "StonecutterRecipe[" + ingredient + " -> " + result + "]";
    }
}
