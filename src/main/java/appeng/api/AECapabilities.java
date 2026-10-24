/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.EnumFacing;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.capabilities.BlockCapability;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import appeng.core.AppEng;

/**
 * Utility class that holds the capabilities provided by AE2.
 */
public final class AECapabilities {
    private AECapabilities() {
    }

    public static BlockCapability<MEStorage, @Nullable EnumFacing> ME_STORAGE = BlockCapability
            .createSided(AppEng.makeId("me_storage"), MEStorage.class);

    public static BlockCapability<ICraftingMachine, @Nullable EnumFacing> CRAFTING_MACHINE = BlockCapability
            .createSided(AppEng.makeId("crafting_machine"), ICraftingMachine.class);

    public static BlockCapability<GenericInternalInventory, @Nullable EnumFacing> GENERIC_INTERNAL_INV = BlockCapability
            .createSided(AppEng.makeId("generic_internal_inv"), GenericInternalInventory.class);

    public static BlockCapability<IInWorldGridNodeHost, Void> IN_WORLD_GRID_NODE_HOST = BlockCapability
            .createVoid(AppEng.makeId("inworld_gridnode_host"), IInWorldGridNodeHost.class);

    public static BlockCapability<ICrankable, @Nullable EnumFacing> CRANKABLE = BlockCapability
            .createSided(AppEng.makeId("crankable"), ICrankable.class);

}
