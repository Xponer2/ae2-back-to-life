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

import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.blockentity.misc.CrankBlockEntity;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.ItemModText;

/**
 * Charger recipes, with the crank and the power it takes like in the 1.21 recipe viewer plugins.
 */
class ChargerCategory implements IRecipeCategory<ChargerRecipeWrapper> {

    static final String UID = AppEng.makeId("charger").toString();

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int SLOT_CRANK = 2;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    ChargerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(130, 50);
        this.icon = guiHelper.createDrawableIngredient(AEBlocks.CHARGER.stack());
        this.slot = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 17, 24, 17);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return AEBlocks.CHARGER.stack().getDisplayName();
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
        arrow.draw(minecraft, 52, 8);

        var turns = (ChargerBlockEntity.POWER_MAXIMUM_AMOUNT + CrankBlockEntity.POWER_PER_CRANK_TURN - 1)
                / CrankBlockEntity.POWER_PER_CRANK_TURN;
        var text = ItemModText.CHARGER_REQUIRED_POWER.text(turns, ChargerBlockEntity.POWER_MAXIMUM_AMOUNT);
        minecraft.fontRenderer.drawString(text.getFormattedText(), 20, 35, 0x7E7E7E);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, ChargerRecipeWrapper recipeWrapper,
            IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(SLOT_INPUT, true, 30, 7);
        itemStacks.setBackground(SLOT_INPUT, slot);
        itemStacks.init(SLOT_OUTPUT, false, 80, 7);
        itemStacks.setBackground(SLOT_OUTPUT, slot);
        itemStacks.set(ingredients);

        // The crank powers the charger by hand; shown without slot background, it is no ingredient
        itemStacks.init(SLOT_CRANK, true, 2, 29);
        itemStacks.set(SLOT_CRANK, AEBlocks.CRANK.stack());
    }
}
