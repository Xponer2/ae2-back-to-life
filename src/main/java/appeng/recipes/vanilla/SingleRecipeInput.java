/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes.vanilla;

import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code net.minecraft.world.item.crafting.SingleRecipeInput}: the input of a stonecutting recipe.
 */
public final class SingleRecipeInput {
    private final ItemStack item;

    public SingleRecipeInput(ItemStack item) {
        this.item = item;
    }

    public ItemStack item() {
        return item;
    }

    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("No item for index " + index);
        }
        return item;
    }

    public int size() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SingleRecipeInput other && ItemStack.areItemStacksEqual(item, other.item);
    }

    @Override
    public int hashCode() {
        return item.isEmpty() ? 0 : item.getItem().hashCode() * 31 + item.getMetadata();
    }

    @Override
    public String toString() {
        return "SingleRecipeInput[item=" + item + "]";
    }
}
