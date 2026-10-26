/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.waila;

import java.util.List;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import appeng.api.integrations.igtooltip.TooltipBuilder;

/**
 * 1.12.2 only: Hwyla tooltips are lists of formatted strings, built on the client.
 */
class WailaTooltipBuilder implements TooltipBuilder {
    private final List<String> tooltip;

    WailaTooltipBuilder(List<String> tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void addLine(ITextComponent line) {
        tooltip.add(line.getFormattedText());
    }

    @Override
    public void addLine(ITextComponent line, ResourceLocation id) {
        addLine(line);
    }
}
