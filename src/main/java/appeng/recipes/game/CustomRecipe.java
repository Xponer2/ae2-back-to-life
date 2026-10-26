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

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

import appeng.recipes.RecipeSerializer;
import appeng.util.CraftingInput;

/**
 * A crafting table recipe written against the crafting recipe API of newer versions ({@link CraftingInput} trimmed to
 * the used area, {@code assemble}, {@code getResultItem}, ...), registered as a 1.12.2 {@link IRecipe}.
 */
public abstract class CustomRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    public abstract boolean matches(CraftingInput input, World level);

    public abstract ItemStack assemble(CraftingInput input);

    public abstract boolean canCraftInDimensions(int width, int height);

    public abstract RecipeSerializer<?> getSerializer();

    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    /**
     * The items left in the grid, per stack of the (trimmed) input. By default the container items (empty buckets).
     */
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < result.size(); i++) {
            result.set(i, ForgeHooks.getContainerItem(input.getItem(i)));
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------ IRecipe

    @Override
    public final boolean matches(InventoryCrafting inv, World world) {
        return matches(CraftingInput.of(inv), world);
    }

    @Override
    public final ItemStack getCraftingResult(InventoryCrafting inv) {
        return assemble(CraftingInput.of(inv));
    }

    @Override
    public final boolean canFit(int width, int height) {
        return canCraftInDimensions(width, height);
    }

    @Override
    public final ItemStack getRecipeOutput() {
        return getResultItem();
    }

    /**
     * Maps the remaining items of the trimmed input back onto the slots of the crafting grid.
     */
    @Override
    public final NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        var positioned = CraftingInput.ofPositioned(inv);
        var input = positioned.input();
        var remaining = getRemainingItems(input);
        NonNullList<ItemStack> result = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int slot = x + positioned.left() + (y + positioned.top()) * inv.getWidth();
                result.set(slot, remaining.get(x + y * input.width()));
            }
        }
        return result;
    }

    /**
     * Special recipes don't show in the recipe book.
     */
    @Override
    public boolean isDynamic() {
        return true;
    }
}
