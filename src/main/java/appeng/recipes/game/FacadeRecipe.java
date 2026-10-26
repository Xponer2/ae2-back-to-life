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

package appeng.recipes.game;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.ItemDefinition;
import appeng.items.parts.FacadeItem;
import appeng.recipes.RecipeSerializer;
import appeng.util.CraftingInput;
import com.mojang.serialization.MapCodec;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

public final class FacadeRecipe extends CustomRecipe {
    // The recipe file has no data, the facade item is looked up when it's loaded (after item registration)
    public static RecipeSerializer<FacadeRecipe> SERIALIZER = new RecipeSerializer<FacadeRecipe>() {
        @Override
        public MapCodec<FacadeRecipe> codec() {
            return MapCodec.unit(() -> new FacadeRecipe(AEItems.FACADE.get()));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FacadeRecipe> streamCodec() {
            return StreamCodec.of((buffer, recipe) -> {
            }, buffer -> new FacadeRecipe(AEItems.FACADE.get()));
        }
    };

    private final ItemDefinition<?> anchor = AEParts.CABLE_ANCHOR;
    private final FacadeItem facade;

    public FacadeRecipe(FacadeItem facade) {
        this.facade = facade;
    }

    @Override
    public boolean matches(CraftingInput inv, World level) {
        return !this.getOutput(inv, false).isEmpty();
    }

    private ItemStack getOutput(CraftingInput inv, boolean createFacade) {
        if (inv.width() == 3 && inv.height() == 3 && inv.getItem(0).isEmpty() && inv.getItem(2).isEmpty()
                && inv.getItem(6).isEmpty() && inv.getItem(8).isEmpty()) {
            if (this.anchor.is(inv.getItem(1)) && this.anchor.is(inv.getItem(3))
                    && this.anchor.is(inv.getItem(5)) && this.anchor.is(inv.getItem(7))) {
                final ItemStack facades = this.facade.createFacadeForItem(inv.getItem(4), !createFacade);
                if (!facades.isEmpty() && createFacade) {
                    facades.setCount(4);
                }
                return facades;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        return this.getOutput(inv, true);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<FacadeRecipe> getSerializer() {
        return SERIALIZER;
    }

}
