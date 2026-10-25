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

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Input events of a GUI element, in the shape of the input handling of newer Minecraft versions: mouse coordinates
 * are screen coordinates, the methods return true if they consumed the event. Key codes are LWJGL 2
 * {@link org.lwjgl.input.Keyboard} codes; the scan code is unused and the modifiers are {@link Keys} flags.
 */
@SideOnly(Side.CLIENT)
public interface GuiEventListener {
    default void mouseMoved(double mouseX, double mouseY) {
    }

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    default boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    void setFocused(boolean focused);

    boolean isFocused();
}
