/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.crafting.pattern;

import java.util.List;
import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Item stack comparison and hashing of newer versions ({@code ItemStack.matches}, {@code listMatches},
 * {@code hashItemAndComponents}, {@code hashStackList}): item, metadata, NBT and count.
 */
final class PatternStacks {
    private PatternStacks() {
    }

    static boolean matches(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.areItemStacksEqual(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hash of item, metadata and NBT (not the count).
     */
    static int hash(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int hash = 31 + Item.getIdFromItem(stack.getItem());
        hash = 31 * hash + stack.getMetadata();
        return 31 * hash + Objects.hashCode(stack.getTagCompound());
    }

    static int hash(List<ItemStack> stacks) {
        int hash = 0;
        for (var stack : stacks) {
            hash = 31 * hash + hash(stack);
        }
        return hash;
    }
}
