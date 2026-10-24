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
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/**
 * 1.12.2 has no smithing table: this is the 1.21 {@code SmithingTransformRecipe} (template + base + addition, the
 * result keeps the NBT of the base), loaded from the {@code minecraft:smithing_transform} recipe JSON and kept in
 * {@link VanillaRecipes}. AE2 uses it for smithing table patterns.
 */
public final class SmithingRecipe {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public SmithingRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStack result) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    public boolean matches(SmithingRecipeInput input, World level) {
        return template.apply(input.template()) && base.apply(input.base()) && addition.apply(input.addition());
    }

    public ItemStack assemble(SmithingRecipeInput input) {
        var stack = result.copy();
        var baseTag = input.base().getTagCompound();
        if (baseTag != null) {
            var tag = baseTag.copy();
            if (stack.hasTagCompound()) {
                tag.merge(stack.getTagCompound());
            }
            stack.setTagCompound(tag);
        }
        return stack;
    }

    public ItemStack getResultItem() {
        return result.copy();
    }

    public boolean isTemplateIngredient(ItemStack stack) {
        return template.apply(stack);
    }

    public boolean isBaseIngredient(ItemStack stack) {
        return base.apply(stack);
    }

    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.apply(stack);
    }

    public Ingredient getTemplate() {
        return template;
    }

    public Ingredient getBase() {
        return base;
    }

    public Ingredient getAddition() {
        return addition;
    }

    public NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.withSize(3, Ingredient.EMPTY);
        list.set(0, template);
        list.set(1, base);
        list.set(2, addition);
        return list;
    }

    @Override
    public String toString() {
        return "SmithingRecipe[" + template + " + " + base + " + " + addition + " -> " + result + "]";
    }
}
