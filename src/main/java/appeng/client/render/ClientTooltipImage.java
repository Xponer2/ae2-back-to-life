/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;

/**
 * Stand-in for the 1.21 ClientTooltipComponent: an image drawn below the text lines of a tooltip. Created from the
 * tooltip data of an item by the factories registered in {@link TooltipImages}.
 */
@SideOnly(Side.CLIENT)
public interface ClientTooltipImage {
    int getHeight();

    int getWidth(FontRenderer font);

    default void renderText(FontRenderer font, int x, int y) {
    }

    default void renderImage(FontRenderer font, int x, int y, GuiGraphics guiGraphics) {
    }
}
