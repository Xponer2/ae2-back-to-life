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

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;

/**
 * A rectangular GUI element with a message, in the style of the widgets of newer Minecraft versions (the 1.12.2
 * GuiButton has a different API). Added to an {@link AbstractContainerScreen} with
 * {@link AbstractContainerScreen#addRenderableWidget}, which forwards rendering and input to it.
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractWidget implements Renderable, GuiEventListener {
    // Public like the fields of the button of the game, code written against it reads them directly
    public int width;
    public int height;
    private int x;
    private int y;
    private ITextComponent message;
    protected boolean isHovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0F;
    private boolean focused;

    public AbstractWidget(int x, int y, int width, int height, ITextComponent message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        this.isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + this.width
                && mouseY < getY() + this.height;
        renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected abstract void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /**
     * There is no narration in 1.12.2, kept for the widgets that describe themselves.
     */
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    protected void defaultButtonNarrationText(NarrationElementOutput output) {
        output.add(NarrationElementOutput.NarratedElementType.TITLE, createNarrationMessage());
    }

    protected ITextComponent createNarrationMessage() {
        return getMessage();
    }

    public boolean isHovered() {
        return this.isHovered;
    }

    public boolean isHoveredOrFocused() {
        return this.isHovered || this.focused;
    }

    public boolean isActive() {
        return this.visible && this.active;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getRight() {
        return getX() + this.width;
    }

    public int getBottom() {
        return getY() + this.height;
    }

    public ITextComponent getMessage() {
        return this.message;
    }

    public void setMessage(ITextComponent message) {
        this.message = message;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void onClick(double mouseX, double mouseY) {
    }

    public void onRelease(double mouseX, double mouseY) {
    }

    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
    }

    protected boolean isValidClickButton(int button) {
        return button == Keys.MOUSE_BUTTON_LEFT;
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= getX() && mouseY >= getY()
                && mouseX < getX() + this.width && mouseY < getY() + this.height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && isValidClickButton(button) && clicked(mouseX, mouseY)) {
            playDownSound(Minecraft.getMinecraft().getSoundHandler());
            onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isValidClickButton(button)) {
            onRelease(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isValidClickButton(button)) {
            onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= getX() && mouseY >= getY()
                && mouseX < getX() + this.width && mouseY < getY() + this.height;
    }

    public void playDownSound(SoundHandler soundHandler) {
        soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }
}
