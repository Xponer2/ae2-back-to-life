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

package appeng.spatial;

import java.util.HashSet;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

import appeng.core.AEConfig;
import appeng.core.AppEng;

/**
 * The spatial storage dimension. 1.21 defines it with data driven ids ({@code ae2:spatial_storage}); 1.12.2 needs a
 * numeric dimension id and dimension type id, which are picked once and stored in the config.
 */
public final class SpatialStorageDimensionIds {

    /**
     * Id of the 1.21 level; also the name of the 1.12.2 dimension type.
     */
    public static final ResourceLocation WORLD_ID = AppEng.makeId("spatial_storage");

    /**
     * Id of the {@link SpatialStorageBiome}.
     */
    public static final ResourceLocation BIOME_KEY = AppEng.makeId("spatial_storage");

    private static DimensionType dimensionType;
    private static int dimensionId;

    private SpatialStorageDimensionIds() {
    }

    /**
     * Registers the dimension type and the dimension. Call once in pre-init, after the config was loaded.
     */
    public static void register() {
        var config = AEConfig.instance();

        if (config.getSpatialDimensionTypeId() == -1) {
            var used = new HashSet<Integer>();
            for (DimensionType type : DimensionType.values()) {
                used.add(type.getId());
            }
            int id = -11;
            while (used.contains(id)) {
                --id;
            }
            config.setSpatialDimensionTypeId(id);
            config.save();
        }
        dimensionType = DimensionType.register(WORLD_ID.toString(), "_" + WORLD_ID.getPath(),
                config.getSpatialDimensionTypeId(), SpatialStorageWorldProvider.class, false);

        if (config.getSpatialDimensionId() == -1) {
            config.setSpatialDimensionId(DimensionManager.getNextFreeDimId());
            config.save();
        }
        dimensionId = config.getSpatialDimensionId();
        DimensionManager.registerDimension(dimensionId, dimensionType);
    }

    public static DimensionType getDimensionType() {
        return dimensionType;
    }

    public static int getDimensionId() {
        return dimensionId;
    }

}
