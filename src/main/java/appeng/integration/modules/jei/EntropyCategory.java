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
import net.minecraft.util.text.TextFormatting;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;

import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.localization.ItemModText;

/**
 * Recipes of the entropy manipulator (heat / cool a block or fluid in the world).
 */
class EntropyCategory implements IRecipeCategory<EntropyRecipeWrapper> {

    static final String UID = AppEng.makeId("entropy_manipulator").toString();

    static final int PADDING = 5;
    private static final int WIDTH = 130 + 2 * PADDING;
    private static final int HEIGHT = 50 + 2 * PADDING;
    private static final int CENTER_X = WIDTH / 2;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    EntropyCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(AEItems.ENTROPY_MANIPULATOR.stack());
        this.slot = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 17, 24, 17);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return AEItems.ENTROPY_MANIPULATOR.stack().getDisplayName();
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
        arrow.draw(minecraft, CENTER_X - 12, PADDING + 14);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, EntropyRecipeWrapper recipeWrapper, IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        var fluidStacks = recipeLayout.getFluidStacks();
        int y = PADDING + 15;

        // The block or fluid the manipulator is used on
        int inputX = CENTER_X - 34;
        if (recipeWrapper.getInputFluid() != null) {
            fluidStacks.init(0, true, inputX, y, 16, 16, 1000, false, null);
            fluidStacks.setBackground(0, slot);
            fluidStacks.set(0, recipeWrapper.getInputFluid());
        } else {
            itemStacks.init(0, true, inputX - 1, y - 1);
            itemStacks.setBackground(0, slot);
            itemStacks.set(0, recipeWrapper.getInputItem());
        }

        int x = CENTER_X + 20;
        int itemIndex = 1;
        if (recipeWrapper.isInputConsumed()) {
            // Shown again with a cross over it
            if (recipeWrapper.getInputFluid() != null) {
                fluidStacks.init(1, false, x, y, 16, 16, 1000, false, null);
                fluidStacks.set(1, recipeWrapper.getInputFluid());
            } else {
                itemStacks.init(itemIndex, false, x - 1, y - 1);
                itemStacks.set(itemIndex, recipeWrapper.getInputItem());
                itemIndex++;
            }
            x += 18;
        }

        if (recipeWrapper.getOutputFluid() != null) {
            fluidStacks.init(2, false, x, y, 16, 16, 1000, false, null);
            fluidStacks.setBackground(2, slot);
            fluidStacks.set(2, recipeWrapper.getOutputFluid());
            x += 18;
        }
        for (var output : recipeWrapper.getOutputItems()) {
            itemStacks.init(itemIndex, false, x - 1, y - 1);
            itemStacks.setBackground(itemIndex, slot);
            itemStacks.set(itemIndex, output);
            itemIndex++;
            x += 18;
        }

        var consumed = recipeWrapper.isInputConsumed();
        var flowing = recipeWrapper.isInputFlowing();
        itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (consumed && !input && slotIndex == 1) {
                tooltip.add("" + TextFormatting.RED + TextFormatting.BOLD
                        + ItemModText.CONSUMED.text().getFormattedText());
            }
        });
        fluidStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (flowing && slotIndex == 0 && !tooltip.isEmpty()) {
                // The manipulator has to be used on the flowing variant of the fluid
                tooltip.set(0, ItemModText.FLOWING_FLUID_NAME.text(tooltip.get(0)).getFormattedText());
            }
            if (consumed && slotIndex == 1) {
                tooltip.add("" + TextFormatting.RED + TextFormatting.BOLD
                        + ItemModText.CONSUMED.text().getFormattedText());
            }
        });
    }
}
