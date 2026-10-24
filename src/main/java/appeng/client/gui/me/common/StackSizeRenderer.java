/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.client.gui.me.common;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;
import appeng.core.AEConfig;

/**
 * @author AlgorithmX2
 * @author thatsIch
 * @version rv2
 * @since rv0
 */
@SideOnly(Side.CLIENT)
public class StackSizeRenderer {
    /**
     * Draws the label with the current GL transform (already scaled by {@code scaleFactor}).
     */
    private static void renderSizeLabel(FontRenderer fontRenderer, float xPos, float yPos, String text,
            boolean largeFonts) {
        final float scaleFactor = largeFonts ? 0.85f : 0.666f;
        final float inverseScaleFactor = 1.0f / scaleFactor;
        final int offset = largeFonts ? 0 : -1;

        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        final int X = (int) ((xPos + offset + 16.0f + 2.0f - fontRenderer.getStringWidth(text) * scaleFactor)
                * inverseScaleFactor);
        final int Y = (int) ((yPos + offset + 16.0f - 5.0f * scaleFactor) * inverseScaleFactor);
        fontRenderer.drawString(text, X + 1, Y + 1, 0x413f54, false);
        fontRenderer.drawString(text, X, Y, 0xffffff, false);
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void renderSizeLabel(GuiGraphics guiGraphics, FontRenderer fontRenderer, float xPos, float yPos,
            String text) {
        renderSizeLabel(guiGraphics, fontRenderer, xPos, yPos, text, AEConfig.instance().isUseLargeFonts());
    }

    public static void renderSizeLabel(GuiGraphics guiGraphics, FontRenderer fontRenderer, float xPos, float yPos,
            String text,
            boolean largeFonts) {
        final float scaleFactor = largeFonts ? 0.85f : 0.666f;

        var stack = guiGraphics.pose();
        stack.pushPose();
        // According to ItemRenderer, text is 200 above items.
        stack.translate(0, 0, 200);
        stack.scale(scaleFactor, scaleFactor, scaleFactor);

        renderSizeLabel(fontRenderer, xPos, yPos, text, largeFonts);

        stack.popPose();
    }

}
