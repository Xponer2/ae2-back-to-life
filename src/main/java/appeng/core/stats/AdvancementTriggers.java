/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2017, AlgorithmX2, All rights reserved.
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

package appeng.core.stats;

import appeng.core.AppEng;

public class AdvancementTriggers {
    /**
     * Has a network with 8 channels
     */
    public static final PlayerTrigger NETWORK_APPRENTICE = new PlayerTrigger(AppEng.makeId("network_apprentice"));
    /**
     * Has a network with 128 channels
     */
    public static final PlayerTrigger NETWORK_ENGINEER = new PlayerTrigger(AppEng.makeId("network_engineer"));
    /**
     * Has a network with 2048 channels
     */
    public static final PlayerTrigger NETWORK_ADMIN = new PlayerTrigger(AppEng.makeId("network_admin"));
    /**
     * Entered spatial dimension
     */
    public static final PlayerTrigger SPATIAL_EXPLORER = new PlayerTrigger(AppEng.makeId("spatial_explorer"));
    /**
     * Placed a storage bus on an interface.
     */
    public static final PlayerTrigger RECURSIVE = new PlayerTrigger(AppEng.makeId("recursive_networking"));
}
