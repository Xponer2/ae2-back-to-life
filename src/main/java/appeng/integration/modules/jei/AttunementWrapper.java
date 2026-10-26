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

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

/**
 * Items that attune a P2P tunnel to a certain type when right-clicked on it.
 */
class AttunementWrapper implements IRecipeWrapper {

    private final List<ItemStack> inputs;
    private final ItemStack tunnel;
    private final List<ITextComponent> description;

    AttunementWrapper(List<ItemStack> inputs, ItemStack tunnel, List<ITextComponent> description) {
        this.inputs = inputs;
        this.tunnel = tunnel;
        this.description = description;
    }

    List<ITextComponent> getDescription() {
        return description;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(inputs));
        ingredients.setOutput(VanillaTypes.ITEM, tunnel);
    }
}
