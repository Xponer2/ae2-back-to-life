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

package appeng.core;

import net.minecraft.world.World;

import appeng.client.EffectType;

/**
 * Dedicated server side of AE2 (selected by FML's sided proxy).
 */
public class AppEngServer extends AppEngBase {
    public AppEngServer() {
    }

    @Override
    public World getClientLevel() {
        return null;
    }

    @Override
    public void registerHotkey(String id) {
        // don't register any Hotkeys
    }

    @Override
    public void spawnEffect(EffectType effect, World level, double posX, double posY, double posZ, Object o) {
        // Spawning client-side effects on a server is impossible
    }
}
