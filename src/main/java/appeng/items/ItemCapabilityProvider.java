/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.items;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import appeng.api.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the item capabilities registered in {@link RegisterCapabilitiesEvent} (energy for powered tools, ...) on a
 * 1.12.2 item stack.
 */
public final class ItemCapabilityProvider implements ICapabilityProvider {
    private final ItemStack stack;

    private ItemCapabilityProvider(ItemStack stack) {
        this.stack = stack;
    }

    @Nullable
    public static ICapabilityProvider forStack(ItemStack stack) {
        if (!RegisterCapabilitiesEvent.get().hasItemCapabilities(stack.getItem())) {
            return null;
        }
        return new ItemCapabilityProvider(stack);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return RegisterCapabilitiesEvent.get().getItemCapability(stack, capability) != null;
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return RegisterCapabilitiesEvent.get().getItemCapability(stack, capability);
    }
}
