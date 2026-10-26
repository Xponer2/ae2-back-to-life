/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;

import appeng.recipes.game.CustomRecipe;

/**
 * Shows AE2 crafting recipes that are no vanilla/ore recipe classes (quartz cutting knife recipes, smithing recipes as
 * crafting recipes) as shapeless recipes in the crafting category. JEI collects them from the recipe registry itself,
 * it only needs this wrapper.
 */
class CustomCraftingRecipeWrapper implements ICraftingRecipeWrapper {

    private final CustomRecipe recipe;

    CustomCraftingRecipeWrapper(CustomRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return recipe.getRegistryName();
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (var ingredient : recipe.getIngredients()) {
            inputs.add(InscriberRecipeWrapper.stacks(ingredient));
        }
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getResultItem());
    }
}
