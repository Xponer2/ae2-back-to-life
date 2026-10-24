/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.init.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;
import appeng.core.AppEng;

/**
 * In 1.12.2 the variant keys of a blockstate file must name every property of the block. The AE2 blockstate files only
 * use the properties that change the model (as in 1.21, where unused properties can be left out), so for each AE2 block
 * the properties that its blockstate file doesn't use are ignored with a {@link StateMap}.
 * <p/>
 * Called from {@code ModelRegistryEvent}.
 */
@SideOnly(Side.CLIENT)
public final class InitStateMappers {

    private InitStateMappers() {
    }

    public static void init() {
        for (Block block : ForgeRegistries.BLOCKS) {
            var id = block.getRegistryName();
            if (id == null || !AppEng.MOD_ID.equals(id.getNamespace())) {
                continue;
            }

            var properties = block.getBlockState().getProperties();
            if (properties.isEmpty()) {
                continue;
            }

            var usedProperties = readUsedProperties(id);
            if (usedProperties == null) {
                continue; // No blockstate file
            }

            List<IProperty<?>> ignored = new ArrayList<>();
            for (var property : properties) {
                if (!usedProperties.contains(property.getName())) {
                    ignored.add(property);
                }
            }

            if (!ignored.isEmpty()) {
                ModelLoader.setCustomStateMapper(block,
                        new StateMap.Builder().ignore(ignored.toArray(new IProperty<?>[0])).build());
            }
        }
    }

    /**
     * @return The names of the properties used in the variant keys or multipart conditions of the block's blockstate
     *         file, or null if it doesn't have one.
     */
    private static Set<String> readUsedProperties(ResourceLocation blockId) {
        var file = new ResourceLocation(blockId.getNamespace(), "blockstates/" + blockId.getPath() + ".json");
        try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
                var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            var json = new JsonParser().parse(reader).getAsJsonObject();
            var result = new HashSet<String>();
            if (json.has("variants")) {
                for (var variant : json.getAsJsonObject("variants").entrySet()) {
                    var key = variant.getKey();
                    for (var part : key.split(",")) {
                        int eq = part.indexOf('=');
                        if (eq > 0) {
                            result.add(part.substring(0, eq));
                        }
                    }
                }
            }
            if (json.has("multipart")) {
                for (var part : json.getAsJsonArray("multipart")) {
                    var when = part.getAsJsonObject().get("when");
                    if (when != null) {
                        collectConditionKeys(when, result);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            AELog.debug("No blockstate file for {}: {}", blockId, e.toString());
            return null;
        }
    }

    private static void collectConditionKeys(JsonElement condition, Set<String> result) {
        if (condition.isJsonArray()) {
            for (var element : condition.getAsJsonArray()) {
                collectConditionKeys(element, result);
            }
        } else if (condition.isJsonObject()) {
            JsonObject object = condition.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if ("OR".equals(entry.getKey()) || "AND".equals(entry.getKey())) {
                    collectConditionKeys(entry.getValue(), result);
                } else {
                    result.add(entry.getKey());
                }
            }
        }
    }
}
