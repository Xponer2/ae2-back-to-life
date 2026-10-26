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

import net.minecraft.client.Minecraft;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;

import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.localization.ItemModText;

/**
 * Recipes that happen in the world: items thrown into a fluid or blown up (layout of the 1.21 REI category).
 */
class TransformCategory implements IRecipeCategory<TransformRecipeWrapper> {

    static final String UID = AppEng.makeId("item_transformation").toString();

    private static final int WIDTH = 150;
    private static final int HEIGHT = 72;

    private static final int COL1 = 10;
    private static final int COL2 = COL1 + 25;
    private static final int COL3 = COL2 + 24 + 6;
    private static final int COL4 = COL3 + 16 + 5;
    private static final int COL5 = COL4 + 24 + 10;
    private static final int Y_OFFSET = 28;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    TransformCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.stack());
        this.slot = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 17, 24, 17);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return ItemModText.TRANSFORM_CATEGORY.text().getFormattedText();
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
        // Arrow pointing into the fluid / explosion, then to the result
        arrow.draw(minecraft, COL2, Y_OFFSET);
        arrow.draw(minecraft, COL4, Y_OFFSET);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, TransformRecipeWrapper recipeWrapper,
            IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        var inputs = ingredients.getInputs(mezz.jei.api.ingredients.VanillaTypes.ITEM);

        // First column contains ingredients
        int x = COL1;
        int y = 10;
        if (inputs.size() < 3) {
            // so ingredients lists with less than two rows get centered vertically
            y += 9 * (3 - inputs.size());
        }
        int index = 0;
        for (var input : inputs) {
            itemStacks.init(index, true, x - 1, y - 1);
            itemStacks.setBackground(index, slot);
            itemStacks.set(index, input);
            index++;
            y += 18;
            if (y >= 64) {
                // we don't actually have room to make multiple columns of ingredients look nice,
                // but this is better than just overflowing downwards.
                y -= 54;
                x += 18;
            }
        }

        // The result
        int outputIndex = index++;
        itemStacks.init(outputIndex, false, COL5 - 1, Y_OFFSET - 1);
        itemStacks.setBackground(outputIndex, slot);
        itemStacks.set(outputIndex, ingredients.getOutputs(mezz.jei.api.ingredients.VanillaTypes.ITEM).get(0));

        // The fluid or the explosives in the middle
        var fluids = recipeWrapper.getCatalystFluids();
        if (!fluids.isEmpty()) {
            var fluidStacks = recipeLayout.getFluidStacks();
            fluidStacks.init(0, true, COL3, Y_OFFSET, 16, 16, 1000, false, null);
            fluidStacks.set(0, fluids);
        }
        var explosives = recipeWrapper.getCatalystItems();
        if (!explosives.isEmpty()) {
            itemStacks.init(index, true, COL3 - 1, Y_OFFSET - 1);
            itemStacks.set(index, explosives);
        }
    }
}
