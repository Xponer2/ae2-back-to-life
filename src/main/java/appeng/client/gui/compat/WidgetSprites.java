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

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The GUI sprites of a widget for its states (see {@link appeng.client.gui.GuiGraphics#blitSprite}).
 */
@SideOnly(Side.CLIENT)
public final class WidgetSprites {
    private final ResourceLocation enabled;
    private final ResourceLocation disabled;
    private final ResourceLocation enabledFocused;
    private final ResourceLocation disabledFocused;

    public WidgetSprites(ResourceLocation enabled, ResourceLocation enabledFocused) {
        this(enabled, enabled, enabledFocused, enabled);
    }

    public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused) {
        this(enabled, disabled, enabledFocused, disabled);
    }

    public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused,
            ResourceLocation disabledFocused) {
        this.enabled = enabled;
        this.disabled = disabled;
        this.enabledFocused = enabledFocused;
        this.disabledFocused = disabledFocused;
    }

    public ResourceLocation get(boolean enabled, boolean focused) {
        if (enabled) {
            return focused ? this.enabledFocused : this.enabled;
        }
        return focused ? this.disabledFocused : this.disabled;
    }

    public ResourceLocation enabled() {
        return enabled;
    }

    public ResourceLocation disabled() {
        return disabled;
    }

    public ResourceLocation enabledFocused() {
        return enabledFocused;
    }

    public ResourceLocation disabledFocused() {
        return disabledFocused;
    }
}
