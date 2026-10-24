/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;

/**
 * 1.12.2 only: registers AE2's materials in the Forge ore dictionary under the names AE2 used on 1.12.2 (rv6), so
 * recipes of other mods that ask for these names accept them. The other direction is done by the {@code c:} tags of
 * these materials, which also list the names ({@code ore:...} entries): materials of other mods registered under them
 * work in AE2's recipes.
 */
public final class OreDictionaryCompat {

    private OreDictionaryCompat() {
    }

    /**
     * Called once during init, after the items and blocks are registered.
     */
    public static void register() {
        register(AEItems.CERTUS_QUARTZ_CRYSTAL.stack(), "crystalCertusQuartz", "gemCertusQuartz");
        register(AEItems.CERTUS_QUARTZ_DUST.stack(), "dustCertusQuartz");
        register(AEItems.FLUIX_CRYSTAL.stack(), "crystalFluix");
        register(AEItems.FLUIX_DUST.stack(), "dustFluix");
        register(AEItems.FLUIX_PEARL.stack(), "pearlFluix");
        register(AEItems.SILICON.stack(), "itemSilicon");
        register(AEItems.ENDER_DUST.stack(), "dustEnder", "dustEnderPearl");
        register(AEItems.COPPER_INGOT.stack(), "ingotCopper");

        register(AEBlocks.QUARTZ_BLOCK.stack(), "blockCertusQuartz");
        register(AEBlocks.FLUIX_BLOCK.stack(), "blockFluix");
        register(AEBlocks.COPPER_ORE.stack(), "oreCopper");

        // any damage, the knives wear out
        register(anyDamage(AEItems.CERTUS_QUARTZ_WRENCH.asItem()), "itemQuartzWrench");
        register(anyDamage(AEItems.NETHER_QUARTZ_WRENCH.asItem()), "itemQuartzWrench");
        register(anyDamage(AEItems.CERTUS_QUARTZ_KNIFE.asItem()), "itemQuartzKnife");
        register(anyDamage(AEItems.NETHER_QUARTZ_KNIFE.asItem()), "itemQuartzKnife");

        register(AEParts.MONITOR.stack(), "itemIlluminatedPanel");
        register(AEParts.SEMI_DARK_MONITOR.stack(), "itemIlluminatedPanel");
        register(AEParts.DARK_MONITOR.stack(), "itemIlluminatedPanel");
    }

    private static ItemStack anyDamage(Item item) {
        return new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE);
    }

    private static void register(ItemStack stack, String... names) {
        for (String name : names) {
            OreDictionary.registerOre(name, stack);
        }
    }
}
