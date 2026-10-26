/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import appeng.core.AELog;
import appeng.core.tags.TagManager;
import appeng.recipes.AERecipe;
import appeng.recipes.AERecipeSerializers;
import appeng.recipes.RecipeManager;
import appeng.recipes.game.SmithingCraftingRecipe;
import appeng.recipes.ingredient.IngredientCodecs;
import appeng.recipes.transform.TransformLogic;
import appeng.recipes.vanilla.SmithingRecipe;
import appeng.recipes.vanilla.StonecutterRecipe;
import appeng.recipes.vanilla.VanillaRecipes;
import appeng.util.DataResults;
import appeng.util.ResourceLocations;

/**
 * Loads the recipe files of newer versions ({@code data/<namespace>/recipe/<path>.json} in every mod) with the id
 * {@code <namespace>:<path>}:
 * <ul>
 * <li>shaped/shapeless crafting: {@link ShapedOreRecipe}/{@link ShapelessOreRecipe} in the recipe registry,</li>
 * <li>smelting/blasting: {@link FurnaceRecipes} (blasting only where no smelting recipe exists),</li>
 * <li>stonecutting and smithing: {@link VanillaRecipes}, smithing recipes also as crafting recipes,</li>
 * <li>AE2 recipe types: crafting table recipes in the recipe registry, the others in {@link RecipeManager}.</li>
 * </ul>
 * {@code neoforge:conditions} are evaluated when loading. Recipes that can't be loaded are logged and skipped.
 */
public final class RecipeLoader {

    private static final String CONDITIONS = "neoforge:conditions";

    /**
     * Recipes left out because a tag they test was empty: loaded again by {@link #loadDeferred()}.
     */
    private static final Map<ResourceLocation, JsonObject> DEFERRED = new TreeMap<>(
            (a, b) -> a.toString().compareTo(b.toString()));

    @Nullable
    private final IForgeRegistry<IRecipe> registry;
    private final Map<String, Integer> loaded = new TreeMap<>();
    private int skipped;

    private RecipeLoader(@Nullable IForgeRegistry<IRecipe> registry) {
        this.registry = registry;
    }

    public static void loadAll(IForgeRegistry<IRecipe> registry) {
        RecipeManager.get().clear();
        VanillaRecipes.clear();
        DEFERRED.clear();

        var loader = new RecipeLoader(registry);
        // Sorted by id, so recipes load in the same order everywhere
        var files = new TreeMap<ResourceLocation, JsonObject>((a, b) -> a.toString().compareTo(b.toString()));
        // Several containers share one source (minecraft, forge, ...)
        var scanned = new HashSet<File>();
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            File source = mod.getSource();
            if (source != null && source.exists() && scanned.add(source)) {
                try {
                    scanSource(source.toPath(), files);
                } catch (Exception e) {
                    AELog.warn("Failed to scan recipes of %s: %s", mod.getModId(), e);
                }
            }
        }
        for (var entry : files.entrySet()) {
            loader.load(entry.getKey(), entry.getValue());
        }

        TransformLogic.clearCache();
        AELog.info("Loaded recipes %s, skipped %d", loader.loaded, loader.skipped);
    }

    /**
     * Recipes are registered before the init phase, when many mods have not registered their ore names yet. Recipes
     * whose conditions test such tags (matter cannon ammo of other mods' metals) are tried again after post init.
     * Crafting recipes can't be added this late and stay left out.
     */
    public static void loadDeferred() {
        if (DEFERRED.isEmpty()) {
            return;
        }
        var loader = new RecipeLoader(null);
        for (var entry : DEFERRED.entrySet()) {
            loader.load(entry.getKey(), entry.getValue());
        }
        DEFERRED.clear();
        TransformLogic.clearCache();
        if (!loader.loaded.isEmpty() || loader.skipped > 0) {
            AELog.info("Loaded recipes after post init %s, skipped %d", loader.loaded, loader.skipped);
        }
    }

    // ------------------------------------------------------------------------------------------ files

    private static void scanSource(Path source, Map<ResourceLocation, JsonObject> files) throws IOException {
        if (Files.isDirectory(source)) {
            var data = source.resolve("data");
            if (Files.isDirectory(data)) {
                scanDataDir(data, files);
            }
            // In the development environment the resources are next to the classes: build/classes/java/main and
            // build/resources/main
            var resData = devResourcesData(source);
            if (resData != null) {
                scanDataDir(resData, files);
            }
        } else if (source.toString().endsWith(".jar") || source.toString().endsWith(".zip")) {
            try (FileSystem fs = FileSystems.newFileSystem(source, (ClassLoader) null)) {
                var data = fs.getPath("/data");
                if (Files.isDirectory(data)) {
                    scanDataDir(data, files);
                }
            }
        }
    }

    /**
     * The data folder of the resource output that belongs to a class output folder of the development environment
     * ({@code build/classes/java/main} -> {@code build/resources/main/data}), or null.
     */
    @Nullable
    public static Path devResourcesData(Path classes) {
        var dir = classes.getParent();
        for (int i = 0; i < 3 && dir != null; i++, dir = dir.getParent()) {
            var data = dir.resolve("resources").resolve("main").resolve("data");
            if (Files.isDirectory(data)) {
                return data;
            }
        }
        return null;
    }

    private static void scanDataDir(Path data, Map<ResourceLocation, JsonObject> files) throws IOException {
        try (Stream<Path> namespaces = Files.list(data)) {
            for (Path ns : (Iterable<Path>) namespaces::iterator) {
                var namespace = ns.getFileName().toString().replace("/", "");
                var recipes = ns.resolve("recipe");
                if (!Files.isDirectory(recipes)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(recipes)) {
                    for (Path file : (Iterable<Path>) walk::iterator) {
                        if (!file.toString().endsWith(".json")) {
                            continue;
                        }
                        var rel = recipes.relativize(file).toString().replace('\\', '/');
                        var id = new ResourceLocation(namespace, rel.substring(0, rel.length() - ".json".length()));
                        try (InputStream in = Files.newInputStream(file)) {
                            var json = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                            files.put(id, json.getAsJsonObject());
                        } catch (Exception e) {
                            AELog.warn("Failed to read recipe %s: %s", id, e);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------ recipes

    private void load(ResourceLocation id, JsonObject json) {
        try {
            if (json.has(CONDITIONS) && !Conditions.test(json.getAsJsonArray(CONDITIONS))) {
                if (registry != null && Conditions.testsTags(json.get(CONDITIONS))) {
                    DEFERRED.put(id, json);
                }
                return;
            }
            var type = ResourceLocations.parse(json.get("type").getAsString());
            switch (type.toString()) {
                case "minecraft:crafting_shaped" -> register(id, shaped(json));
                case "minecraft:crafting_shapeless" -> register(id, shapeless(json));
                case "minecraft:smelting" -> smelting(json, true);
                case "minecraft:blasting" -> smelting(json, false);
                case "minecraft:stonecutting" -> VanillaRecipes.addStonecutting(id, new StonecutterRecipe(
                        json.has("group") ? json.get("group").getAsString() : "",
                        IngredientCodecs.fromJson(json.get("ingredient")), result(json.get("result"))));
                case "minecraft:smithing_transform" -> smithing(id, json);
                default -> loadAE2(id, type, json);
            }
            loaded.merge(type.toString(), 1, Integer::sum);
        } catch (Exception e) {
            skipped++;
            AELog.warn("Skipped recipe %s: %s", id, e.getMessage());
        }
    }

    private void register(ResourceLocation id, IRecipe recipe) {
        if (registry == null) {
            throw new JsonParseException("Crafting recipes can't be added after the recipe registration");
        }
        recipe.setRegistryName(id);
        registry.register(recipe);
    }

    private static ItemStack result(JsonElement json) {
        if (json.isJsonPrimitive()) {
            return IngredientCodecs.itemStack(ResourceLocations.parse(json.getAsString()), 1, false);
        }
        var object = json.getAsJsonObject();
        var count = object.has("count") ? object.get("count").getAsInt() : 1;
        var id = object.has("id") ? object.get("id") : object.get("item");
        return IngredientCodecs.itemStack(ResourceLocations.parse(id.getAsString()), count, false);
    }

    private static String group(JsonObject json) {
        return json.has("group") ? json.get("group").getAsString() : "";
    }

    private static IRecipe shaped(JsonObject json) {
        var pattern = json.getAsJsonArray("pattern");
        var keys = new LinkedHashMap<Character, Ingredient>();
        keys.put(' ', Ingredient.EMPTY);
        for (var entry : json.getAsJsonObject("key").entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonParseException("Invalid key '" + entry.getKey() + "'");
            }
            keys.put(entry.getKey().charAt(0), IngredientCodecs.fromJson(entry.getValue()));
        }

        var primer = new CraftingHelper.ShapedPrimer();
        primer.height = pattern.size();
        primer.width = 0;
        for (var line : pattern) {
            primer.width = Math.max(primer.width, line.getAsString().length());
        }
        primer.mirrored = true;
        primer.input = NonNullList.withSize(primer.width * primer.height, Ingredient.EMPTY);
        for (int y = 0; y < primer.height; y++) {
            var line = pattern.get(y).getAsString();
            for (int x = 0; x < line.length(); x++) {
                var ingredient = keys.get(line.charAt(x));
                if (ingredient == null) {
                    throw new JsonParseException("Pattern uses undefined key '" + line.charAt(x) + "'");
                }
                primer.input.set(x + y * primer.width, ingredient);
            }
        }
        return new ShapedOreRecipe(groupId(json), result(json.get("result")), primer);
    }

    private static IRecipe shapeless(JsonObject json) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (var element : json.getAsJsonArray("ingredients")) {
            ingredients.add(IngredientCodecs.fromJson(element));
        }
        if (ingredients.isEmpty()) {
            throw new JsonParseException("No ingredients");
        }
        return new ShapelessOreRecipe(groupId(json), ingredients, result(json.get("result")));
    }

    private static ResourceLocation groupId(JsonObject json) {
        var group = group(json);
        return group.isEmpty() ? null : new ResourceLocation(group);
    }

    /**
     * Smelting goes into the furnace for every item of the ingredient; blasting only where the furnace has nothing yet
     * (there is no blast furnace in this version).
     */
    private static void smelting(JsonObject json, boolean replace) {
        var ingredient = IngredientCodecs.fromJson(json.get("ingredient"));
        var result = result(json.get("result"));
        var experience = json.has("experience") ? json.get("experience").getAsFloat() : 0;
        var furnace = FurnaceRecipes.instance();
        for (var stack : ingredient.getMatchingStacks()) {
            if (replace || furnace.getSmeltingResult(stack).isEmpty()) {
                furnace.addSmeltingRecipe(stack, result, experience);
            }
        }
    }

    private void smithing(ResourceLocation id, JsonObject json) {
        var recipe = new SmithingRecipe(
                IngredientCodecs.fromJson(json.get("template")),
                IngredientCodecs.fromJson(json.get("base")),
                IngredientCodecs.fromJson(json.get("addition")),
                result(json.get("result")));
        VanillaRecipes.addSmithing(id, recipe);
        register(id, new SmithingCraftingRecipe(recipe));
    }

    private void loadAE2(ResourceLocation id, ResourceLocation type, JsonObject json) {
        var serializer = AERecipeSerializers.get(type);
        if (serializer == null) {
            throw new JsonParseException("Unknown recipe type " + type);
        }
        var recipe = DataResults.getOrThrow(serializer.codec().codec().parse(JsonOps.INSTANCE, json));
        if (recipe instanceof IRecipe craftingRecipe) {
            register(id, craftingRecipe);
        } else if (recipe instanceof AERecipe<?> aeRecipe) {
            addAE2(aeRecipe.getType(), id, aeRecipe);
        } else {
            throw new JsonParseException("Recipe type " + type + " made a " + recipe);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addAE2(appeng.recipes.RecipeType<?> type, ResourceLocation id, Object recipe) {
        RecipeManager.get().add((appeng.recipes.RecipeType) type, id, recipe);
    }

    /**
     * Ensures the tags used by conditions are read before recipes are loaded.
     */
    static boolean isTagEmpty(ResourceLocation tag) {
        return TagManager.getItems(tag).isEmpty();
    }
}
