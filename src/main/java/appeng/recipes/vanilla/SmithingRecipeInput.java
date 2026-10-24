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
 * 1.21 {@code net.minecraft.world.item.crafting.SmithingRecipeInput}: template, base and addition.
 */
public final class SmithingRecipeInput {
    private final ItemStack template;
    private final ItemStack base;
    private final ItemStack addition;

    public SmithingRecipeInput(ItemStack template, ItemStack base, ItemStack addition) {
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    public ItemStack template() {
        return template;
    }

    public ItemStack base() {
        return base;
    }

    public ItemStack addition() {
        return addition;
    }

    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> template;
            case 1 -> base;
            case 2 -> addition;
            default -> throw new IllegalArgumentException("No item for index " + index);
        };
    }

    public int size() {
        return 3;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SmithingRecipeInput other
                && ItemStack.areItemStacksEqual(template, other.template)
                && ItemStack.areItemStacksEqual(base, other.base)
                && ItemStack.areItemStacksEqual(addition, other.addition);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(template.getItem(), base.getItem(), addition.getItem());
    }

    @Override
    public String toString() {
        return "SmithingRecipeInput[template=" + template + ", base=" + base + ", addition=" + addition + "]";
    }
}
