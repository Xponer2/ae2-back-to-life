/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes.ingredient;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import appeng.core.tags.TagManager;

/**
 * Matches the items of an item tag ({@code {"tag": "c:ingots/iron"}} in recipe files), looked up when it is used so
 * tags filled in later (ore dictionary) count too.
 */
public final class TagIngredient extends Ingredient {
    private final ResourceLocation tag;
    @Nullable
    private ItemStack[] stacks;
    @Nullable
    private IntList packed;
    // Tag contents change until post init (ore names of other mods)
    private int generation = -1;

    public TagIngredient(ResourceLocation tag) {
        super(0);
        this.tag = tag;
    }

    public ResourceLocation getTag() {
        return tag;
    }

    @Override
    public ItemStack[] getMatchingStacks() {
        if (generation != TagManager.getGeneration()) {
            invalidate();
            generation = TagManager.getGeneration();
        }
        if (stacks == null) {
            stacks = TagManager.getItems(tag).toArray(new ItemStack[0]);
        }
        return stacks;
    }

    @Override
    public boolean apply(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && TagManager.isInItemTag(tag, stack.getItem(), stack.getMetadata());
    }

    @Override
    public IntList getValidItemStacksPacked() {
        if (generation != TagManager.getGeneration()) {
            invalidate();
            generation = TagManager.getGeneration();
        }
        if (packed == null) {
            packed = new IntArrayList();
            for (var stack : getMatchingStacks()) {
                packed.add(RecipeItemHelper.pack(stack));
            }
            packed.sort(null);
        }
        return packed;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    protected void invalidate() {
        stacks = null;
        packed = null;
    }

    @Override
    public String toString() {
        return "#" + tag;
    }
}
