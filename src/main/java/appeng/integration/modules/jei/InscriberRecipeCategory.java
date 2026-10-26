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
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;

import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;

/**
 * Same layout as the inscriber category of the 1.21 recipe viewer plugins: the part of the inscriber GUI with the
 * three input slots, the output slot and the progress bar.
 */
class InscriberRecipeCategory implements IRecipeCategory<InscriberRecipeWrapper> {

    static final String UID = AppEng.makeId("inscriber").toString();

    private static final int SLOT_INPUT_TOP = 0;
    private static final int SLOT_INPUT_MIDDLE = 1;
    private static final int SLOT_INPUT_BOTTOM = 2;
    private static final int SLOT_OUTPUT = 3;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    InscriberRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = AppEng.makeId("textures/guis/inscriber.png");
        this.background = guiHelper.createDrawable(location, 36, 20, 105, 54);
        this.icon = guiHelper.createDrawableIngredient(AEBlocks.INSCRIBER.stack());
        var progressDrawable = guiHelper.createDrawable(location, 177, 0, 6, 18);
        this.progress = guiHelper.createAnimatedDrawable(progressDrawable, 40,
                IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return AEBlocks.INSCRIBER.stack().getDisplayName();
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
        this.progress.draw(minecraft, 100, 19);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, InscriberRecipeWrapper recipeWrapper,
            IIngredients ingredients) {
        var itemStacks = recipeLayout.getItemStacks();
        // JEI positions are the 18x18 slot, one pixel left of / above the item
        itemStacks.init(SLOT_INPUT_TOP, true, 2, 2);
        itemStacks.init(SLOT_INPUT_MIDDLE, true, 26, 18);
        itemStacks.init(SLOT_INPUT_BOTTOM, true, 2, 34);
        itemStacks.init(SLOT_OUTPUT, false, 76, 19);
        itemStacks.set(ingredients);
    }
}
