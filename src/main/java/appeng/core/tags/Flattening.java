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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;

/**
 * Translates modern ("flattened", 1.13+) vanilla ids to their 1.12.2 id + metadata, so data files from AE2 for 1.21 can
 * be used unchanged.
 */
public final class Flattening {
    public static final class Entry {
        private final ResourceLocation id;
        private final int meta;

        Entry(ResourceLocation id, int meta) {
            this.id = id;
            this.meta = meta;
        }

        public ResourceLocation id() {
            return id;
        }

        /**
         * -1 = any metadata.
         */
        public int meta() {
            return meta;
        }
    }

    private static final Map<ResourceLocation, Entry> MAP = new HashMap<>();

    private Flattening() {
    }

    private static void put(String modern, String old, int meta) {
        MAP.put(new ResourceLocation(modern), new Entry(new ResourceLocation(old), meta));
    }

    static {
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String n = color.getName();
            if (n.equals("silver")) {
                n = "light_gray";
            }
            put(n + "_wool", "wool", color.getMetadata());
            put(n + "_carpet", "carpet", color.getMetadata());
            put(n + "_stained_glass", "stained_glass", color.getMetadata());
            put(n + "_stained_glass_pane", "stained_glass_pane", color.getMetadata());
            put(n + "_terracotta", "stained_hardened_clay", color.getMetadata());
            put(n + "_concrete", "concrete", color.getMetadata());
            put(n + "_concrete_powder", "concrete_powder", color.getMetadata());
            put(n + "_dye", "dye", color.getDyeDamage());
        }
        // Dyes which used to be special items
        put("ink_sac", "dye", 0);
        put("cocoa_beans", "dye", 3);
        put("lapis_lazuli", "dye", 4);
        put("bone_meal", "dye", 15);

        put("oak_planks", "planks", 0);
        put("spruce_planks", "planks", 1);
        put("birch_planks", "planks", 2);
        put("jungle_planks", "planks", 3);
        put("acacia_planks", "planks", 4);
        put("dark_oak_planks", "planks", 5);
        put("oak_log", "log", 0);
        put("spruce_log", "log", 1);
        put("birch_log", "log", 2);
        put("jungle_log", "log", 3);
        put("acacia_log", "log2", 0);
        put("dark_oak_log", "log2", 1);
        put("oak_sapling", "sapling", 0);
        put("spruce_sapling", "sapling", 1);
        put("birch_sapling", "sapling", 2);
        put("jungle_sapling", "sapling", 3);
        put("acacia_sapling", "sapling", 4);
        put("dark_oak_sapling", "sapling", 5);
        put("oak_leaves", "leaves", 0);
        put("spruce_leaves", "leaves", 1);
        put("birch_leaves", "leaves", 2);
        put("jungle_leaves", "leaves", 3);
        put("acacia_leaves", "leaves2", 0);
        put("dark_oak_leaves", "leaves2", 1);
        put("oak_slab", "wooden_slab", 0);
        put("oak_fence", "fence", 0);
        put("oak_door", "wooden_door", 0);
        put("oak_trapdoor", "trapdoor", 0);
        put("oak_pressure_plate", "wooden_pressure_plate", 0);
        put("oak_button", "wooden_button", 0);

        put("stone", "stone", 0);
        put("granite", "stone", 1);
        put("polished_granite", "stone", 2);
        put("diorite", "stone", 3);
        put("polished_diorite", "stone", 4);
        put("andesite", "stone", 5);
        put("polished_andesite", "stone", 6);
        put("stone_bricks", "stonebrick", 0);
        put("mossy_stone_bricks", "stonebrick", 1);
        put("cracked_stone_bricks", "stonebrick", 2);
        put("chiseled_stone_bricks", "stonebrick", 3);
        put("smooth_stone", "stone", 0);
        put("smooth_stone_slab", "stone_slab", 0);
        put("stone_slab", "stone_slab", 0);
        put("grass_block", "grass", 0);
        put("coarse_dirt", "dirt", 1);
        put("podzol", "dirt", 2);
        put("red_sand", "sand", 1);
        put("sand", "sand", 0);
        put("sandstone", "sandstone", 0);
        put("chiseled_sandstone", "sandstone", 1);
        put("cut_sandstone", "sandstone", 2);
        put("nether_bricks", "nether_brick", 0);
        put("nether_brick", "netherbrick", 0);
        put("quartz_block", "quartz_block", 0);
        put("chiseled_quartz_block", "quartz_block", 1);
        put("quartz_pillar", "quartz_block", 2);
        put("prismarine_bricks", "prismarine", 1);
        put("dark_prismarine", "prismarine", 2);
        put("sea_lantern", "sea_lantern", 0);
        put("terracotta", "hardened_clay", 0);
        put("end_stone", "end_stone", 0);
        put("end_stone_bricks", "end_bricks", 0);
        put("purpur_block", "purpur_block", 0);
        put("snow_block", "snow", 0);
        put("snow", "snow_layer", 0);
        put("sugar_cane", "reeds", 0);
        put("cobweb", "web", 0);
        put("lily_pad", "waterlily", 0);
        put("dead_bush", "deadbush", 0);
        put("short_grass", "tallgrass", 1);
        put("fern", "tallgrass", 2);
        put("dandelion", "yellow_flower", 0);
        put("poppy", "red_flower", 0);
        put("melon", "melon_block", 0);
        put("melon_slice", "melon", 0);
        put("carved_pumpkin", "pumpkin", 0);
        put("jack_o_lantern", "lit_pumpkin", 0);
        put("spawner", "mob_spawner", 0);
        put("nether_quartz_ore", "quartz_ore", 0);
        put("redstone_torch", "redstone_torch", 0);
        put("light_weighted_pressure_plate", "light_weighted_pressure_plate", 0);
        put("crafting_table", "crafting_table", -1);
        put("comparator", "comparator", -1);
        put("repeater", "repeater", -1);
        put("glass", "glass", -1);
        put("glass_pane", "glass_pane", -1);
        put("bricks", "brick_block", 0);
        put("brick", "brick", 0);
        put("magma_block", "magma", 0);
        put("nether_wart_block", "nether_wart_block", 0);
        put("red_nether_bricks", "red_nether_brick", 0);
        put("slime_ball", "slime_ball", -1);
        put("gunpowder", "gunpowder", -1);
        put("firework_rocket", "fireworks", 0);
        put("fire_charge", "fire_charge", 0);
        put("enchanted_golden_apple", "golden_apple", 1);
        put("cooked_porkchop", "cooked_porkchop", 0);
        put("porkchop", "porkchop", 0);
        put("cod", "fish", 0);
        put("salmon", "fish", 1);
        put("tropical_fish", "fish", 2);
        put("pufferfish", "fish", 3);
        put("cooked_cod", "cooked_fish", 0);
        put("cooked_salmon", "cooked_fish", 1);
        put("player_head", "skull", 3);
        put("skeleton_skull", "skull", 0);
        put("wither_skeleton_skull", "skull", 1);
        put("zombie_head", "skull", 2);
        put("creeper_head", "skull", 4);
        put("dragon_head", "skull", 5);
        put("experience_bottle", "experience_bottle", 0);
        put("writable_book", "writable_book", 0);
        put("map", "map", 0);
        put("filled_map", "filled_map", -1);
        put("wheat_seeds", "wheat_seeds", 0);
        put("nether_wart", "nether_wart", 0);
        put("powered_rail", "golden_rail", 0);
        put("rail", "rail", 0);
        put("note_block", "noteblock", 0);
        put("tnt", "tnt", 0);
        put("ender_eye", "ender_eye", 0);
        put("clock", "clock", 0);
        put("compass", "compass", 0);
        put("iron_bars", "iron_bars", 0);
        put("cauldron", "cauldron", 0);
        put("brewing_stand", "brewing_stand", 0);
        put("flower_pot", "flower_pot", 0);
        put("enchanting_table", "enchanting_table", 0);
        put("end_portal_frame", "end_portal_frame", 0);
    }

    @Nullable
    public static Entry lookup(ResourceLocation modern) {
        return MAP.get(modern);
    }
}
