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

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;

import appeng.recipes.game.StorageCellUpgradeRecipe;

/**
 * Shows the special crafting recipe that upgrades a storage cell with a bigger component (shapeless, the old component
 * is given back), like the 1.21 recipe viewer plugins.
 */
class StorageCellUpgradeWrapper implements ICraftingRecipeWrapper {

    private final StorageCellUpgradeRecipe recipe;

    StorageCellUpgradeWrapper(StorageCellUpgradeRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, Arrays.asList(
                new ItemStack(recipe.getInputCell()),
                new ItemStack(recipe.getInputComponent())));
        // The crafting category has one output slot; the replaced component goes back to the player
        ingredients.setOutputs(VanillaTypes.ITEM, Arrays.asList(
                new ItemStack(recipe.getResultCell()),
                new ItemStack(recipe.getResultComponent())));
    }
}
