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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

import appeng.core.localization.ItemModText;
import appeng.integration.modules.itemlists.CraftingHelper;
import appeng.menu.me.items.CraftingTermMenu;

/**
 * Moves the ingredients of a crafting recipe from the network into the crafting grid of a crafting terminal (like
 * {@code UseCraftingRecipeTransfer} of the 1.21 REI plugin). Ctrl-click also requests the missing craftable items.
 * <p>
 * JEI 4 can only show errors that block the transfer, so missing ingredients are only reported when none of the
 * ingredients is available.
 */
class CraftingTermTransferHandler<T extends CraftingTermMenu> implements IRecipeTransferHandler<T> {

    private static final int CRAFTING_GRID_SIZE = 9;
    // Slot 0 of the JEI crafting category is the output, 1-9 are the crafting grid
    private static final int FIRST_INPUT_SLOT = 1;

    private final Class<T> containerClass;
    private final IRecipeTransferHandlerHelper helper;

    CraftingTermTransferHandler(Class<T> containerClass, IRecipeTransferHandlerHelper helper) {
        this.containerClass = containerClass;
        this.helper = helper;
    }

    @Override
    public Class<T> getContainerClass() {
        return containerClass;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(T menu, IRecipeLayout recipeLayout, EntityPlayer player,
            boolean maxTransfer, boolean doTransfer) {
        var ingredients = getGridIngredients(recipeLayout);

        Map<Integer, Ingredient> slotToIngredient = new HashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            if (ingredients.get(i).getMatchingStacks().length > 0) {
                slotToIngredient.put(i, ingredients.get(i));
            }
        }

        var missingSlots = menu.findMissingIngredients(slotToIngredient);
        if (!slotToIngredient.isEmpty() && missingSlots.missingSlots().size() == slotToIngredient.size()) {
            // All missing, can't do much...
            var jeiSlots = new ArrayList<Integer>();
            for (var slot : missingSlots.missingSlots()) {
                jeiSlots.add(slot + FIRST_INPUT_SLOT);
            }
            return helper.createUserErrorForSlots(ItemModText.NO_ITEMS.text().getFormattedText(), jeiSlots);
        }

        if (doTransfer) {
            // Thank you RS for pioneering this amazing feature! :)
            boolean craftMissing = GuiScreen.isCtrlKeyDown();
            CraftingHelper.performTransfer(menu, null, ingredients, craftMissing);
        }

        return null;
    }

    private static List<Ingredient> getGridIngredients(IRecipeLayout recipeLayout) {
        var result = new ArrayList<Ingredient>(CRAFTING_GRID_SIZE);
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            result.add(Ingredient.EMPTY);
        }

        for (var entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
            var gridSlot = entry.getKey() - FIRST_INPUT_SLOT;
            var ingredient = entry.getValue();
            if (!ingredient.isInput() || gridSlot < 0 || gridSlot >= CRAFTING_GRID_SIZE) {
                continue;
            }
            var stacks = ingredient.getAllIngredients().stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .toArray(ItemStack[]::new);
            if (stacks.length > 0) {
                result.set(gridSlot, Ingredient.fromStacks(stacks));
            }
        }
        return result;
    }
}
