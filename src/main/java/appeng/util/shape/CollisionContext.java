/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util.shape;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;

/**
 * Stand-in for Minecraft's CollisionContext.
 */
public final class CollisionContext {
    private static final CollisionContext EMPTY = new CollisionContext(null);

    @Nullable
    private final Entity entity;

    private CollisionContext(@Nullable Entity entity) {
        this.entity = entity;
    }

    public static CollisionContext empty() {
        return EMPTY;
    }

    public static CollisionContext of(@Nullable Entity entity) {
        return entity == null ? EMPTY : new CollisionContext(entity);
    }

    @Nullable
    public Entity getEntity() {
        return entity;
    }
}
