/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;

import appeng.api.components.DataComponentType;

/**
 * Backport of Minecraft's ItemProperties. Collects the settings and applies them to a 1.12.2 {@link Item} via
 * {@link #applyTo(Item)} (done by AE2's base item classes).
 */
public final class ItemProperties {
    private int maxStackSize = 64;
    private int maxDamage;
    @Nullable
    private EnumRarity rarity;
    private boolean fireResistant;
    private boolean noRepair;
    @Nullable
    private Item craftRemainder;
    @Nullable
    private CreativeTabs tab;
    private final Map<DataComponentType<?>, Object> defaultComponents = new IdentityHashMap<>();

    public ItemProperties stacksTo(int maxStackSize) {
        if (this.maxDamage > 0) {
            throw new IllegalStateException("Unable to have damage AND stack.");
        }
        this.maxStackSize = maxStackSize;
        return this;
    }

    public ItemProperties durability(int maxDamage) {
        this.maxDamage = maxDamage;
        this.maxStackSize = 1;
        return this;
    }

    public ItemProperties defaultDurability(int maxDamage) {
        return this.maxDamage == 0 ? durability(maxDamage) : this;
    }

    public ItemProperties rarity(EnumRarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public ItemProperties fireResistant() {
        this.fireResistant = true;
        return this;
    }

    public ItemProperties setNoRepair() {
        this.noRepair = true;
        return this;
    }

    public ItemProperties craftRemainder(Item item) {
        this.craftRemainder = item;
        return this;
    }

    public ItemProperties tab(CreativeTabs tab) {
        this.tab = tab;
        return this;
    }

    /**
     * Default component values. On 1.12.2 there are no default components, the values are reported by
     * {@link #getDefaultComponent} and AE2's code falls back to them when a stack has no explicit value.
     */
    public <T> ItemProperties component(DataComponentType<T> type, T value) {
        defaultComponents.put(type, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getDefaultComponent(DataComponentType<T> type) {
        return (T) defaultComponents.get(type);
    }

    public Map<DataComponentType<?>, Object> getDefaultComponents() {
        return defaultComponents;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    @Nullable
    public EnumRarity getRarity() {
        return rarity;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }

    public void applyTo(Item item) {
        item.setMaxStackSize(maxStackSize);
        if (maxDamage > 0) {
            item.setMaxDamage(maxDamage);
        }
        if (noRepair) {
            item.setNoRepair();
        }
        if (craftRemainder != null) {
            item.setContainerItem(craftRemainder);
        }
        if (tab != null) {
            item.setCreativeTab(tab);
        }
    }
}
