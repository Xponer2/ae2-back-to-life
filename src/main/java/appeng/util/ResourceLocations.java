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

import net.minecraft.util.ResourceLocation;

/**
 * Parses ids of the form {@code namespace:path} like newer versions do. The parsing of this game version only accepts
 * namespaces with at least two characters: {@code new ResourceLocation("c:ingots/iron")} is
 * {@code minecraft:ingots/iron}, which breaks every reference to the common {@code c:} tags.
 */
public final class ResourceLocations {
    private ResourceLocations() {
    }

    public static ResourceLocation parse(String id) {
        if (id.indexOf(':') == 1) {
            return new ResourceLocation(id.substring(0, 1), id.substring(2));
        }
        return new ResourceLocation(id);
    }
}
