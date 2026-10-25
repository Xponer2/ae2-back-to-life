/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.gui.compat;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Keyboard helpers for the GUI. Key codes are LWJGL 2 {@link Keyboard} codes, the modifier flags of
 * {@link GuiEventListener#keyPressed} are built from the currently pressed modifier keys.
 */
@SideOnly(Side.CLIENT)
public final class Keys {

    public static final int MOD_SHIFT = 1;
    public static final int MOD_CONTROL = 2;
    public static final int MOD_ALT = 4;

    public static final int MOUSE_BUTTON_LEFT = 0;
    public static final int MOUSE_BUTTON_RIGHT = 1;
    public static final int MOUSE_BUTTON_MIDDLE = 2;

    private Keys() {
    }

    public static int currentModifiers() {
        int modifiers = 0;
        if (GuiScreen.isShiftKeyDown()) {
            modifiers |= MOD_SHIFT;
        }
        if (GuiScreen.isCtrlKeyDown()) {
            modifiers |= MOD_CONTROL;
        }
        if (GuiScreen.isAltKeyDown()) {
            modifiers |= MOD_ALT;
        }
        return modifiers;
    }

    public static boolean isKeyDown(int keyCode) {
        return keyCode > 0 && keyCode < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(keyCode);
    }

    /**
     * Whether the key binding is bound to the given key code (mouse bindings never match a key).
     */
    public static boolean matches(KeyBinding binding, int keyCode) {
        return keyCode != 0 && binding.isActiveAndMatches(keyCode);
    }

    public static boolean isSelectAll(int keyCode) {
        return GuiScreen.isKeyComboCtrlA(keyCode);
    }

    public static boolean isCopy(int keyCode) {
        return GuiScreen.isKeyComboCtrlC(keyCode);
    }

    public static boolean isPaste(int keyCode) {
        return GuiScreen.isKeyComboCtrlV(keyCode);
    }

    public static boolean isCut(int keyCode) {
        return GuiScreen.isKeyComboCtrlX(keyCode);
    }
}
