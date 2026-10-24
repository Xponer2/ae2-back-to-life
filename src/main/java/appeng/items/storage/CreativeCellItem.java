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

package appeng.items.storage;

import appeng.util.ItemProperties;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import appeng.api.util.TooltipComponent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.client.AEKeyRendering;
import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.util.ItemLike;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.me.cells.CreativeCellHandler;
import appeng.util.ConfigInventory;

public class CreativeCellItem extends AEBaseItem implements ICellWorkbenchItem {
    public CreativeCellItem(ItemProperties props) {
        super(props);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack is) {
        return CellConfig.create(is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> lines,
            ITooltipFlag advancedTooltips) {
        var inventory = StorageCells.getCellInventory(stack, null);

        if (inventory != null) {
            var cc = getConfigInventory(stack);
            if (!cc.isEmpty()) {
                if (GuiScreen.hasShiftDown()) {
                    for (var key : cc.keySet()) {
                        lines.add(Tooltips.of(AEKeyRendering.getDisplayName(key)));
                    }
                } else {
                    lines.add(Tooltips.of(GuiText.PressShiftForFullList));
                }
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return CreativeCellHandler.INSTANCE.getTooltipImage(stack);
    }

    public static ItemStack ofItems(ItemLike... items) {
        var cell = AEItems.CREATIVE_CELL.stack();
        var configInv = AEItems.CREATIVE_CELL.get().getConfigInventory(cell);
        for (int i = 0; i < items.length; i++) {
            configInv.setStack(i, GenericStack.fromItemStack(new ItemStack(items[i])));
        }
        return cell;
    }

    public static ItemStack ofFluids(Fluid... fluids) {
        var cell = AEItems.CREATIVE_CELL.stack();
        var configInv = AEItems.CREATIVE_CELL.get().getConfigInventory(cell);
        for (int i = 0; i < fluids.length; i++) {
            configInv.setStack(i, new GenericStack(AEFluidKey.of(fluids[i]), 1));
        }
        return cell;
    }
}
