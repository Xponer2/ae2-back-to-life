/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/**
 * 1.12.2 replacement for the 1.21 {@code CraftingInput}: a width x height grid of stacks, trimmed to the smallest
 * rectangle containing all non-empty stacks (like 1.21). 1.12.2 recipes take an {@link InventoryCrafting}, see
 * {@link #toInventory()}, {@link #findRecipe}, {@link #assemble} and {@link #getRemainingItems}.
 */
public final class CraftingInput {
    public static final CraftingInput EMPTY = new CraftingInput(0, 0, Collections.emptyList());

    private final int width;
    private final int height;
    private final List<ItemStack> items;

    private CraftingInput(int width, int height, List<ItemStack> items) {
        this.width = width;
        this.height = height;
        this.items = items;
    }

    public static CraftingInput of(int width, int height, List<ItemStack> items) {
        return ofPositioned(width, height, items).input();
    }

    public static Positioned ofPositioned(int width, int height, List<ItemStack> items) {
        if (width == 0 || height == 0) {
            return Positioned.EMPTY;
        }
        int left = width - 1, right = 0, top = height - 1, bottom = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!items.get(x + y * width).isEmpty()) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left) {
            return Positioned.EMPTY;
        }
        int w = right - left + 1;
        int h = bottom - top + 1;
        if (w == width && h == height) {
            return new Positioned(new CraftingInput(width, height, items), left, top);
        }
        var trimmed = new ArrayList<ItemStack>(w * h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                trimmed.add(items.get(x + left + (y + top) * width));
            }
        }
        return new Positioned(new CraftingInput(w, h, trimmed), left, top);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        for (var item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack getItem(int index) {
        return items.get(index);
    }

    public ItemStack getItem(int x, int y) {
        return items.get(x + y * width);
    }

    public List<ItemStack> items() {
        return items;
    }

    /**
     * A new crafting inventory with copies of the stacks, for the 1.12.2 recipe methods.
     */
    public InventoryCrafting toInventory() {
        var inv = new InventoryCrafting(NoopContainer.INSTANCE, Math.max(width, 1), Math.max(height, 1));
        for (int i = 0; i < items.size(); i++) {
            inv.setInventorySlotContents(i, items.get(i).copy());
        }
        return inv;
    }

    /**
     * Like the 1.21 {@code RecipeManager.getRecipeFor(RecipeType.CRAFTING, input, level)}.
     */
    @Nullable
    public RecipeHolder<IRecipe> findRecipe(World level) {
        var recipe = CraftingManager.findMatchingRecipe(toInventory(), level);
        if (recipe == null || recipe.getRegistryName() == null) {
            return null;
        }
        return new RecipeHolder<>(recipe.getRegistryName(), recipe);
    }

    public boolean matches(IRecipe recipe, World level) {
        return recipe.matches(toInventory(), level);
    }

    public ItemStack assemble(IRecipe recipe) {
        return recipe.getCraftingResult(toInventory());
    }

    public NonNullList<ItemStack> getRemainingItems(IRecipe recipe) {
        return recipe.getRemainingItems(toInventory());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CraftingInput other) || width != other.width || height != other.height
                || items.size() != other.items.size()) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!ItemStack.areItemStacksEqual(items.get(i), other.items.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = width * 31 + height;
        for (var item : items) {
            result = result * 31 + (item.isEmpty() ? 0 : item.getItem().hashCode() * 31 + item.getMetadata());
        }
        return result;
    }

    public static final class Positioned {
        public static final Positioned EMPTY = new Positioned(CraftingInput.EMPTY, 0, 0);

        private final CraftingInput input;
        private final int left;
        private final int top;

        public Positioned(CraftingInput input, int left, int top) {
            this.input = input;
            this.left = left;
            this.top = top;
        }

        public CraftingInput input() {
            return input;
        }

        public int left() {
            return left;
        }

        public int top() {
            return top;
        }
    }

    /**
     * Container for the crafting inventories created here, nobody listens to them.
     */
    private static final class NoopContainer extends Container {
        static final NoopContainer INSTANCE = new NoopContainer();

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }

        @Override
        public void onCraftMatrixChanged(IInventory inventory) {
        }
    }
}
