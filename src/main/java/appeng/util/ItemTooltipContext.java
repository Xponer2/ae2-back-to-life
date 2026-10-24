/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.World;

/**
 * Backport of Minecraft's ItemTooltipContext (on 1.12.2 only the level is available).
 */
public final class ItemTooltipContext {
    public static final ItemTooltipContext EMPTY = new ItemTooltipContext(null);

    @Nullable
    private final World level;

    private ItemTooltipContext(@Nullable World level) {
        this.level = level;
    }

    public static ItemTooltipContext of(@Nullable World level) {
        return level == null ? EMPTY : new ItemTooltipContext(level);
    }

    @Nullable
    public World level() {
        return level;
    }

    public float tickRate() {
        return 20.0f;
    }
}
