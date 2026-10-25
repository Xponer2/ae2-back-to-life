/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import appeng.recipes.vanilla.VanillaRecipes;
import appeng.util.RecipeHolder;

/**
 * The recipes of AE2's own recipe types (1.21 {@code RecipeManager}), loaded by
 * {@link appeng.recipes.loader.RecipeLoader}. The recipe files come from the mod files, so client and server load the
 * same recipes and nothing is synchronized.
 * <p/>
 * {@link #byKey} also finds crafting recipes of the game and the stonecutting/smithing recipes of
 * {@link VanillaRecipes}, since 1.21 code looks all of them up by id.
 */
public final class RecipeManager {

    private static final RecipeManager INSTANCE = new RecipeManager();

    private final Map<RecipeType<?>, List<RecipeHolder<?>>> byType = new IdentityHashMap<>();
    private final Map<ResourceLocation, RecipeHolder<?>> byId = new HashMap<>();

    private RecipeManager() {
    }

    /**
     * Same as 1.21 {@code level.getRecipeManager()}: all levels share the recipes.
     */
    public static RecipeManager of(@Nullable World level) {
        return INSTANCE;
    }

    public static RecipeManager get() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> List<RecipeHolder<T>> byType(RecipeType<T> type) {
        var list = byType.get(type);
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<RecipeHolder<T>>) (List<?>) list;
    }

    /**
     * @return The AE2 recipe, crafting recipe or stonecutting/smithing recipe with the given id.
     */
    public synchronized Optional<RecipeHolder<?>> byKey(ResourceLocation id) {
        var holder = byId.get(id);
        if (holder != null) {
            return Optional.of(holder);
        }
        RecipeHolder<?> vanilla = VanillaRecipes.getStonecutting(id);
        if (vanilla == null) {
            vanilla = VanillaRecipes.getSmithing(id);
        }
        if (vanilla != null) {
            return Optional.of(vanilla);
        }
        var crafting = CraftingManager.getRecipe(id);
        if (crafting != null) {
            return Optional.of(new RecipeHolder<>(id, crafting));
        }
        return Optional.empty();
    }

    /**
     * @return The first recipe of the type (in load order) that matches the input.
     */
    public <I extends RecipeInput, T extends AERecipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type,
            I input, World level) {
        for (var holder : byType(type)) {
            if (holder.value().matches(input, level)) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    public synchronized Collection<RecipeHolder<?>> getRecipes() {
        return Collections.unmodifiableCollection(new ArrayList<>(byId.values()));
    }

    // ------------------------------------------------------------------------------------------ loading

    public synchronized void clear() {
        byType.clear();
        byId.clear();
    }

    public synchronized <T> void add(RecipeType<T> type, ResourceLocation id, T recipe) {
        var holder = new RecipeHolder<>(id, recipe);
        var previous = byId.put(id, holder);
        // The lists are replaced, not changed, so callers can keep iterating a list they got earlier
        var list = new ArrayList<>(byType.getOrDefault(type, Collections.emptyList()));
        if (previous != null) {
            list.remove(previous);
        }
        list.add(holder);
        byType.put(type, Collections.unmodifiableList(list));
    }
}
