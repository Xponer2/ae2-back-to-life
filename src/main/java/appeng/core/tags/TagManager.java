/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.tags;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import appeng.core.AELog;

/**
 * Minimal implementation of Minecraft's data driven tags for 1.12.2.
 * <p/>
 * Tag files are read from {@code data/<namespace>/tags/<item|block|fluid>/<path>.json} inside of every loaded mod and
 * from {@code config/ae2/tags/<namespace>/<item|block|fluid>/<path>.json}. The format is identical to modern Minecraft.
 * Additional value syntax for 1.12.2:
 * <ul>
 * <li>{@code "minecraft:dye@4"} item or block with a specific metadata value</li>
 * <li>{@code "ore:ingotIron"} all entries of an ore dictionary name</li>
 * </ul>
 * Modern (flattened) vanilla ids are translated using {@link Flattening}.
 */
public final class TagManager {
    private static final String[] TYPES = { "item", "block", "fluid", "data_component_type" };

    /** type -> tag id -> raw entries */
    private static final Map<String, Map<ResourceLocation, List<String>>> RAW = new HashMap<>();
    private static final Map<String, Map<ResourceLocation, Resolved>> RESOLVED = new HashMap<>();
    private static boolean loaded;

    private TagManager() {
    }

    private static final class Resolved {
        final Set<Object> wildcard = new HashSet<>(); // Item, Block or Fluid
        final Map<Object, Set<Integer>> withMeta = new HashMap<>();

        boolean contains(Object entry, int meta) {
            if (wildcard.contains(entry)) {
                return true;
            }
            var metas = withMeta.get(entry);
            if (metas == null) {
                return false;
            }
            return meta < 0 || metas.contains(meta) || meta == OreDictionary.WILDCARD_VALUE;
        }
    }

    public static synchronized void reload() {
        RAW.clear();
        RESOLVED.clear();
        for (String type : TYPES) {
            RAW.put(type, new HashMap<>());
            RESOLVED.put(type, new HashMap<>());
        }
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            File source = mod.getSource();
            if (source == null || !source.exists()) {
                continue;
            }
            try {
                scanSource(source.toPath());
            } catch (Exception e) {
                AELog.warn("Failed to scan tags of %s: %s", mod.getModId(), e);
            }
        }
        var configDir = new File(Loader.instance().getConfigDir(), "ae2/tags");
        if (configDir.isDirectory()) {
            try {
                scanTagRoot(configDir.toPath(), true);
            } catch (IOException e) {
                AELog.warn("Failed to read tags from %s: %s", configDir, e);
            }
        }
        loaded = true;
    }

    private static void scanSource(Path source) throws IOException {
        if (Files.isDirectory(source)) {
            // dev environment: resources may be in a separate output folder, handled by the classpath scan below
            var data = source.resolve("data");
            if (Files.isDirectory(data)) {
                scanDataDir(data);
            }
            // RFG puts resources next to classes: build/resources/main
            var res = source.getParent() != null ? source.getParent().getParent() : null;
            if (res != null) {
                var resData = res.resolve("resources").resolve("main").resolve("data");
                if (Files.isDirectory(resData)) {
                    scanDataDir(resData);
                }
            }
        } else if (source.toString().endsWith(".jar") || source.toString().endsWith(".zip")) {
            try (FileSystem fs = FileSystems.newFileSystem(source, (ClassLoader) null)) {
                var data = fs.getPath("/data");
                if (Files.isDirectory(data)) {
                    scanDataDir(data);
                }
            }
        }
    }

    private static void scanDataDir(Path data) throws IOException {
        try (Stream<Path> namespaces = Files.list(data)) {
            for (Path ns : (Iterable<Path>) namespaces::iterator) {
                var tags = ns.resolve("tags");
                if (Files.isDirectory(tags)) {
                    scanNamespace(stripSlash(ns.getFileName().toString()), tags);
                }
            }
        }
    }

    private static void scanTagRoot(Path root, boolean nsFirst) throws IOException {
        try (Stream<Path> namespaces = Files.list(root)) {
            for (Path ns : (Iterable<Path>) namespaces::iterator) {
                if (Files.isDirectory(ns)) {
                    scanNamespace(stripSlash(ns.getFileName().toString()), ns);
                }
            }
        }
    }

    private static String stripSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static void scanNamespace(String namespace, Path tagsDir) throws IOException {
        for (String type : TYPES) {
            for (String folder : new String[] { type, type + "s" }) {
                var dir = tagsDir.resolve(folder);
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(dir)) {
                    for (Path file : (Iterable<Path>) files::iterator) {
                        var fn = file.toString();
                        if (!fn.endsWith(".json")) {
                            continue;
                        }
                        var rel = dir.relativize(file).toString().replace('\\', '/');
                        var path = rel.substring(0, rel.length() - ".json".length());
                        try (InputStream in = Files.newInputStream(file)) {
                            readTagFile(type, new ResourceLocation(namespace, path), in);
                        } catch (Exception e) {
                            AELog.warn("Failed to read tag file %s: %s", file, e);
                        }
                    }
                }
            }
        }
    }

    private static void readTagFile(String type, ResourceLocation id, InputStream in) {
        JsonObject json = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();
        var entries = RAW.get(type).computeIfAbsent(id, k -> new ArrayList<>());
        if (json.has("replace") && json.get("replace").getAsBoolean()) {
            entries.clear();
        }
        if (json.has("values")) {
            for (JsonElement value : json.getAsJsonArray("values")) {
                if (value.isJsonObject()) {
                    entries.add(value.getAsJsonObject().get("id").getAsString());
                } else {
                    entries.add(value.getAsString());
                }
            }
        }
    }

    private static Resolved resolve(String type, ResourceLocation id) {
        if (!loaded) {
            reload();
        }
        var cache = RESOLVED.get(type);
        var result = cache.get(id);
        if (result != null) {
            return result;
        }
        result = new Resolved();
        cache.put(id, result);
        resolveInto(type, id, result, new HashSet<>());
        return result;
    }

    private static void resolveInto(String type, ResourceLocation id, Resolved result, Set<ResourceLocation> visited) {
        if (!visited.add(id)) {
            return;
        }
        var raw = RAW.get(type).get(id);
        if (raw == null) {
            return;
        }
        for (String entry : raw) {
            if (entry.startsWith("#")) {
                resolveInto(type, new ResourceLocation(entry.substring(1)), result, visited);
            } else if (entry.startsWith("ore:")) {
                if (!type.equals("fluid")) {
                    for (ItemStack ore : OreDictionary.getOres(entry.substring(4), false)) {
                        addStack(type, result, ore);
                    }
                }
            } else {
                addEntry(type, result, entry);
            }
        }
    }

    private static void addStack(String type, Resolved result, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Object key = stack.getItem();
        if (type.equals("block")) {
            var block = Block.getBlockFromItem(stack.getItem());
            if (block == Blocks.AIR) {
                return;
            }
            key = block;
        }
        if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) {
            result.wildcard.add(key);
        } else {
            result.withMeta.computeIfAbsent(key, k -> new HashSet<>()).add(stack.getMetadata());
        }
    }

    private static void addEntry(String type, Resolved result, String entry) {
        int meta = -1;
        int at = entry.indexOf('@');
        if (at >= 0) {
            meta = Integer.parseInt(entry.substring(at + 1));
            entry = entry.substring(0, at);
        }
        var rl = new ResourceLocation(entry);
        if (meta < 0) {
            var flattened = Flattening.lookup(rl);
            if (flattened != null) {
                rl = flattened.id();
                meta = flattened.meta();
            }
        }
        Object key;
        switch (type) {
            case "item" -> {
                var item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null || item == Items.AIR) {
                    return;
                }
                key = item;
            }
            case "block" -> {
                if (!ForgeRegistries.BLOCKS.containsKey(rl)) {
                    return;
                }
                key = ForgeRegistries.BLOCKS.getValue(rl);
            }
            default -> {
                Fluid fluid = FluidRegistry.getFluid(rl.getPath());
                if (fluid == null) {
                    return;
                }
                key = fluid;
            }
        }
        if (meta < 0) {
            result.wildcard.add(key);
        } else {
            result.withMeta.computeIfAbsent(key, k -> new HashSet<>()).add(meta);
        }
    }

    public static boolean isInItemTag(ResourceLocation tag, Item item, int meta) {
        return resolve("item", tag).contains(item, meta);
    }

    public static boolean isInBlockTag(ResourceLocation tag, Block block, int meta) {
        return resolve("block", tag).contains(block, meta);
    }

    public static boolean isInFluidTag(ResourceLocation tag, Fluid fluid) {
        return resolve("fluid", tag).contains(fluid, -1);
    }

    /**
     * All stacks in an item tag (wildcard metadata is expanded to {@link OreDictionary#WILDCARD_VALUE}).
     */
    public static List<ItemStack> getItems(ResourceLocation tag) {
        var resolved = resolve("item", tag);
        var result = new ArrayList<ItemStack>();
        for (Object o : resolved.wildcard) {
            result.add(new ItemStack((Item) o, 1, OreDictionary.WILDCARD_VALUE));
        }
        resolved.withMeta.forEach((item, metas) -> {
            for (int meta : metas) {
                result.add(new ItemStack((Item) item, 1, meta));
            }
        });
        return result;
    }

    public static Collection<Block> getBlocks(ResourceLocation tag) {
        var resolved = resolve("block", tag);
        var result = new LinkedHashSet<Block>();
        for (Object o : resolved.wildcard) {
            result.add((Block) o);
        }
        for (Object o : resolved.withMeta.keySet()) {
            result.add((Block) o);
        }
        return result;
    }

    public static Collection<ResourceLocation> getTagIds(String type) {
        if (!loaded) {
            reload();
        }
        return Collections.unmodifiableSet(RAW.get(type).keySet());
    }

    /**
     * Tag ids of all item tags containing the given stack.
     */
    public static List<ResourceLocation> getItemTagsOf(ItemStack stack) {
        var result = new ArrayList<ResourceLocation>();
        for (var id : getTagIds("item")) {
            if (isInItemTag(id, stack.getItem(), stack.getMetadata())) {
                result.add(id);
            }
        }
        return result;
    }

    @Nullable
    public static List<String> getRawEntries(String type, ResourceLocation id) {
        if (!loaded) {
            reload();
        }
        return RAW.get(type).get(id);
    }

    /**
     * Entry ids of a tag (nested tags expanded), for registries without items/blocks such as
     * {@code data_component_type}.
     */
    public static Set<ResourceLocation> getIds(String type, ResourceLocation tag) {
        var result = new LinkedHashSet<ResourceLocation>();
        collectIds(type, tag, result, new HashSet<>());
        return result;
    }

    private static void collectIds(String type, ResourceLocation tag, Set<ResourceLocation> out,
            Set<ResourceLocation> visited) {
        if (!visited.add(tag)) {
            return;
        }
        var entries = getRawEntries(type, tag);
        if (entries == null) {
            return;
        }
        for (var entry : entries) {
            if (entry.startsWith("#")) {
                collectIds(type, new ResourceLocation(entry.substring(1)), out, visited);
            } else {
                out.add(new ResourceLocation(entry));
            }
        }
    }

    /**
     * Must be called once all mods registered their ore dictionary entries.
     */
    public static synchronized void invalidate() {
        for (var map : RESOLVED.values()) {
            map.clear();
        }
    }
}
