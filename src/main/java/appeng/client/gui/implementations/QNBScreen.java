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

package appeng.client.gui.implementations;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import appeng.blockentity.qnb.QuantumBridgeBlockEntity;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.menu.implementations.QNBMenu;

public class QNBScreen extends AEBaseScreen<QNBMenu> {

    public QNBScreen(QNBMenu menu, InventoryPlayer playerInventory, ITextComponent title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    protected List<ITextComponent> getTooltipFromContainerItem(ItemStack stack) {
        var tooltip = super.getTooltipFromContainerItem(stack);

        if (AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(stack)
                && !QuantumBridgeBlockEntity.isValidEntangledSingularity(stack)) {
            tooltip = new ArrayList<>(tooltip);
            tooltip.add(Tooltips.of(GuiText.InvalidSingularity, Tooltips.RED));
        }

        return tooltip;
    }

}
