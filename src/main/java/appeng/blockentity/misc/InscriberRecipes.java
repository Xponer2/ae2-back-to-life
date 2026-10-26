/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.blockentity.misc;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import appeng.util.RecipeHolder;

import appeng.api.ids.AEComponents;
import appeng.core.definitions.AEItems;
import appeng.recipes.AERecipeTypes;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.util.Platform;
import appeng.recipes.RecipeManager;

/**
 * This class indexes all inscriber recipes to find valid inputs for the top and bottom optional slots. This speeds up
 * checks whether inputs for those two slots are valid.
 */
public final class InscriberRecipes {

    private InscriberRecipes() {
    }

    /**
     * Returns an unmodifiable view of all registered inscriber recipes.
     */
    public static Iterable<RecipeHolder<InscriberRecipe>> getRecipes(World level) {
        return RecipeManager.of(level).byType(AERecipeTypes.INSCRIBER);
    }

    @Nullable
    public static InscriberRecipe findRecipe(World level, ItemStack input, ItemStack plateA, ItemStack plateB,
            boolean supportNamePress) {
        if (supportNamePress) {
            boolean isNameA = AEItems.NAME_PRESS.is(plateA);
            boolean isNameB = AEItems.NAME_PRESS.is(plateB);

            if (isNameA && isNameB || isNameA && plateB.isEmpty()) {
                return makeNamePressRecipe(input, plateA, plateB);
            } else if (plateA.isEmpty() && isNameB) {
                return makeNamePressRecipe(input, plateB, plateA);
            }
        }

        for (var holder : getRecipes(level)) {
            var recipe = holder.value();
            // The recipe can be flipped at will
            final boolean matchA = recipe.getTopOptional().test(plateA) && recipe.getBottomOptional().test(plateB);
            final boolean matchB = recipe.getTopOptional().test(plateB) && recipe.getBottomOptional().test(plateA);

            if ((matchA || matchB) && recipe.getMiddleInput().test(input)) {
                return recipe;
            }
        }

        return null;
    }

    private static InscriberRecipe makeNamePressRecipe(ItemStack input, ItemStack plateA, ItemStack plateB) {
        ITextComponent name = null;

        if (!plateA.isEmpty()) {
            var plateAName = AEComponents.NAME_PRESS_NAME.get(plateA);
            if (plateAName != null) {
                name = plateAName;
            }
        }

        if (!plateB.isEmpty()) {
            var plateBName = AEComponents.NAME_PRESS_NAME.get(plateB);
            if (plateBName != null) {
                if (name == null) {
                    name = plateBName;
                } else {
                    name = name.createCopy().appendText(" ").appendSibling(plateBName);
                }
            }
        }

        var startingItem = Ingredient.fromStacks(input.copy());
        var renamedItem = Platform.copyWithCount(input, 1);

        if (name != null) {
            AEComponents.CUSTOM_NAME.set(renamedItem, name);
        } else {
            AEComponents.CUSTOM_NAME.remove(renamedItem);
        }

        final InscriberProcessType type = InscriberProcessType.INSCRIBE;

        return new InscriberRecipe(startingItem, renamedItem,
                plateA.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(plateA),
                plateB.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(plateB), type);
    }

    /**
     * Checks if there is an inscriber recipe that supports the given combination of top/bottom presses. Both the given
     * combination and the reverse will be searched.
     */
    public static boolean isValidOptionalIngredientCombination(World level, ItemStack pressA, ItemStack pressB) {
        for (var holder : getRecipes(level)) {
            var recipe = holder.value();
            if (recipe.getTopOptional().test(pressA) && recipe.getBottomOptional().test(pressB)
                    || recipe.getTopOptional().test(pressB) && recipe.getBottomOptional().test(pressA)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if there is an inscriber recipe that would use the given item stack as an optional ingredient. Bottom and
     * top can be used interchangeably here, because the inscriber will flip the recipe if needed.
     */
    public static boolean isValidOptionalIngredient(World level, ItemStack is) {
        for (var holder : getRecipes(level)) {
            var recipe = holder.value();
            if (recipe.getTopOptional().test(is) || recipe.getBottomOptional().test(is)) {
                return true;
            }
        }

        return false;
    }

}
