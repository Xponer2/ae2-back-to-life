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

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import appeng.core.localization.ItemModText;
import appeng.items.tools.powered.EntropyManipulatorItem;
import appeng.recipes.entropy.EntropyMode;
import appeng.recipes.entropy.EntropyRecipe;

/**
 * A recipe of the entropy manipulator: the block or fluid it is used on, and what it becomes (layout of the 1.21 REI
 * display). The input is either an item (block) or a fluid; if the block is removed it is shown as consumed.
 */
class EntropyRecipeWrapper implements IRecipeWrapper {

    static final int BODY_TEXT_COLOR = 0x7E7E7E;

    private final EntropyRecipe recipe;
    private final IDrawable modeIcon;
    private final IDrawable consumedIcon;

    // The in-world input: a block (as item) or a fluid
    @Nullable
    private final ItemStack inputItem;
    @Nullable
    private final FluidStack inputFluid;
    private final boolean inputFlowing;
    private final boolean inputConsumed;

    // The in-world output (block or fluid) followed by the drops
    private final List<ItemStack> outputItems = new ArrayList<>();
    @Nullable
    private final FluidStack outputFluid;

    EntropyRecipeWrapper(IGuiHelper guiHelper, EntropyRecipe recipe) {
        this.recipe = recipe;
        this.modeIcon = recipe.getMode() == EntropyMode.HEAT
                ? guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 68, 6, 6)
                : guiHelper.createDrawable(JEIPlugin.TEXTURE, 6, 68, 6, 6);
        this.consumedIcon = guiHelper.createDrawable(JEIPlugin.TEXTURE, 0, 52, 16, 16);

        var input = recipe.getInput();
        if (input.fluid().isPresent()) {
            this.inputItem = null;
            this.inputFluid = new FluidStack(input.fluid().get().fluid(), Fluid.BUCKET_VOLUME);
            this.inputFlowing = input.fluid().get().ref().flowing();
        } else {
            this.inputItem = input.block().map(b -> blockStack(b.block())).orElse(null);
            this.inputFluid = null;
            this.inputFlowing = false;
        }

        var output = recipe.getOutput();
        this.inputConsumed = output.block().isPresent() && output.block().get().block() == Blocks.AIR
                && !output.fluid().isPresent();

        FluidStack fluidOut = null;
        if (!inputConsumed) {
            if (output.fluid().isPresent()) {
                fluidOut = new FluidStack(output.fluid().get().fluid(), Fluid.BUCKET_VOLUME);
            } else if (output.block().isPresent()) {
                var stack = blockStack(output.block().get().block());
                if (stack != null) {
                    outputItems.add(stack);
                }
            }
            // Additional item drops
            outputItems.addAll(recipe.getDrops());
        }
        this.outputFluid = fluidOut;
    }

    @Nullable
    private static ItemStack blockStack(Block block) {
        var item = Item.getItemFromBlock(block);
        return item == net.minecraft.init.Items.AIR ? null : new ItemStack(item);
    }

    EntropyRecipe getRecipe() {
        return recipe;
    }

    @Nullable
    ItemStack getInputItem() {
        return inputItem;
    }

    @Nullable
    FluidStack getInputFluid() {
        return inputFluid;
    }

    boolean isInputFlowing() {
        return inputFlowing;
    }

    boolean isInputConsumed() {
        return inputConsumed;
    }

    List<ItemStack> getOutputItems() {
        return outputItems;
    }

    @Nullable
    FluidStack getOutputFluid() {
        return outputFluid;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        if (inputItem != null) {
            ingredients.setInput(VanillaTypes.ITEM, inputItem);
        }
        if (inputFluid != null) {
            ingredients.setInput(VanillaTypes.FLUID, inputFluid);
        }
        if (!outputItems.isEmpty()) {
            ingredients.setOutputs(VanillaTypes.ITEM, outputItems);
        }
        if (outputFluid != null) {
            ingredients.setOutput(VanillaTypes.FLUID, outputFluid);
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        var font = minecraft.fontRenderer;
        var centerX = recipeWidth / 2;
        var y = EntropyCategory.PADDING;

        var labelText = (recipe.getMode() == EntropyMode.HEAT
                ? ItemModText.ENTROPY_MANIPULATOR_HEAT.text(EntropyManipulatorItem.ENERGY_PER_USE)
                : ItemModText.ENTROPY_MANIPULATOR_COOL.text(EntropyManipulatorItem.ENERGY_PER_USE))
                .getFormattedText();
        var labelX = centerX + 4 - font.getStringWidth(labelText) / 2;
        font.drawString(labelText, labelX, y + 2, BODY_TEXT_COLOR);
        modeIcon.draw(minecraft, labelX - 9, y + 3);

        var interaction = (recipe.getMode() == EntropyMode.HEAT
                ? ItemModText.RIGHT_CLICK.text()
                : ItemModText.SHIFT_RIGHT_CLICK.text()).getFormattedText();
        font.drawString(interaction, centerX - font.getStringWidth(interaction) / 2, y + 38, BODY_TEXT_COLOR);

        if (inputConsumed) {
            // Cross over the consumed block
            consumedIcon.draw(minecraft, centerX + 20, y + 15);
        }
    }
}
