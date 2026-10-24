/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.menu;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Replacement for the 1.14+ vanilla {@code MenuType}: identifies a kind of menu and knows how to create the client-side
 * instance from the data sent by the server ({@link appeng.core.network.clientbound.OpenMenuPacket}). The server side
 * opens menus with {@link MenuOpener}.
 */
public final class MenuType<T extends Container> {
    private static final Map<ResourceLocation, MenuType<?>> BY_ID = new LinkedHashMap<>();

    private final ResourceLocation id;
    private final ClientFactory<T> clientFactory;

    public MenuType(ResourceLocation id, ClientFactory<T> clientFactory) {
        this.id = id;
        this.clientFactory = clientFactory;
    }

    /**
     * Makes the type known to {@link #byId}, which the client uses to create the menu.
     */
    public static synchronized <T extends Container> MenuType<T> register(MenuType<T> type) {
        var existing = BY_ID.putIfAbsent(type.id, type);
        if (existing != null && existing != type) {
            throw new IllegalStateException("Menu type " + type.id + " is already registered");
        }
        return type;
    }

    @Nullable
    public static synchronized MenuType<?> byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static synchronized Collection<MenuType<?>> getAll() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public ResourceLocation getId() {
        return id;
    }

    /**
     * Creates the client-side menu from the data written by the server when it opened the menu.
     */
    public T create(int windowId, InventoryPlayer playerInventory, RegistryFriendlyByteBuf data) {
        return clientFactory.create(windowId, playerInventory, data);
    }

    @Override
    public String toString() {
        return "MenuType[" + id + "]";
    }

    @FunctionalInterface
    public interface ClientFactory<T> {
        T create(int windowId, InventoryPlayer playerInventory, RegistryFriendlyByteBuf data);
    }
}
