/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.jei;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;

/**
 * Keeps the JEI item list away from the extra panels of AE2 screens (the exclusion zones of the 1.21 plugins) and
 * lets JEI look up the stack under the mouse, e.g. in the terminal's network inventory.
 */
@SuppressWarnings("rawtypes")
class AdvancedGuiHandler implements mezz.jei.api.gui.IAdvancedGuiHandler<AEBaseScreen> {

    @Override
    public Class<AEBaseScreen> getGuiContainerClass() {
        return AEBaseScreen.class;
    }

    @Override
    public List<Rectangle> getGuiExtraAreas(AEBaseScreen screen) {
        List<appeng.client.gui.Rect2i> zones = screen.getExclusionZones();
        return zones.stream()
                .map(ez -> new Rectangle(ez.getX(), ez.getY(), ez.getWidth(), ez.getHeight()))
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable Object getIngredientUnderMouse(AEBaseScreen screen, int mouseX, int mouseY) {
        var stack = screen.getStackUnderMouse(mouseX, mouseY);
        if (stack == null) {
            return null;
        }
        var what = stack.stack().what();
        if (what instanceof AEItemKey itemKey) {
            return itemKey.toStack();
        } else if (what instanceof AEFluidKey fluidKey) {
            return fluidKey.toStack(1000);
        }
        return null;
    }
}
