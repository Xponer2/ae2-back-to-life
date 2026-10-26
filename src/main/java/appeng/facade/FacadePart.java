/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.facade;

import java.util.Objects;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import appeng.api.ids.AEComponents;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.util.InteractionUtil;

public class FacadePart implements IFacadePart {

    private final EnumFacing side;
    private IBlockState facade;

    public FacadePart(IBlockState facade, EnumFacing side) {
        this.side = Objects.requireNonNull(side, "side");
        this.facade = Objects.requireNonNull(facade, "facade");
    }

    @Override
    public ItemStack getItemStack() {
        return AEItems.FACADE.get().createFacadeForItemUnchecked(getTextureItem());
    }

    @Override
    public void getBoxes(IPartCollisionHelper ch, boolean itemEntity) {
        if (itemEntity) {
            // the box is 15.9 for annihilation planes to pick up collision events.
            ch.addBox(0.0, 0.0, 15, 16.0, 16.0, 15.9);
        } else {
            // prevent weird snag behavior
            ch.addBox(0.0, 0.0, 15, 16.0, 16.0, 16.0);
        }
    }

    @Override
    public EnumFacing getSide() {
        return this.side;
    }

    @Override
    public Item getItem() {
        return Item.getItemFromBlock(facade.getBlock());
    }

    @Override
    public ItemStack getTextureItem() {
        // The variant (wool color, plank type) is the metadata of the item
        return new ItemStack(getItem(), 1, facade.getBlock().damageDropped(facade));
    }

    @Override
    public IBlockState getBlockState() {
        return facade;
    }

    private void setBlockState(IBlockState blockState) {
        facade = blockState;
    }

    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (!InteractionUtil.canWrenchRotate(heldItem))
            return false;

        return handleInteraction(player, true, heldItem);
    }

    public boolean onClicked(EntityPlayer player, Vec3d pos) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (!InteractionUtil.canWrenchRotate(heldItem))
            return false;

        return handleInteraction(player, false, heldItem);
    }

    private boolean handleInteraction(EntityPlayer player, boolean shouldCycleState, ItemStack heldItem) {
        var statedefinition = getBlockState().getBlock().getBlockState();
        var properties = statedefinition.getProperties();
        if (properties.isEmpty()) {
            return false;
        }

        var firstProperty = properties.iterator().next();
        var cyclePropertyName = AEComponents.FACADE_CYCLE_PROPERTY.getOrDefault(heldItem, firstProperty.getName());
        var property = statedefinition.getProperty(cyclePropertyName);
        if (property == null) {
            // Fall back to the first property if the wrench was set to a property that does not exist on this facade
            property = firstProperty;
        }

        if (shouldCycleState) {
            var newState = getBlockState().cycleProperty(property);
            setBlockState(newState);

            // If we reached the default value of the property, we consider that wrapping and show
            // a message indicating to the player that they can left-click to change which property is cycled
            var defaultValue = getBlockState().getBlock().getDefaultState().getValue(property);
            if (Objects.equals(newState.getValue(property), defaultValue)) {
                message(player, PlayerMessages.FacadePropertyWrapped.text(property.getName()));
            }
        } else {
            property = findNext(properties, property);
            if (property == firstProperty) {
                AEComponents.FACADE_CYCLE_PROPERTY.remove(heldItem);
            } else {
                AEComponents.FACADE_CYCLE_PROPERTY.set(heldItem, property.getName());
            }
            message(player, PlayerMessages.FacadePropertySelected.text(property.getName()));
        }
        return true;
    }

    /**
     * @return The element after the given one, or the first one after the last (or if it isn't there).
     */
    private static <T> T findNext(Iterable<T> values, T current) {
        var it = values.iterator();
        T first = it.next();
        T value = first;
        while (value != current) {
            if (!it.hasNext()) {
                return first;
            }
            value = it.next();
        }
        return it.hasNext() ? it.next() : first;
    }

    private static void message(EntityPlayer player, ITextComponent messageComponent) {
        if (player instanceof EntityPlayerMP serverPlayer) {
            serverPlayer.sendStatusMessage(messageComponent, true);
        }
    }
}
