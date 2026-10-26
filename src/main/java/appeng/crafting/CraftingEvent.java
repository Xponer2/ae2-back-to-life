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

package appeng.crafting;


import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import appeng.api.crafting.IPatternDetails;
import appeng.util.Platform;

public class CraftingEvent {

    public static void fireAutoCraftingEvent(World level,
            // NOTE: We want to be able to include the recipe in the event later
            @SuppressWarnings("unused") IPatternDetails pattern,
            ItemStack craftedItem,
            IInventory container) {
        var serverLevel = (WorldServer) level;
        var fakePlayer = Platform.getFakePlayer(serverLevel, null);
        FMLCommonHandler.instance().firePlayerCraftingEvent(fakePlayer, craftedItem, container);
    }

}
