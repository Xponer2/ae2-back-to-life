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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;

import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

import appeng.core.FacadeCreativeTab;
import appeng.items.parts.FacadeItem;

/**
 * This plugin will dynamically add facade recipes for any item that can be turned into a facade.
 */
class FacadeRegistryPlugin implements IRecipeRegistryPlugin {

    private final FacadeItem itemFacade;
    private final ItemStack cableAnchor;

    FacadeRegistryPlugin(FacadeItem itemFacade, ItemStack cableAnchor) {
        this.itemFacade = itemFacade;
        this.cableAnchor = cableAnchor;
    }

    @Override
    public <V> List<String> getRecipeCategoryUids(IFocus<V> focus) {
        if (!(focus.getValue() instanceof ItemStack itemStack)) {
            return Collections.emptyList();
        }
        if (focus.getMode() == IFocus.Mode.OUTPUT) {
            // Looking up how a certain facade is crafted
            if (itemStack.getItem() instanceof FacadeItem) {
                return Collections.singletonList(VanillaRecipeCategoryUid.CRAFTING);
            }
        } else if (focus.getMode() == IFocus.Mode.INPUT) {
            // Looking up if a certain block can be used to make a facade, or what cable anchors are used for
            if (!this.itemFacade.createFacadeForItem(itemStack, true).isEmpty()
                    || isCableAnchor(itemStack)) {
                return Collections.singletonList(VanillaRecipeCategoryUid.CRAFTING);
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory,
            IFocus<V> focus) {
        if (!VanillaRecipeCategoryUid.CRAFTING.equals(recipeCategory.getUid())
                || !(focus.getValue() instanceof ItemStack itemStack)) {
            return Collections.emptyList();
        }

        if (focus.getMode() == IFocus.Mode.OUTPUT) {
            // Looking up how a certain facade is crafted
            if (itemStack.getItem() instanceof FacadeItem facadeItem) {
                ItemStack textureItem = facadeItem.getTextureItem(itemStack);
                return Collections.singletonList(
                        (T) new FacadeRecipeWrapper(textureItem, this.cableAnchor, itemStack));
            }
        } else if (focus.getMode() == IFocus.Mode.INPUT) {
            // Looking up if a certain block can be used to make a facade
            var input = itemStack.copy();
            input.setCount(1);
            ItemStack facade = this.itemFacade.createFacadeForItem(input, false);
            if (!facade.isEmpty()) {
                return Collections.singletonList((T) new FacadeRecipeWrapper(input, this.cableAnchor, facade));
            }

            // Looking up uses for cable anchors
            if (isCableAnchor(itemStack)) {
                return FacadeCreativeTab.getDisplayItems().stream()
                        .map(stack -> {
                            if (stack.getItem() instanceof FacadeItem facadeItem) {
                                ItemStack textureItem = facadeItem.getTextureItem(stack);
                                return (T) new FacadeRecipeWrapper(textureItem, this.cableAnchor, stack);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    @Override
    public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory) {
        return Collections.emptyList();
    }

    private boolean isCableAnchor(ItemStack stack) {
        return stack.getItem() == cableAnchor.getItem();
    }
}
