/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Splitter;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import appeng.api.config.CondenserOutput;
import appeng.api.implementations.items.IStorageComponent;
import appeng.blockentity.misc.CondenserBlockEntity;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;

class CondenserOutputWrapper implements IRecipeWrapper {

    private final CondenserOutput type;
    private final ItemStack output;
    private final List<ItemStack> viableStorageComponents;
    private final IDrawable typeIcon;

    CondenserOutputWrapper(IGuiHelper guiHelper, CondenserOutput type) {
        this.type = type;
        this.output = switch (type) {
            case MATTER_BALLS -> AEItems.MATTER_BALL.stack();
            case SINGULARITY -> AEItems.SINGULARITY.stack();
            default -> ItemStack.EMPTY;
        };
        this.viableStorageComponents = getViableStorageComponents(type);
        var states = AppEng.makeId("textures/guis/states.png");
        this.typeIcon = type == CondenserOutput.SINGULARITY
                ? guiHelper.createDrawable(states, 32, 112, 14, 14)
                : guiHelper.createDrawable(states, 16, 112, 14, 14);
    }

    CondenserOutput getType() {
        return type;
    }

    List<ItemStack> getViableStorageComponents() {
        return viableStorageComponents;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        typeIcon.draw(minecraft, CondenserCategory.PADDING + 80, CondenserCategory.PADDING + 28);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        int x = CondenserCategory.PADDING + 80;
        int y = CondenserCategory.PADDING + 28;
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            String key = switch (type) {
                case MATTER_BALLS -> "gui.tooltips.ae2.MatterBalls";
                case SINGULARITY -> "gui.tooltips.ae2.Singularity";
                default -> null;
            };
            if (key != null) {
                return Splitter.on("\n").splitToList(
                        new TextComponentTranslation(key, type.requiredPower).getFormattedText());
            }
        }
        return Collections.emptyList();
    }

    private static List<ItemStack> getViableStorageComponents(CondenserOutput condenserOutput) {
        List<ItemStack> viableComponents = new ArrayList<>();
        addViableComponent(condenserOutput, viableComponents, AEItems.CELL_COMPONENT_1K.stack());
        addViableComponent(condenserOutput, viableComponents, AEItems.CELL_COMPONENT_4K.stack());
        addViableComponent(condenserOutput, viableComponents, AEItems.CELL_COMPONENT_16K.stack());
        addViableComponent(condenserOutput, viableComponents, AEItems.CELL_COMPONENT_64K.stack());
        addViableComponent(condenserOutput, viableComponents, AEItems.CELL_COMPONENT_256K.stack());
        return viableComponents;
    }

    private static void addViableComponent(CondenserOutput condenserOutput, List<ItemStack> viableComponents,
            ItemStack itemStack) {
        IStorageComponent comp = (IStorageComponent) itemStack.getItem();
        int storage = comp.getBytes(itemStack) * CondenserBlockEntity.BYTE_MULTIPLIER;
        if (storage >= condenserOutput.requiredPower) {
            viableComponents.add(itemStack);
        }
    }
}
