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

package appeng.init.worldgen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.GameRegistry;

import appeng.worldgen.CopperOreWorldGen;
import appeng.worldgen.meteorite.MeteoriteWorldGen;

/**
 * 1.12.2 has no data driven structures: the meteorite structure set of 1.21 ({@code ae2:meteorite}) is implemented by
 * {@link MeteoriteWorldGen}, plus AE2's own copper ore which vanilla 1.12.2 doesn't have.
 */
public final class InitStructures {

    private InitStructures() {
    }

    /**
     * Registers the world generators. Call once during init.
     */
    public static void init() {
        var meteorites = new MeteoriteWorldGen();
        GameRegistry.registerWorldGenerator(meteorites, 0);
        MinecraftForge.EVENT_BUS.register(meteorites);
        GameRegistry.registerWorldGenerator(new CopperOreWorldGen(), 0);
    }
}
