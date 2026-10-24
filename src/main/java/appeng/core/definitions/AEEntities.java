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

package appeng.core.definitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.registries.IForgeRegistry;

import appeng.core.AppEng;
import appeng.entity.TinyTNTPrimedEntity;

public final class AEEntities {
    public static final Map<String, String> ENTITY_ENGLISH_NAMES = new HashMap<>();
    private static final List<EntityType<?>> TYPES = new ArrayList<>();

    public static final EntityType<TinyTNTPrimedEntity> TINY_TNT_PRIMED = create(
            "tiny_tnt_primed",
            "Tiny TNT Primed",
            TinyTNTPrimedEntity.class,
            TinyTNTPrimedEntity::new,
            16, 4, true);

    private AEEntities() {
    }

    private static <T extends Entity> EntityType<T> create(String id, String englishName, Class<T> entityClass,
            Function<World, T> factory, int trackingRange, int updateInterval, boolean velocityUpdates) {
        ENTITY_ENGLISH_NAMES.put(id, englishName);
        var type = new EntityType<>(AppEng.makeId(id), entityClass, factory, trackingRange, updateInterval,
                velocityUpdates);
        TYPES.add(type);
        return type;
    }

    public static void register(IForgeRegistry<EntityEntry> registry) {
        int networkId = 0;
        for (var type : TYPES) {
            registry.register(type.toEntry(networkId++));
        }
    }
}
