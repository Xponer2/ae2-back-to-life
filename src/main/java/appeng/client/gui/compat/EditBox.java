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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;

/**
 * A single line text input with cursor, selection, scrolling and clipboard support, with the API of the edit box of
 * newer Minecraft versions (value, responder, filter, highlight position, ...).
 */
@SideOnly(Side.CLIENT)
public class EditBox extends AbstractWidget {

    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0;
    private static final int DEFAULT_TEXT_COLOR_UNEDITABLE = 0x707070;
    private static final int CURSOR_BLINK_MS = 300;

    private final FontRenderer font;
    private String value = "";
    private int maxLength = 32;
    private boolean bordered = true;
    private boolean canLoseFocus = true;
    private boolean editable = true;
    // Index of the first visible character
    private int displayPos;
    private int cursorPos;
    // The other end of the selection (== cursorPos without selection)
    private int highlightPos;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int textColorUneditable = DEFAULT_TEXT_COLOR_UNEDITABLE;
    @Nullable
    private String suggestion;
    @Nullable
    private ITextComponent hint;
    @Nullable
    private Consumer<String> responder;
    private Predicate<String> filter = Objects::nonNull;
    private long focusedTime = Minecraft.getSystemTime();

    public EditBox(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
        super(x, y, width, height, message);
        this.font = font;
    }

    public void setResponder(@Nullable Consumer<String> responder) {
        this.responder = responder;
    }

    public void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    public void setValue(String text) {
        if (!this.filter.test(text)) {
            return;
        }
        this.value = text.length() > this.maxLength ? text.substring(0, this.maxLength) : text;
        moveCursorToEnd(false);
        setHighlightPos(this.cursorPos);
        onValueChange(this.value);
    }

    public String getValue() {
        return this.value;
    }

    public String getHighlighted() {
        int start = Math.min(this.cursorPos, this.highlightPos);
        int end = Math.max(this.cursorPos, this.highlightPos);
        return this.value.substring(start, end);
    }

    public void setMaxLength(int length) {
        this.maxLength = length;
        if (this.value.length() > length) {
            this.value = this.value.substring(0, length);
            onValueChange(this.value);
        }
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    private void onValueChange(String newText) {
        if (this.responder != null) {
            this.responder.accept(newText);
        }
    }

    /**
     * Replaces the selection with the given text (only the characters allowed in chat, cut to the maximum length).
     */
    public void insertText(String textToWrite) {
        int start = Math.min(this.cursorPos, this.highlightPos);
        int end = Math.max(this.cursorPos, this.highlightPos);
        int room = this.maxLength - this.value.length() + (end - start);
        String filtered = ChatAllowedCharacters.filterAllowedCharacters(textToWrite);
        if (filtered.length() > room) {
            filtered = filtered.substring(0, Math.max(0, room));
        }
        String newValue = new StringBuilder(this.value).replace(start, end, filtered).toString();
        if (!this.filter.test(newValue)) {
            return;
        }
        this.value = newValue;
        setCursorPosition(start + filtered.length());
        setHighlightPos(this.cursorPos);
        onValueChange(this.value);
    }

    /**
     * Deletes the selection, or the given number of characters before (negative) or after the cursor.
     */
    public void deleteChars(int num) {
        if (this.value.isEmpty()) {
            return;
        }
        if (this.highlightPos != this.cursorPos) {
            insertText("");
            return;
        }
        int target = MathHelper.clamp(this.cursorPos + num, 0, this.value.length());
        int start = Math.min(target, this.cursorPos);
        int end = Math.max(target, this.cursorPos);
        if (start == end) {
            return;
        }
        String newValue = new StringBuilder(this.value).delete(start, end).toString();
        if (!this.filter.test(newValue)) {
            return;
        }
        this.value = newValue;
        moveCursorTo(start, false);
        onValueChange(this.value);
    }

    /**
     * Deletes up to the start of the previous (negative) or next word.
     */
    public void deleteWords(int num) {
        if (this.value.isEmpty()) {
            return;
        }
        if (this.highlightPos != this.cursorPos) {
            insertText("");
            return;
        }
        deleteChars(getWordPosition(num) - this.cursorPos);
    }

    /**
     * @return The cursor position after jumping over the given number of words (negative = backwards).
     */
    public int getWordPosition(int numWords) {
        int pos = this.cursorPos;
        boolean backwards = numWords < 0;
        for (int i = 0; i < Math.abs(numWords); i++) {
            if (backwards) {
                while (pos > 0 && this.value.charAt(pos - 1) == ' ') {
                    pos--;
                }
                while (pos > 0 && this.value.charAt(pos - 1) != ' ') {
                    pos--;
                }
            } else {
                while (pos < this.value.length() && this.value.charAt(pos) != ' ') {
                    pos++;
                }
                while (pos < this.value.length() && this.value.charAt(pos) == ' ') {
                    pos++;
                }
            }
        }
        return pos;
    }

    public void moveCursor(int delta, boolean select) {
        moveCursorTo(this.cursorPos + delta, select);
    }

    public void moveCursorTo(int pos, boolean select) {
        setCursorPosition(pos);
        if (!select) {
            setHighlightPos(this.cursorPos);
        }
        onValueChange(this.value);
    }

    public void setCursorPosition(int pos) {
        this.cursorPos = MathHelper.clamp(pos, 0, this.value.length());
        scrollTo(this.cursorPos);
    }

    public int getCursorPosition() {
        return this.cursorPos;
    }

    public void moveCursorToStart(boolean select) {
        moveCursorTo(0, select);
    }

    public void moveCursorToEnd(boolean select) {
        moveCursorTo(this.value.length(), select);
    }

    public void setHighlightPos(int position) {
        this.highlightPos = MathHelper.clamp(position, 0, this.value.length());
        scrollTo(this.highlightPos);
    }

    /**
     * Scrolls the text so that the character at the given position is visible.
     */
    private void scrollTo(int position) {
        if (this.font == null) {
            return;
        }
        this.displayPos = Math.min(this.displayPos, this.value.length());
        int innerWidth = getInnerWidth();
        String visible = this.font.trimStringToWidth(this.value.substring(this.displayPos), innerWidth);
        int lastVisible = visible.length() + this.displayPos;
        if (position == this.displayPos) {
            this.displayPos -= this.font.trimStringToWidth(this.value, innerWidth, true).length();
        }
        if (position > lastVisible) {
            this.displayPos += position - lastVisible;
        } else if (position <= this.displayPos) {
            this.displayPos -= this.displayPos - position;
        }
        this.displayPos = MathHelper.clamp(this.displayPos, 0, this.value.length());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!canConsumeInput()) {
            return false;
        }
        boolean shift = (modifiers & Keys.MOD_SHIFT) != 0;
        boolean word = (modifiers & Keys.MOD_CONTROL) != 0;
        if (Keys.isSelectAll(keyCode)) {
            moveCursorToEnd(false);
            setHighlightPos(0);
            return true;
        } else if (Keys.isCopy(keyCode)) {
            GuiScreen.setClipboardString(getHighlighted());
            return true;
        } else if (Keys.isPaste(keyCode)) {
            if (this.editable) {
                insertText(GuiScreen.getClipboardString());
            }
            return true;
        } else if (Keys.isCut(keyCode)) {
            GuiScreen.setClipboardString(getHighlighted());
            if (this.editable) {
                insertText("");
            }
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_BACK -> {
                if (this.editable) {
                    if (word) {
                        deleteWords(-1);
                    } else {
                        deleteChars(-1);
                    }
                }
                return true;
            }
            case Keyboard.KEY_DELETE -> {
                if (this.editable) {
                    if (word) {
                        deleteWords(1);
                    } else {
                        deleteChars(1);
                    }
                }
                return true;
            }
            case Keyboard.KEY_RIGHT -> {
                if (word) {
                    moveCursorTo(getWordPosition(1), shift);
                } else {
                    moveCursor(1, shift);
                }
                return true;
            }
            case Keyboard.KEY_LEFT -> {
                if (word) {
                    moveCursorTo(getWordPosition(-1), shift);
                } else {
                    moveCursor(-1, shift);
                }
                return true;
            }
            case Keyboard.KEY_HOME -> {
                moveCursorToStart(shift);
                return true;
            }
            case Keyboard.KEY_END -> {
                moveCursorToEnd(shift);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!canConsumeInput()) {
            return false;
        }
        if (ChatAllowedCharacters.isAllowedCharacter(codePoint)) {
            if (this.editable) {
                insertText(Character.toString(codePoint));
            }
            return true;
        }
        return false;
    }

    public boolean canConsumeInput() {
        return isVisible() && isFocused() && isEditable();
    }

    /**
     * Places the cursor under the mouse. The screen focuses the box when the click is accepted.
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
        int relativeX = MathHelper.floor(mouseX) - getX();
        if (this.bordered) {
            relativeX -= 4;
        }
        String visible = this.font.trimStringToWidth(this.value.substring(this.displayPos), getInnerWidth());
        moveCursorTo(this.font.trimStringToWidth(visible, relativeX).length() + this.displayPos,
                GuiScreen.isShiftKeyDown());
    }

    @Override
    public void playDownSound(SoundHandler soundHandler) {
        // Text fields don't click
    }

    @Override
    public void setFocused(boolean focused) {
        if (!this.canLoseFocus && !focused) {
            return;
        }
        if (focused && !isFocused()) {
            this.focusedTime = Minecraft.getSystemTime();
        }
        super.setFocused(focused);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) {
            return;
        }
        if (this.bordered) {
            int borderColor = isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + this.width + 1, getY() + this.height + 1, borderColor);
            guiGraphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xFF000000);
        }

        int color = this.editable ? this.textColor : this.textColorUneditable;
        int cursorInView = this.cursorPos - this.displayPos;
        String visible = this.font.trimStringToWidth(this.value.substring(this.displayPos), getInnerWidth());
        boolean cursorVisible = cursorInView >= 0 && cursorInView <= visible.length();
        boolean blink = isFocused() && (Minecraft.getSystemTime() - this.focusedTime) / CURSOR_BLINK_MS % 2 == 0
                && cursorVisible;
        int textX = this.bordered ? getX() + 4 : getX();
        int textY = this.bordered ? getY() + (this.height - 8) / 2 : getY();
        int afterCursorX = textX;
        int highlightInView = MathHelper.clamp(this.highlightPos - this.displayPos, 0, visible.length());

        if (!visible.isEmpty()) {
            String beforeCursor = cursorVisible ? visible.substring(0, cursorInView) : visible;
            afterCursorX = guiGraphics.drawString(this.font, beforeCursor, textX, textY, color, true);
        }

        boolean cursorInText = this.cursorPos < this.value.length() || this.value.length() >= this.maxLength;
        int cursorX = afterCursorX;
        if (!cursorVisible) {
            cursorX = cursorInView > 0 ? textX + this.width : textX;
        } else if (cursorInText) {
            cursorX = afterCursorX - 1;
            afterCursorX--;
        }

        if (!visible.isEmpty() && cursorVisible && cursorInView < visible.length()) {
            guiGraphics.drawString(this.font, visible.substring(cursorInView), afterCursorX, textY, color, true);
        }

        if (this.hint != null && visible.isEmpty() && !isFocused()) {
            guiGraphics.drawString(this.font, this.hint, afterCursorX, textY, color, true);
        }
        if (!cursorInText && this.suggestion != null) {
            guiGraphics.drawString(this.font, this.suggestion, cursorX - 1, textY, 0xFF808080, true);
        }

        if (blink) {
            if (cursorInText) {
                guiGraphics.fill(cursorX, textY - 1, cursorX + 1, textY + 1 + this.font.FONT_HEIGHT, 0xFFD0D0D0);
            } else {
                guiGraphics.drawString(this.font, "_", cursorX, textY, color, true);
            }
        }

        if (highlightInView != cursorInView) {
            int highlightX = textX + this.font.getStringWidth(visible.substring(0, highlightInView));
            renderHighlight(guiGraphics, cursorX, textY - 1, highlightX - 1, textY + 1 + this.font.FONT_HEIGHT);
        }
    }

    /**
     * Draws the selection by inverting the area (the selection color is OR-ed with the inverted background).
     */
    public void renderHighlight(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY) {
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX);
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY);
        maxX = Math.min(maxX, getX() + this.width);
        minX = Math.min(minX, getX() + this.width);

        GlStateManager.enableColorLogic();
        GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
        guiGraphics.fill(minX, minY, maxX, maxY, 0xFF0000FF);
        GlStateManager.disableColorLogic();
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    public void setBordered(boolean bordered) {
        this.bordered = bordered;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setTextColorUneditable(int color) {
        this.textColorUneditable = color;
    }

    public void setCanLoseFocus(boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
    }

    public int getInnerWidth() {
        return this.bordered ? this.width - 8 : this.width;
    }

    public void setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
    }

    public void setHint(@Nullable ITextComponent hint) {
        this.hint = hint;
    }

    /**
     * @return The screen x coordinate of the given character index.
     */
    public int getScreenX(int charNum) {
        if (charNum > this.value.length()) {
            return getX();
        }
        int start = Math.min(this.displayPos, charNum);
        return getX() + this.font.getStringWidth(this.value.substring(start, charNum));
    }

    protected FontRenderer getFont() {
        return this.font;
    }
}
