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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import appeng.recipes.ingredient.IngredientCodecs;
import appeng.util.ResourceLocations;

/**
 * The load conditions of recipe files ({@code neoforge:conditions}): all of them have to be true.
 */
final class Conditions {
    private Conditions() {
    }

    static boolean test(JsonArray conditions) {
        for (var condition : conditions) {
            if (!test(condition)) {
                return false;
            }
        }
        return true;
    }

    static boolean test(JsonElement json) {
        var object = json.getAsJsonObject();
        var type = object.get("type").getAsString();
        return switch (type) {
            case "neoforge:true", "forge:true" -> true;
            case "neoforge:false", "forge:false" -> false;
            case "neoforge:not", "forge:not" -> !test(object.get("value"));
            case "neoforge:and", "forge:and" -> test(object.getAsJsonArray("values"));
            case "neoforge:or", "forge:or" -> {
                for (var value : object.getAsJsonArray("values")) {
                    if (test(value)) {
                        yield true;
                    }
                }
                yield false;
            }
            case "neoforge:mod_loaded", "forge:mod_loaded" -> Loader.isModLoaded(object.get("modid").getAsString());
            case "neoforge:item_exists", "forge:item_exists" -> IngredientCodecs
                    .tryItemStack(ResourceLocations.parse(object.get("item").getAsString()), 1, false) != null;
            case "neoforge:tag_empty", "forge:tag_empty" -> RecipeLoader
                    .isTagEmpty(ResourceLocations.parse(object.get("tag").getAsString()));
            default -> throw new JsonParseException("Unknown condition " + type);
        };
    }

    /**
     * Whether conditions test a tag: other mods add ore names to tags until post init, so the result can change.
     */
    static boolean testsTags(JsonElement json) {
        if (json.isJsonArray()) {
            for (var element : json.getAsJsonArray()) {
                if (testsTags(element)) {
                    return true;
                }
            }
            return false;
        }
        if (!json.isJsonObject()) {
            return false;
        }
        var object = json.getAsJsonObject();
        return object.has("tag")
                || object.has("value") && testsTags(object.get("value"))
                || object.has("values") && testsTags(object.get("values"));
    }
}
