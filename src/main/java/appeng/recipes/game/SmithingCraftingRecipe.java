/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes.game;

import com.mojang.serialization.MapCodec;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.vanilla.SmithingRecipe;
import appeng.util.CraftingInput;

/**
 * There is no smithing table in this version: a smithing recipe can also be crafted from its template, base and
 * addition anywhere in the crafting grid. The result keeps the data of the base (enchantments, name, damage), like
 * the smithing table of newer versions.
 */
public final class SmithingCraftingRecipe extends CustomRecipe {
    private final SmithingRecipe smithing;

    public SmithingCraftingRecipe(SmithingRecipe smithing) {
        this.smithing = smithing;
    }

    public SmithingRecipe getSmithingRecipe() {
        return smithing;
    }

    @Override
    public boolean matches(CraftingInput input, World level) {
        return findBase(input) != null;
    }

    /**
     * @return The base item if the grid holds exactly the template, base and addition.
     */
    private ItemStack findBase(CraftingInput input) {
        if (input.ingredientCount() != 3) {
            return null;
        }
        boolean template = false;
        boolean addition = false;
        ItemStack base = null;
        for (var stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!template && smithing.getTemplate().apply(stack)) {
                template = true;
            } else if (base == null && smithing.getBase().apply(stack)) {
                base = stack;
            } else if (!addition && smithing.getAddition().apply(stack)) {
                addition = true;
            } else {
                return null;
            }
        }
        return template && addition ? base : null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        var base = findBase(input);
        if (base == null) {
            return ItemStack.EMPTY;
        }
        var result = smithing.getResultItem().copy();
        if (base.hasTagCompound()) {
            result.setTagCompound(base.getTagCompound().copy());
        }
        if (result.isItemStackDamageable() && base.isItemStackDamageable()) {
            result.setItemDamage(Math.min(base.getItemDamage(), result.getMaxDamage() - 1));
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem() {
        return smithing.getResultItem();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, smithing.getTemplate(), smithing.getBase(), smithing.getAddition());
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    /**
     * Not read from recipe files, the recipe loader creates it for every smithing recipe.
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return new RecipeSerializer<SmithingCraftingRecipe>() {
            @Override
            public MapCodec<SmithingCraftingRecipe> codec() {
                throw new UnsupportedOperationException();
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, SmithingCraftingRecipe> streamCodec() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
