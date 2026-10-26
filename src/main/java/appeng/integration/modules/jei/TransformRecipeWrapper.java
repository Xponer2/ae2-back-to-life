/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import appeng.core.definitions.AEBlocks;
import appeng.core.localization.ItemModText;
import appeng.recipes.transform.TransformCircumstance;
import appeng.recipes.transform.TransformRecipe;

class TransformRecipeWrapper implements IRecipeWrapper {

    private final TransformRecipe recipe;

    TransformRecipeWrapper(TransformRecipe recipe) {
        this.recipe = recipe;
    }

    TransformRecipe getRecipe() {
        return recipe;
    }

    TransformCircumstance getTransformCircumstance() {
        return recipe.getCircumstance();
    }

    /**
     * The fluids to show for a fluid circumstance (one bucket each).
     */
    List<FluidStack> getCatalystFluids() {
        var circumstance = getTransformCircumstance();
        if (!circumstance.isFluid()) {
            return new ArrayList<>();
        }
        return circumstance.getFluidsForRendering().stream()
                .map(fluid -> new FluidStack(fluid, Fluid.BUCKET_VOLUME))
                .collect(Collectors.toList());
    }

    /**
     * The explosives to show for an explosion circumstance.
     */
    List<ItemStack> getCatalystItems() {
        var stacks = new ArrayList<ItemStack>();
        if (getTransformCircumstance().isExplosion()) {
            stacks.add(AEBlocks.TINY_TNT.stack());
            stacks.add(new ItemStack(Blocks.TNT));
        }
        return stacks;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        var inputs = new ArrayList<List<ItemStack>>();
        for (var ingredient : recipe.getIngredients()) {
            inputs.add(InscriberRecipeWrapper.stacks(ingredient));
        }
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getResultItem());
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        var circumstance = getTransformCircumstance().isExplosion()
                ? ItemModText.EXPLOSION.text()
                : ItemModText.SUBMERGE_IN.text();
        var text = circumstance.getFormattedText();
        var font = minecraft.fontRenderer;
        font.drawString(text, (recipeWidth - font.getStringWidth(text)) / 2, 11, 0x7E7E7E);
    }
}
