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
import appeng.core.definitions.AEParts;
import appeng.core.localization.ItemModText;

/**
 * Shows which items attune a P2P tunnel to which tunnel type.
 */
class AttunementCategory implements IRecipeCategory<AttunementWrapper> {

    static final String UID = AppEng.makeId("attunement").toString();

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    AttunementCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(86, 26);
        this.icon = guiHelper.createDrawableIngredient(AEParts.ME_P2P_TUNNEL.stack());
        this.slot = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 17, 24, 17);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return ItemModText.P2P_TUNNEL_ATTUNEMENT.text().getFormattedText();
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
    public void setRecipe(IRecipeLayout recipeLayout, AttunementWrapper recipeWrapper, IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(SLOT_INPUT, true, 3, 4);
        itemStacks.setBackground(SLOT_INPUT, slot);
        itemStacks.init(SLOT_OUTPUT, false, 60, 4);
        itemStacks.setBackground(SLOT_OUTPUT, slot);
        itemStacks.set(ingredients);

        // The description of the attunement goes into the tooltip of the items
        itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == SLOT_INPUT) {
                for (var line : recipeWrapper.getDescription()) {
                    tooltip.add(line.getFormattedText());
                }
            }
        });
    }
}
