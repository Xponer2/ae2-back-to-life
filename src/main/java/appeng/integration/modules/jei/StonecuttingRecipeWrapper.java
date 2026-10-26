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

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import appeng.recipes.vanilla.StonecutterRecipe;
import appeng.util.RecipeHolder;

class StonecuttingRecipeWrapper implements IRecipeWrapper {

    private final RecipeHolder<StonecutterRecipe> holder;

    StonecuttingRecipeWrapper(RecipeHolder<StonecutterRecipe> holder) {
        this.holder = holder;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, InscriberRecipeWrapper.stacks(holder.value().getIngredient()));
        ingredients.setOutput(VanillaTypes.ITEM, holder.value().getResultItem());
    }
}
