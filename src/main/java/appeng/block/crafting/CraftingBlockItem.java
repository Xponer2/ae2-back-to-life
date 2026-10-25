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

package appeng.block.crafting;

import appeng.util.ItemProperties;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import appeng.block.AEBaseBlockItem;
import appeng.core.definitions.AEBlocks;
import appeng.recipes.game.CraftingUnitTransformRecipe;
import appeng.util.InteractionUtil;
import appeng.util.Platform;

/**
 * Item that allows uncrafting CPU parts by disassembling them back into the crafting unit and the extra item.
 */
public class CraftingBlockItem extends AEBaseBlockItem {
    public CraftingBlockItem(Block id, ItemProperties props) {
        super(id, props);
    }

    @Override
    public ActionResult<ItemStack> use(World level, EntityPlayer player, EnumHand hand) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            ItemStack stack = player.getHeldItem(hand);

            var removedUpgrade = CraftingUnitTransformRecipe.getRemovedUpgrade(level, getBlock());
            if (removedUpgrade.isEmpty()) {
                return super.use(level, player, hand);
            }

            int itemCount = stack.getCount();
            player.setHeldItem(hand, ItemStack.EMPTY);

            Platform.placeItemBackInInventory(player,
                    Platform.copyWithCount(removedUpgrade, removedUpgrade.getCount() * itemCount));
            // This is hard-coded, as this is always a base block.
            Platform.placeItemBackInInventory(player, AEBlocks.CRAFTING_UNIT.stack(itemCount));

            return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
        }
        return super.use(level, player, hand);
    }
}
