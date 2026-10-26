/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import appeng.recipes.handlers.InscriberRecipe;

class InscriberRecipeWrapper implements IRecipeWrapper {

    private final InscriberRecipe recipe;

    InscriberRecipeWrapper(InscriberRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputSlots = new ArrayList<>(3);
        inputSlots.add(stacks(this.recipe.getTopOptional()));
        inputSlots.add(stacks(this.recipe.getMiddleInput()));
        inputSlots.add(stacks(this.recipe.getBottomOptional()));
        ingredients.setInputLists(VanillaTypes.ITEM, inputSlots);
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.getResultItem());
    }

    static List<ItemStack> stacks(Ingredient ingredient) {
        return Arrays.asList(ingredient.getMatchingStacks());
    }
}
