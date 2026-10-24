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

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;

/**
 * Client-side replacement for vanilla {@code MenuScreens}: maps a {@link MenuType} to the factory of its screen.
 * {@code InitScreens} registers the AE2 screens here.
 */
@SideOnly(Side.CLIENT)
public final class MenuScreens {
    private static final Map<MenuType<?>, ScreenConstructor<?, ?>> FACTORIES = new IdentityHashMap<>();

    private MenuScreens() {
    }

    @FunctionalInterface
    public interface ScreenConstructor<M extends Container, U extends GuiScreen> {
        U create(M menu, InventoryPlayer playerInventory, ITextComponent title);
    }

    public static synchronized <M extends Container, U extends GuiScreen> void register(MenuType<? extends M> type,
            ScreenConstructor<M, U> factory) {
        if (FACTORIES.putIfAbsent(type, factory) != null) {
            throw new IllegalStateException("Duplicate screen registration for " + type);
        }
    }

    public static synchronized boolean hasScreen(MenuType<?> type) {
        return FACTORIES.containsKey(type);
    }

    /**
     * Shows the screen of a menu that the server opened, and makes the menu the player's open container.
     *
     * @return false if no screen is registered for the type
     */
    @SuppressWarnings("unchecked")
    public static <M extends Container> boolean open(MenuType<M> type, M menu, EntityPlayer player,
            ITextComponent title) {
        ScreenConstructor<M, ?> factory;
        synchronized (MenuScreens.class) {
            factory = (ScreenConstructor<M, ?>) FACTORIES.get(type);
        }
        if (factory == null) {
            AELog.warn("No screen registered for menu type %s", type.getId());
            return false;
        }
        var screen = factory.create(menu, player.inventory, title);
        Minecraft.getMinecraft().displayGuiScreen(screen);
        player.openContainer = menu;
        return true;
    }
}
