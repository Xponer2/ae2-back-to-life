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

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;

import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.GuiText;

class CondenserCategory implements IRecipeCategory<CondenserOutputWrapper> {

    static final String UID = AppEng.makeId("condenser").toString();

    static final int PADDING = 7;

    private static final int SLOT_OUTPUT = 0;
    private static final int SLOT_STORAGE_COMPONENT = 1;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable iconTrash;
    private final IDrawable iconButton;
    private final IDrawableAnimated progress;

    CondenserCategory(IGuiHelper guiHelper) {
        ResourceLocation location = AppEng.makeId("textures/guis/condenser.png");
        this.background = guiHelper.drawableBuilder(location, 48, 25, 96, 48)
                .addPadding(PADDING, PADDING, PADDING, PADDING)
                .build();
        this.icon = guiHelper.createDrawableIngredient(AEBlocks.CONDENSER.stack());

        ResourceLocation statesLocation = AppEng.makeId("textures/guis/states.png");
        this.iconTrash = guiHelper.createDrawable(statesLocation, 241, 81, 14, 14);
        this.iconButton = guiHelper.createDrawable(statesLocation, 240, 240, 16, 16);

        var progressDrawable = guiHelper.createDrawable(location, 176, 0, 6, 18);
        this.progress = guiHelper.createAnimatedDrawable(progressDrawable, 40,
                IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return GuiText.Condenser.text().getFormattedText();
    }

    @Override
    public String getModName() {
        return AppEng.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        this.iconTrash.draw(minecraft, PADDING + 4, PADDING + 28);
        this.iconButton.draw(minecraft, PADDING + 80, PADDING + 28);
        this.progress.draw(minecraft, PADDING + 72, PADDING);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CondenserOutputWrapper recipeWrapper,
            IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(SLOT_OUTPUT, false, PADDING + 56, PADDING + 26);
        itemStacks.init(SLOT_STORAGE_COMPONENT, true, PADDING + 52, PADDING);
        itemStacks.set(ingredients);
        itemStacks.set(SLOT_STORAGE_COMPONENT, recipeWrapper.getViableStorageComponents());
    }
}
