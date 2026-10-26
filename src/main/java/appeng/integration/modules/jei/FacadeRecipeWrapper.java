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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper;

import appeng.core.AppEng;

/**
 * The crafting recipe of a facade: four cable anchors around the block the facade looks like.
 */
class FacadeRecipeWrapper implements IShapedCraftingRecipeWrapper {

    private final ItemStack textureItem;
    private final ItemStack cableAnchor;
    private final ItemStack facade;

    FacadeRecipeWrapper(ItemStack textureItem, ItemStack cableAnchor, ItemStack facade) {
        this.textureItem = textureItem;
        this.cableAnchor = cableAnchor;
        this.facade = facade.copy();
        this.facade.setCount(4);
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public ResourceLocation getRegistryName() {
        // Only used inside JEI
        return AppEng.makeId("facade/" + Item.getIdFromItem(textureItem.getItem()));
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<ItemStack> input = new ArrayList<>(9);
        input.add(ItemStack.EMPTY);
        input.add(this.cableAnchor);
        input.add(ItemStack.EMPTY);
        input.add(this.cableAnchor);
        input.add(this.textureItem);
        input.add(this.cableAnchor);
        input.add(ItemStack.EMPTY);
        input.add(this.cableAnchor);
        input.add(ItemStack.EMPTY);

        ingredients.setInputs(VanillaTypes.ITEM, input);
        ingredients.setOutput(VanillaTypes.ITEM, this.facade);
    }
}
