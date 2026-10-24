/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.worldgen.meteorite.fallout;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

/**
 * The 1.21 biome tags are mapped to the Forge biome dictionary types of 1.12.2.
 */
public enum FalloutMode {

    /**
     * No fallout, e.g. when without a crater.
     */
    NONE,

    /**
     * Default
     */
    DEFAULT,

    /**
     * For sandy terrain (c:is_sandy, minecraft:is_beach)
     */
    SAND(Type.SANDY, Type.BEACH),

    /**
     * For terracotta (mesa, minecraft:is_badlands)
     */
    TERRACOTTA(Type.MESA),

    /**
     * Icy/snowy terrain (c:is_cold)
     */
    ICE_SNOW(Type.COLD);

    private final List<Type> biomeTypes;

    FalloutMode(Type... biomeTypes) {
        this.biomeTypes = ImmutableList.copyOf(biomeTypes);
    }

    public boolean matches(Biome biome) {
        for (var type : biomeTypes) {
            if (BiomeDictionary.hasType(biome, type)) {
                return true;
            }
        }
        return false;
    }

    public static FalloutMode fromBiome(Biome biome) {
        for (var mode : FalloutMode.values()) {
            if (mode.matches(biome)) {
                return mode;
            }
        }

        return DEFAULT;
    }
}
