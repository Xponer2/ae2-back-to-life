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
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;

/**
 * Encodes the recipe shown in JEI into the pattern encoding terminal (like {@code EncodePatternTransferHandler} of
 * the 1.21 REI plugin): crafting recipes as crafting patterns, stonecutting recipes as stonecutting patterns, every
 * other category as a processing pattern with all its item and fluid inputs and outputs. Registered for all
 * categories.
 */
class PatternEncodingTransferHandler implements IRecipeTransferHandler<PatternEncodingTermMenu> {

    private static final int CRAFTING_GRID_SIZE = 9;
    // Slot 0 of the JEI crafting category is the output, 1-9 are the crafting grid
    private static final int FIRST_INPUT_SLOT = 1;

    @Override
    public Class<PatternEncodingTermMenu> getContainerClass() {
        return PatternEncodingTermMenu.class;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, IRecipeLayout recipeLayout,
            EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }

        var category = recipeLayout.getRecipeCategory();
        if (StonecuttingCategory.UID.equals(category.getUid()) && encodeStonecutting(menu, recipeLayout)) {
            return null;
        }
        if (VanillaRecipeCategoryUid.CRAFTING.equals(category.getUid())) {
            EncodingHelper.encodeCraftingRecipe(menu, null, getGridIngredients(recipeLayout), stack -> true);
        } else {
            var inputs = new ArrayList<List<GenericStack>>();
            var outputs = new ArrayList<GenericStack>();
            collect(sorted(recipeLayout.getItemStacks().getGuiIngredients()), inputs, outputs,
                    GenericStack::fromItemStack);
            collect(sorted(recipeLayout.getFluidStacks().getGuiIngredients()), inputs, outputs,
                    PatternEncodingTransferHandler::fromFluidStack);
            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        }
        return null;
    }

    /**
     * Stonecutting patterns need the id of the recipe, which is looked up from the shown input and output.
     */
    private static boolean encodeStonecutting(PatternEncodingTermMenu menu, IRecipeLayout recipeLayout) {
        var slots = recipeLayout.getItemStacks().getGuiIngredients();
        var input = slots.get(0);
        var output = slots.get(1);
        if (input == null || output == null || output.getDisplayedIngredient() == null) {
            return false;
        }
        var holder = StonecuttingCategory.findRecipe(input.getAllIngredients(), output.getDisplayedIngredient());
        if (holder == null) {
            return false;
        }
        var grid = new ArrayList<List<GenericStack>>();
        var inputs = new ArrayList<GenericStack>();
        for (var stack : input.getAllIngredients()) {
            var genericStack = GenericStack.fromItemStack(stack);
            if (genericStack != null) {
                inputs.add(genericStack);
            }
        }
        grid.add(inputs);
        EncodingHelper.encodeCraftingRecipe(menu, holder, grid, stack -> true);
        return true;
    }

    private static List<List<GenericStack>> getGridIngredients(IRecipeLayout recipeLayout) {
        var result = new ArrayList<List<GenericStack>>(CRAFTING_GRID_SIZE);
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            result.add(new ArrayList<>());
        }
        for (var entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
            var gridSlot = entry.getKey() - FIRST_INPUT_SLOT;
            if (!entry.getValue().isInput() || gridSlot < 0 || gridSlot >= CRAFTING_GRID_SIZE) {
                continue;
            }
            for (var stack : entry.getValue().getAllIngredients()) {
                var genericStack = GenericStack.fromItemStack(stack);
                if (genericStack != null) {
                    result.get(gridSlot).add(genericStack);
                }
            }
        }
        return result;
    }

    private static <T> Map<Integer, ? extends IGuiIngredient<T>> sorted(
            Map<Integer, ? extends IGuiIngredient<T>> ingredients) {
        return new TreeMap<>(ingredients);
    }

    private static <T> void collect(Map<Integer, ? extends IGuiIngredient<T>> ingredients,
            List<List<GenericStack>> inputs, List<GenericStack> outputs,
            java.util.function.Function<T, GenericStack> converter) {
        for (var ingredient : ingredients.values()) {
            if (ingredient.isInput()) {
                var possible = new ArrayList<GenericStack>();
                for (var value : ingredient.getAllIngredients()) {
                    var stack = value != null ? converter.apply(value) : null;
                    if (stack != null) {
                        possible.add(stack);
                    }
                }
                if (!possible.isEmpty()) {
                    inputs.add(possible);
                }
            } else {
                // Only one result per output slot: the one JEI shows
                var displayed = ingredient.getDisplayedIngredient();
                var stack = displayed != null ? converter.apply(displayed) : null;
                if (stack != null) {
                    outputs.add(stack);
                }
            }
        }
    }

    @Nullable
    private static GenericStack fromFluidStack(FluidStack fluidStack) {
        return fluidStack.amount > 0 ? GenericStack.fromFluidStack(fluidStack) : null;
    }
}
