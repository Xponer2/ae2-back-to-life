/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.igtooltip;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import appeng.block.AEBaseBlockItem;
import appeng.items.AEBaseItem;
import appeng.util.TextUtil;

/**
 * 1.12.2 only: names as text components that are translated where they are displayed. The One Probe builds its
 * tooltip on the server, where {@link ItemStack#getDisplayName()} would always be English.
 */
public final class ItemNames {
    private ItemNames() {
    }

    public static ITextComponent of(ItemStack stack) {
        if (stack.hasDisplayName()) {
            return new TextComponentString(stack.getDisplayName());
        }
        var item = stack.getItem();
        if (item instanceof AEBaseItem aeItem) {
            return aeItem.getName(stack);
        }
        if (item instanceof AEBaseBlockItem aeBlockItem) {
            return aeBlockItem.getName(stack);
        }
        return new TextComponentTranslation(stack.getTranslationKey() + ".name");
    }

    /**
     * Like {@link Enchantment#getTranslatedName(int)}.
     */
    public static ITextComponent enchantment(Enchantment enchantment, int level) {
        ITextComponent name = new TextComponentTranslation(enchantment.getName());
        if (level != 1 || enchantment.getMaxLevel() != 1) {
            name.appendText(" ").appendSibling(new TextComponentTranslation("enchantment.level." + level));
        }
        return TextUtil.withStyle(name, enchantment.isCurse() ? TextFormatting.RED : TextFormatting.GRAY);
    }
}
