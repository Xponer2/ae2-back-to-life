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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;

/**
 * A widget that is pressed by clicking it or with enter/space while it is focused.
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractButton extends AbstractWidget {

    // The button texture of the Minecraft installation (not part of this mod): 200x20 buttons at v = 46/66/86
    private static final ResourceLocation VANILLA_WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    public AbstractButton(int x, int y, int width, int height, ITextComponent message) {
        super(x, y, width, height, message);
    }

    public abstract void onPress();

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.active || !this.visible) {
            return false;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE
                || keyCode == Keyboard.KEY_NUMPADENTER) {
            playDownSound(Minecraft.getMinecraft().getSoundHandler());
            onPress();
            return true;
        }
        return false;
    }

    /**
     * The look of a standard button: the button texture of the game, stretched to the size of the widget by drawing
     * its left and right halves, and the centered message.
     */
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int v = !this.active ? 46 : isHoveredOrFocused() ? 86 : 66;
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        int halfWidth = this.width / 2;
        int halfHeight = this.height / 2;
        // Each quarter comes from the matching corner of the 200x20 texture, so borders stay crisp
        guiGraphics.blit(VANILLA_WIDGETS, getX(), getY(), 0, v, halfWidth, halfHeight);
        guiGraphics.blit(VANILLA_WIDGETS, getX() + halfWidth, getY(), 200 - (this.width - halfWidth), v,
                this.width - halfWidth, halfHeight);
        guiGraphics.blit(VANILLA_WIDGETS, getX(), getY() + halfHeight, 0, v + 20 - (this.height - halfHeight),
                halfWidth, this.height - halfHeight);
        guiGraphics.blit(VANILLA_WIDGETS, getX() + halfWidth, getY() + halfHeight,
                200 - (this.width - halfWidth), v + 20 - (this.height - halfHeight), this.width - halfWidth,
                this.height - halfHeight);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        renderString(guiGraphics, Minecraft.getMinecraft().fontRenderer,
                color | Math.round(this.alpha * 255.0F) << 24);
    }

    public void renderString(GuiGraphics guiGraphics, FontRenderer font, int color) {
        var text = getMessage().getFormattedText();
        guiGraphics.drawCenteredString(font, text, getX() + this.width / 2, getY() + (this.height - 8) / 2, color);
    }
}
