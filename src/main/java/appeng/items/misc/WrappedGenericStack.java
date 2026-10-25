/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2018, AlgorithmX2, All rights reserved.
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

package appeng.items.misc;

import appeng.util.ItemProperties;

import appeng.core.CreativeTabOutput;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;

/**
 * Wraps a {@link GenericStack} in an {@link ItemStack}. Even stacks that actually represent vanilla {@link Item items}
 * will be wrapped in this item, to allow items with amount 0 to be represented as itemstacks without becoming the empty
 * item.
 */
public class WrappedGenericStack extends AEBaseItem {
    private static final Logger LOG = LogManager.getLogger(WrappedGenericStack.class);

    public static ItemStack wrap(GenericStack stack) {
        Objects.requireNonNull(stack, "stack");
        var item = AEItems.WRAPPED_GENERIC_STACK.asItem();
        var result = new ItemStack(item);
        AEComponents.WRAPPED_STACK.set(result, stack);
        return result;
    }

    public static ItemStack wrap(AEKey what, long amount) {
        Objects.requireNonNull(what, "what");

        return wrap(new GenericStack(what, amount));
    }

    public WrappedGenericStack(ItemProperties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    public AEKey unwrapWhat(ItemStack stack) {
        if (stack.getItem() != this) {
            return null;
        }

        var wrapped = AEComponents.WRAPPED_STACK.get(stack);

        if (wrapped == null) {
            return null;
        }

        return wrapped.what();
    }

    public long unwrapAmount(ItemStack stack) {
        if (stack.getItem() != this) {
            return 0;
        }

        var wrapped = AEComponents.WRAPPED_STACK.get(stack);

        if (wrapped == null) {
            return 0;
        }

        return wrapped.amount();
    }

    /**
     * Allows picking up the contained fluid with a bucket. Items of this version have no hook for clicks with a carried
     * stack: menus call this when the player clicks a slot holding a wrapped stack.
     *
     * @param primaryClick true for a left click
     * @return true if the click was handled (the menu must not run its normal slot click)
     */
    public boolean overrideOtherStackedOnMe(ItemStack itemInSlot, ItemStack otherStack, Slot slot,
            boolean primaryClick, EntityPlayer player) {
        if (player.openContainer == null) {
            // We need the opened menu since we're ignoring slotAccess due to no helper being available for it in the
            // transfer API
            return true;
        }

        // When trying to stack onto degenerate wrapped stacks, delete them
        var what = unwrapWhat(itemInSlot);
        if (what == null && slot.getStack() == itemInSlot) {
            LOG.error("Removing a broken wrapped generic stack from player {} slot {}", player,
                    slot.getSlotIndex());
            slot.putStack(ItemStack.EMPTY);
            return true;
        }

        // Allow picking up fluids items with a fluid container, this is a special case for fluids
        if (primaryClick) {
            var heldContainer = ContainerItemStrategies.findCarriedContextForKey(what, player, player.openContainer);
            if (heldContainer != null) {
                long amount = unwrapAmount(itemInSlot);
                long inserted = heldContainer.insert(what, amount, Actionable.MODULATE);

                // Check client to avoid duplicate sounds in singleplayer
                if (player.world.isRemote) {
                    heldContainer.playFillSound(player, what);
                }

                if (inserted >= amount) {
                    slot.putStack(ItemStack.EMPTY);
                } else {
                    slot.putStack(wrap(what, amount - inserted));
                }
            }
        }

        // Generally disallow picking this up
        return true;
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // Don't show this item in CreativeTabs
    }
}
