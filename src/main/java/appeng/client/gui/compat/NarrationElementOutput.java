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

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Receives the narration of a widget. Minecraft 1.12.2 has no screen narration, so nothing consumes it; it only
 * keeps the widgets' narration code in place.
 */
@SideOnly(Side.CLIENT)
public interface NarrationElementOutput {

    enum NarratedElementType {
        TITLE, POSITION, HINT, USAGE
    }

    NarrationElementOutput NONE = (type, text) -> {
    };

    void add(NarratedElementType type, ITextComponent text);

    default void add(NarratedElementType type, String text) {
        add(type, new net.minecraft.util.text.TextComponentString(text));
    }
}
