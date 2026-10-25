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

import appeng.client.gui.GuiGraphics;

/**
 * Something drawn on a screen every frame, in screen coordinates.
 */
@SideOnly(Side.CLIENT)
@FunctionalInterface
public interface Renderable {
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
}
