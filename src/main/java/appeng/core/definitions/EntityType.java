/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.definitions;

import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Stand-in for Minecraft's EntityType: 1.12.2 entities are registered as {@link EntityEntry}.
 */
public final class EntityType<T extends Entity> {
    private final ResourceLocation id;
    private final Class<T> entityClass;
    private final Function<World, T> factory;
    private final int trackingRange;
    private final int updateInterval;
    private final boolean sendsVelocityUpdates;

    public EntityType(ResourceLocation id, Class<T> entityClass, Function<World, T> factory, int trackingRange,
            int updateInterval, boolean sendsVelocityUpdates) {
        this.id = id;
        this.entityClass = entityClass;
        this.factory = factory;
        this.trackingRange = trackingRange;
        this.updateInterval = updateInterval;
        this.sendsVelocityUpdates = sendsVelocityUpdates;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public T create(World level) {
        return factory.apply(level);
    }

    public EntityEntry toEntry(int networkId) {
        return EntityEntryBuilder.<T>create()
                .entity(entityClass)
                .id(id, networkId)
                .name(id.getNamespace() + "." + id.getPath())
                .factory(factory)
                .tracker(trackingRange * 16, updateInterval, sendsVelocityUpdates)
                .build();
    }
}
