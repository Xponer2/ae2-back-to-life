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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.core.tags.Flattening;
import appeng.util.ResourceLocations;

/**
 * Ingredients in the JSON format of newer versions: {@code {"item": id}}, {@code {"tag": id}}, a list of ingredients
 * (any of them), {@code neoforge:compound} and {@code neoforge:difference}. Modern vanilla ids are translated with
 * {@link Flattening}.
 */
public final class IngredientCodecs {

    public static final Codec<Ingredient> CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        try {
            return DataResult.success(fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()));
        } catch (RuntimeException e) {
            return DataResult.error(String.valueOf(e.getMessage()));
        }
    }, ingredient -> new Dynamic<>(JsonOps.INSTANCE, toJson(ingredient)));

    public static final Codec<Ingredient> CODEC_NONEMPTY = CODEC.flatXmap(
            ingredient -> ingredient == Ingredient.EMPTY
                    ? DataResult.error("Ingredient must not be empty")
                    : DataResult.success(ingredient),
            DataResult::success);

    /**
     * Sends the matching stacks, the receiving side gets a plain item ingredient.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> STREAM_CODEC = ByteBufCodecs.ITEM_STACK_LIST
            .<RegistryFriendlyByteBuf>cast()
            .map(stacks -> stacks.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(stacks.toArray(new ItemStack[0])),
                    ingredient -> {
                        var result = new ArrayList<ItemStack>();
                        for (var stack : ingredient.getMatchingStacks()) {
                            result.add(stack);
                        }
                        return result;
                    });

    private IngredientCodecs() {
    }

    public static Ingredient fromJson(JsonElement json) {
        if (json.isJsonArray()) {
            List<Ingredient> children = new ArrayList<>();
            for (var element : json.getAsJsonArray()) {
                children.add(fromJson(element));
            }
            if (children.isEmpty()) {
                return Ingredient.EMPTY;
            }
            return CombinedIngredient.anyOf(children);
        }
        if (!json.isJsonObject()) {
            throw new JsonParseException("Expected an ingredient object or list: " + json);
        }

        var object = json.getAsJsonObject();
        if (object.has("type")) {
            var type = object.get("type").getAsString();
            switch (type) {
                case "neoforge:compound", "forge:compound" -> {
                    return fromJson(object.get("children"));
                }
                case "neoforge:difference", "forge:difference" -> {
                    return CombinedIngredient.difference(fromJson(object.get("base")),
                            fromJson(object.get("subtracted")));
                }
                default -> throw new JsonParseException("Unsupported ingredient type " + type);
            }
        }
        if (object.has("tag")) {
            return new TagIngredient(ResourceLocations.parse(object.get("tag").getAsString()));
        }
        if (object.has("item")) {
            var stack = itemStack(ResourceLocations.parse(object.get("item").getAsString()), 1, true);
            return Ingredient.fromStacks(stack);
        }
        throw new JsonParseException("Ingredient needs an item or a tag: " + json);
    }

    public static JsonElement toJson(Ingredient ingredient) {
        if (ingredient instanceof TagIngredient tagIngredient) {
            var object = new JsonObject();
            object.addProperty("tag", tagIngredient.getTag().toString());
            return object;
        }
        if (ingredient instanceof CombinedIngredient combined && combined.getExcluded() != null) {
            var object = new JsonObject();
            object.addProperty("type", "neoforge:difference");
            object.add("base", toJson(combined.getIncluded().get(0)));
            object.add("subtracted", toJson(combined.getExcluded()));
            return object;
        }
        var array = new JsonArray();
        for (var stack : ingredient.getMatchingStacks()) {
            var object = new JsonObject();
            object.addProperty("item", String.valueOf(stack.getItem().getRegistryName()));
            if (stack.getMetadata() != OreDictionary.WILDCARD_VALUE && stack.getMetadata() != 0) {
                object.addProperty("data", stack.getMetadata());
            }
            array.add(object);
        }
        return array.size() == 1 ? array.get(0) : array;
    }

    /**
     * Resolves an item id, translating modern vanilla ids.
     *
     * @param anyMeta For ingredients: items without a translated metadata match every metadata (damaged tools).
     * @throws JsonParseException if there is no such item
     */
    public static ItemStack itemStack(ResourceLocation id, int count, boolean anyMeta) {
        var stack = tryItemStack(id, count, anyMeta);
        if (stack == null) {
            throw new JsonParseException("Unknown item: " + id);
        }
        return stack;
    }

    @Nullable
    public static ItemStack tryItemStack(ResourceLocation id, int count, boolean anyMeta) {
        Item item = null;
        int meta = anyMeta ? OreDictionary.WILDCARD_VALUE : 0;
        // The files use the newer ids: some of them name another item in this version (minecraft:snow is the snow
        // layer, not the snow block), so the translation comes first
        var flat = Flattening.lookup(id);
        if (flat != null && ForgeRegistries.ITEMS.containsKey(flat.id())) {
            item = ForgeRegistries.ITEMS.getValue(flat.id());
            if (flat.meta() >= 0) {
                meta = flat.meta();
            }
        } else if (ForgeRegistries.ITEMS.containsKey(id)) {
            item = ForgeRegistries.ITEMS.getValue(id);
        }
        if (item == null) {
            return null;
        }
        return new ItemStack(item, count, meta);
    }
}
