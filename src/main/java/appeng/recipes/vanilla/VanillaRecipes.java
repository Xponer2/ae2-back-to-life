/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes.vanilla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import appeng.util.RecipeHolder;

/**
 * Stonecutting and smithing recipes, which 1.12.2 doesn't have (replacement for the matching parts of the 1.21
 * {@code RecipeManager}). Filled by the recipe loader from the 1.21 recipe JSON in {@code data/<ns>/recipe}.
 */
public final class VanillaRecipes {
    private static final Map<ResourceLocation, RecipeHolder<StonecutterRecipe>> STONECUTTING = new LinkedHashMap<>();
    private static final Map<ResourceLocation, RecipeHolder<SmithingRecipe>> SMITHING = new LinkedHashMap<>();

    private VanillaRecipes() {
    }

    public static synchronized void clear() {
        STONECUTTING.clear();
        SMITHING.clear();
    }

    public static synchronized void addStonecutting(ResourceLocation id, StonecutterRecipe recipe) {
        STONECUTTING.put(id, new RecipeHolder<>(id, recipe));
    }

    public static synchronized void addSmithing(ResourceLocation id, SmithingRecipe recipe) {
        SMITHING.put(id, new RecipeHolder<>(id, recipe));
    }

    @Nullable
    public static synchronized RecipeHolder<StonecutterRecipe> getStonecutting(ResourceLocation id) {
        return STONECUTTING.get(id);
    }

    @Nullable
    public static synchronized RecipeHolder<SmithingRecipe> getSmithing(ResourceLocation id) {
        return SMITHING.get(id);
    }

    public static synchronized List<RecipeHolder<StonecutterRecipe>> getAllStonecutting() {
        return Collections.unmodifiableList(new ArrayList<>(STONECUTTING.values()));
    }

    public static synchronized List<RecipeHolder<SmithingRecipe>> getAllSmithing() {
        return Collections.unmodifiableList(new ArrayList<>(SMITHING.values()));
    }

    /**
     * Like {@code recipeManager.getRecipesFor(RecipeType.STONECUTTING, input, level)}, in load order.
     */
    public static synchronized List<RecipeHolder<StonecutterRecipe>> getStonecuttingRecipesFor(SingleRecipeInput input,
            World level) {
        var result = new ArrayList<RecipeHolder<StonecutterRecipe>>();
        for (var holder : STONECUTTING.values()) {
            if (holder.value().matches(input, level)) {
                result.add(holder);
            }
        }
        return result;
    }

    /**
     * Like {@code recipeManager.getRecipeFor(RecipeType.STONECUTTING, input, level, recipeId)}.
     */
    @Nullable
    public static synchronized RecipeHolder<StonecutterRecipe> getStonecuttingRecipeFor(SingleRecipeInput input,
            World level, ResourceLocation recipeId) {
        var holder = STONECUTTING.get(recipeId);
        return holder != null && holder.value().matches(input, level) ? holder : null;
    }

    /**
     * Like {@code recipeManager.getRecipeFor(RecipeType.SMITHING, input, level)}.
     */
    @Nullable
    public static synchronized RecipeHolder<SmithingRecipe> getSmithingRecipeFor(SmithingRecipeInput input,
            World level) {
        for (var holder : SMITHING.values()) {
            if (holder.value().matches(input, level)) {
                return holder;
            }
        }
        return null;
    }

    public static boolean isSmithingTemplate(ItemStack stack) {
        for (var holder : getAllSmithing()) {
            if (holder.value().isTemplateIngredient(stack)) {
                return true;
            }
        }
        return false;
    }
}
