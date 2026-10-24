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

package appeng.client.gui.widgets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import appeng.client.Point;
import appeng.client.gui.GuiGraphics;
import appeng.client.gui.Rect2i;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;

/**
 * A modified version of the Minecraft text field. You can initialize it over the full element span. The mouse click
 * area is increased to the full element subtracted with the defined padding.
 * <p>
 * The rendering does pay attention to the size of the '_' caret.
 */
public class AETextField extends GuiTextField implements IResizableWidget, ITooltip {
    private static final Blitter BLITTER = Blitter.texture("guis/text_field.png", 128, 128);

    private static final int PADDING = 2;

    private final int fontPad;
    private final ScreenStyle style;
    private int selectionColor;
    private List<ITextComponent> tooltipMessage = Collections.emptyList();

    /**
     * Displayed with a muted text color when the text box is unfocused and has no content.
     */
    @Nullable
    private ITextComponent placeholder;

    /**
     * Uses the values to instantiate a padded version of a text field. Pays attention to the '_' caret.
     *
     * @param fontRenderer renderer for the strings
     * @param xPos         absolute left position
     * @param yPos         absolute top position
     * @param width        absolute width
     * @param height       absolute height
     */
    public AETextField(ScreenStyle style, FontRenderer fontRenderer, int xPos, int yPos, int width,
            int height) {
        super(fontRenderer, xPos + PADDING, yPos + PADDING,
                width - 2 * PADDING - fontRenderer.getStringWidth("_"), height - 2 * PADDING,
                new TextComponentString(""));

        this.style = style;
        this.fontPad = fontRenderer.getStringWidth("_");
        setSelectionColor(style.getColor(PaletteColor.TEXTFIELD_SELECTION).toARGB());
        setTextColor(style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB());
    }

    // Extend the clickable area by the padding so we don't have a mouse deadzone that is still visually within
    // the background we render.
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        var bounds = getVisualBounds();
        return mouseX >= bounds.left && mouseX < bounds.right
                && mouseY >= bounds.top && mouseY < bounds.bottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // This hack is used to allow the standard mouse-click logic to recognize our clicks
        // that are on the padding, but not really inside the edit box.
        if (isMouseOver(mouseX, mouseY)) {
            mouseX = MathHelper.clamp(mouseX, getX(), getX() + width - 1);
            mouseY = MathHelper.clamp(mouseY, getY(), getY() + height - 1);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Swallow all key presses except for focus escape when we're focused to prevent "e" from
        // closing the window instead of typing into the text field
        return isFocused() && canConsumeInput() && keyCode != GLFW.GLFW_KEY_TAB && keyCode != GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public void move(Point pos) {
        super.setX(pos.getX() + PADDING);
        setY(pos.getY() + PADDING);
    }

    @Override
    public void resize(int width, int height) {
        super.setWidth(width - 2 * PADDING - fontPad);
        this.height = height - 2 * PADDING;
    }

    public void selectAll() {
        this.moveCursorTo(0, false);
        this.setHighlightPos(this.getMaxLength());
    }

    public void setSelectionColor(int color) {
        this.selectionColor = color;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (this.isVisible()) {
            var yOffset = 0;
            if (!this.isEditable()) {
                yOffset = 12;
            } else if (isFocused()) {
                yOffset = 24;
            }

            var bounds = getVisualBounds();

            BLITTER.src(0, yOffset, 1, 12)
                    .dest(bounds.left, bounds.top)
                    .blit(guiGraphics);
            var backgroundWidth = Math.min(126, bounds.right - bounds.left - 2);
            BLITTER.src(1, yOffset, backgroundWidth, 12)
                    .dest(bounds.left + 1, bounds.top)
                    .blit(guiGraphics);
            BLITTER.src(127, yOffset, 1, 12)
                    .dest(bounds.right - 1, bounds.top)
                    .blit(guiGraphics);

            super.renderWidget(guiGraphics, mouseX, mouseY, partial);

            // Render a placeholder value if the text field isn't focused and is empty
            if (placeholder != null && !isFocused() && getValue().isEmpty()) {
                var font = Minecraft.getMinecraft().fontRenderer;
                guiGraphics.drawString(font, placeholder, getX(), getY(),
                        style.getColor(PaletteColor.TEXTFIELD_PLACEHOLDER).toARGB(), false);
            }
        }
    }

    @Override
    public void renderHighlight(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY) {
        if (!this.isFocused()) {
            return;
        }

        if (startX < endX) {
            int i = startX;
            startX = endX;
            endX = i;
        }

        startX += 1;
        endX -= 1;

        if (startY < endY) {
            int j = startY;
            startY = endY;
            endY = j;
        }

        startY -= PADDING;

        endX = MathHelper.clamp(endX, getX(), getX() + this.width);
        startX = MathHelper.clamp(startX, getX(), getX() + this.width);

        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        guiGraphics.fill(startX, startY, endX, endY, this.selectionColor);
        RenderSystem.disableColorLogicOp();
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(
                getX() - PADDING,
                getY() - PADDING,
                width + 2 * PADDING + fontPad,
                height + 2 * PADDING);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    @NotNull
    @Override
    public List<ITextComponent> getTooltipMessage() {
        return tooltipMessage;
    }

    public void setTooltipMessage(List<ITextComponent> tooltipMessage) {
        this.tooltipMessage = Objects.requireNonNull(tooltipMessage);
    }

    private VisualBounds getVisualBounds() {
        // Render background
        int left = getX() - PADDING;
        int top = getY() - PADDING;
        int right = left + width + 2 * PADDING + fontPad;
        return new VisualBounds(
                left,
                top,
                right,
                top + height + 2 * PADDING);
    }

    public void setPlaceholder(ITextComponent placeholder) {
        this.placeholder = placeholder;
    }

    public ITextComponent getPlaceholder() {
        return placeholder;
    }

    private final static class VisualBounds {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        public VisualBounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int left() {
            return left;
        }

        public int top() {
            return top;
        }

        public int right() {
            return right;
        }

        public int bottom() {
            return bottom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VisualBounds)) return false;
            VisualBounds that = (VisualBounds) o;
            return left == that.left
                    && top == that.top
                    && right == that.right
                    && bottom == that.bottom;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(left, top, right, bottom);
        }

        @Override
        public String toString() {
            return "VisualBounds[" + "left=" + left + ", " + "top=" + top + ", " + "right=" + right + ", " + "bottom=" + bottom + "]";
        }

    }
}
