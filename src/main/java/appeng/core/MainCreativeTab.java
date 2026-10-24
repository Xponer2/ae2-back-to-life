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

package appeng.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.ItemDefinition;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;

/**
 * AE2's main creative tab. Items are shown in definition order, like on modern Minecraft.
 */
public final class MainCreativeTab extends CreativeTabs {
    public static final MainCreativeTab INSTANCE = new MainCreativeTab();

    private static final List<ItemDefinition<?>> itemDefs = new ArrayList<>();
    private static final Set<ItemDefinition<?>> itemDefSet = Collections.newSetFromMap(new IdentityHashMap<>());

    private MainCreativeTab() {
        super("ae2.main");
    }

    public static void add(ItemDefinition<?> itemDef) {
        itemDefs.add(itemDef);
        itemDefSet.add(itemDef);
    }

    public static boolean contains(ItemDefinition<?> itemDef) {
        return itemDefSet.contains(itemDef);
    }

    @Override
    public String getTranslationKey() {
        return GuiText.CreativeTab.getTranslationKey();
    }


    @Override
    public ItemStack createIcon() {
        return AEBlocks.CONTROLLER.stack(1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        var output = new CreativeTabOutput(items);
        var params = CreativeTabOutput.DisplayParameters.INSTANCE;
        for (var itemDef : itemDefs) {
            var item = itemDef.asItem();
            // For block items, the block controls the creative tab
            if (item instanceof AEBaseBlockItem baseItem) {
                baseItem.addToMainCreativeTab(params, output);
            } else if (item instanceof AEBaseItem baseItem) {
                baseItem.addToMainCreativeTab(params, output);
            } else {
                output.accept(itemDef);
            }
        }
    }
}
