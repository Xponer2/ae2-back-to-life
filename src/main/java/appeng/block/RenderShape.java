/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.block;

import net.minecraft.util.EnumBlockRenderType;

/**
 * Backport of Minecraft's RenderShape.
 */
public enum RenderShape {
    INVISIBLE(EnumBlockRenderType.INVISIBLE),
    ENTITYBLOCK_ANIMATED(EnumBlockRenderType.ENTITYBLOCK_ANIMATED),
    MODEL(EnumBlockRenderType.MODEL);

    private final EnumBlockRenderType type;

    RenderShape(EnumBlockRenderType type) {
        this.type = type;
    }

    public EnumBlockRenderType toVanilla() {
        return type;
    }
}
