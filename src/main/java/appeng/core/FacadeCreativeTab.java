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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.util.Platform;

/**
 * Creative tab with a facade for every block that can be used as one.
 */
public final class FacadeCreativeTab extends CreativeTabs {
    private static FacadeCreativeTab group;
    private static List<ItemStack> cachedFacades;

    private FacadeCreativeTab() {
        super("ae2.facades");
    }

    public static void init() {
        if (group == null) {
            group = new FacadeCreativeTab();
        }
    }

    public static Collection<ItemStack> getDisplayItems() {
        return group == null ? Collections.emptyList() : Collections.unmodifiableList(getFacades());
    }

    private static List<ItemStack> getFacades() {
        if (cachedFacades == null) {
            var result = new ArrayList<ItemStack>();
            var itemFacade = AEItems.FACADE.get();
            for (Item item : Item.REGISTRY) {
                if (item == itemFacade) {
                    continue;
                }
                var subItems = NonNullList.<ItemStack>create();
                try {
                    var tab = item.getCreativeTab();
                    if (tab == null) {
                        continue;
                    }
                    item.getSubItems(tab, subItems);
                } catch (Throwable t) {
                    // just absorb..
                    continue;
                }
                for (var displayItem : subItems) {
                    var facade = itemFacade.createFacadeForItem(displayItem, false);
                    if (!facade.isEmpty() && result.stream().noneMatch(f -> Platform.isSameItemSameComponents(f,
                            facade))) {
                        result.add(facade);
                    }
                }
            }
            cachedFacades = result;
        }
        return cachedFacades;
    }

    @Override
    public String getTranslationKey() {
        return GuiText.CreativeTabFacades.getTranslationKey();
    }


    @Override
    public ItemStack createIcon() {
        var facades = getFacades();
        return facades.isEmpty() ? new ItemStack(Items.CAKE) : facades.get(0);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        for (var facade : getFacades()) {
            items.add(facade.copy());
        }
    }
}
