/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2020, AlgorithmX2, All rights reserved.
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

package appeng.integration.modules.theoneprobe;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public class TOP {
    /**
     * Sends the registration message to The One Probe. Called during init, the messages are handled before post-init.
     */
    public static void enqueueIMC() {
        if (Loader.isModLoaded("theoneprobe")) {
            FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe",
                    "appeng.integration.modules.theoneprobe.TheOneProbeModule");
        }
    }
}
