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

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;

import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.recipes.vanilla.SingleRecipeInput;
import appeng.recipes.vanilla.StonecutterRecipe;
import appeng.recipes.vanilla.VanillaRecipes;
import appeng.util.RecipeHolder;

/**
 * 1.12.2 only: the stonecutting recipes of the recipe files. 1.12.2 has no stonecutter, they are only used by
 * stonecutting patterns in molecular assemblers, so JEI can't show them otherwise.
 */
class StonecuttingCategory implements IRecipeCategory<StonecuttingRecipeWrapper> {

    static final String UID = AppEng.makeId("stonecutting").toString();

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    StonecuttingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(82, 26);
        this.icon = guiHelper.createDrawableIngredient(AEItems.STONECUTTING_PATTERN.stack());
        this.slot = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 17, 24, 17);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return new TextComponentTranslation("ae2.jei.stonecutting").getFormattedText();
    }

    @Override
    public String getModName() {
        return AppEng.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        arrow.draw(minecraft, 27, 4);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, StonecuttingRecipeWrapper recipeWrapper,
            IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(SLOT_INPUT, true, 3, 4);
        itemStacks.setBackground(SLOT_INPUT, slot);
        itemStacks.init(SLOT_OUTPUT, false, 60, 4);
        itemStacks.setBackground(SLOT_OUTPUT, slot);
        itemStacks.set(ingredients);
    }

    /**
     * Finds the recipe shown in a layout of this category (JEI only hands the layout to transfer handlers).
     */
    @Nullable
    static RecipeHolder<StonecutterRecipe> findRecipe(List<ItemStack> inputs, ItemStack output) {
        for (var input : inputs) {
            for (var holder : VanillaRecipes.getStonecuttingRecipesFor(new SingleRecipeInput(input), null)) {
                if (ItemStack.areItemStacksEqual(holder.value().getResultItem(), output)) {
                    return holder;
                }
            }
        }
        return null;
    }
}
