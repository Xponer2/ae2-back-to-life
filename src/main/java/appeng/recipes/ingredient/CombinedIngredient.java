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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import appeng.core.tags.TagManager;

/**
 * Ingredients made of other ingredients: any of them (a list in recipe files, {@code neoforge:compound}) or the items
 * of one without the items of another ({@code neoforge:difference}).
 */
public final class CombinedIngredient extends Ingredient {
    private final List<Ingredient> included;
    @Nullable
    private final Ingredient excluded;
    @Nullable
    private ItemStack[] stacks;
    @Nullable
    private IntList packed;
    // The children may be tags, whose contents change until post init
    private int generation = -1;

    private CombinedIngredient(List<Ingredient> included, @Nullable Ingredient excluded) {
        super(0);
        this.included = included;
        this.excluded = excluded;
    }

    public static Ingredient anyOf(List<Ingredient> ingredients) {
        return ingredients.size() == 1 ? ingredients.get(0) : new CombinedIngredient(ingredients, null);
    }

    public static Ingredient difference(Ingredient base, Ingredient subtracted) {
        List<Ingredient> included = new ArrayList<>();
        included.add(base);
        return new CombinedIngredient(included, subtracted);
    }

    public List<Ingredient> getIncluded() {
        return included;
    }

    @Nullable
    public Ingredient getExcluded() {
        return excluded;
    }

    @Override
    public boolean apply(@Nullable ItemStack stack) {
        if (stack == null || excluded != null && excluded.apply(stack)) {
            return false;
        }
        for (var ingredient : included) {
            if (ingredient.apply(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack[] getMatchingStacks() {
        checkGeneration();
        if (stacks == null) {
            var result = new ArrayList<ItemStack>();
            for (var ingredient : included) {
                for (var stack : ingredient.getMatchingStacks()) {
                    if (excluded == null || !excluded.apply(stack)) {
                        result.add(stack);
                    }
                }
            }
            stacks = result.toArray(new ItemStack[0]);
        }
        return stacks;
    }

    @Override
    public IntList getValidItemStacksPacked() {
        checkGeneration();
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

    private void checkGeneration() {
        if (generation != TagManager.getGeneration()) {
            invalidate();
            generation = TagManager.getGeneration();
        }
    }

    @Override
    protected void invalidate() {
        stacks = null;
        packed = null;
    }
}
