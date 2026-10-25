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

import java.util.function.Supplier;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A button that runs an action when pressed.
 */
@SideOnly(Side.CLIENT)
public class Button extends AbstractButton {

    @FunctionalInterface
    public interface OnPress {
        void onPress(Button button);
    }

    @FunctionalInterface
    public interface CreateNarration {
        ITextComponent createNarrationMessage(Supplier<ITextComponent> defaultMessage);
    }

    protected static final CreateNarration DEFAULT_NARRATION = Supplier::get;

    protected final OnPress onPress;
    protected final CreateNarration createNarration;

    protected Button(int x, int y, int width, int height, ITextComponent message, OnPress onPress,
            CreateNarration createNarration) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.createNarration = createNarration;
    }

    public static Builder builder(ITextComponent message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public static class Builder {
        private final ITextComponent message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private CreateNarration createNarration = DEFAULT_NARRATION;

        public Builder(ITextComponent message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            return pos(x, y).size(width, height);
        }

        public Builder createNarration(CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        public Button build() {
            return new Button(this.x, this.y, this.width, this.height, this.message, this.onPress,
                    this.createNarration);
        }
    }
}
