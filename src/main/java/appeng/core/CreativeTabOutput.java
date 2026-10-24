/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import appeng.api.util.ItemLike;

/**
 * Backport of Minecraft's CreativeModeTab.Output: collects the stacks shown in a creative tab.
 */
public final class CreativeTabOutput {
    private final NonNullList<ItemStack> items;

    public CreativeTabOutput(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public void accept(ItemStack stack) {
        if (!stack.isEmpty()) {
            items.add(stack);
        }
    }

    public void accept(ItemLike item) {
        accept(new ItemStack(item.asItem()));
    }

    public void accept(Item item) {
        accept(new ItemStack(item));
    }

    public void accept(net.minecraft.block.Block block) {
        accept(new ItemStack(block));
    }

    /**
     * Stand-in for CreativeModeTab.ItemDisplayParameters (enabled features, operator tab permission, registries).
     */
    public static final class DisplayParameters {
        public static final DisplayParameters INSTANCE = new DisplayParameters();

        public boolean hasPermissions() {
            return true;
        }
    }
}
