/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 {@code PlayerTrigger}: a criterion without conditions that code grants to a player ({@code {"trigger": id}}).
 */
public class PlayerTrigger implements ICriterionTrigger<PlayerTrigger.Instance> {
    private final ResourceLocation id;
    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new HashMap<>();

    public PlayerTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public void addListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        listeners.computeIfAbsent(advancements, k -> new HashSet<>()).add(listener);
    }

    @Override
    public void removeListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        var set = listeners.get(advancements);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) {
                listeners.remove(advancements);
            }
        }
    }

    @Override
    public void removeAllListeners(PlayerAdvancements advancements) {
        listeners.remove(advancements);
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance(id);
    }

    public void trigger(EntityPlayerMP player) {
        var advancements = player.getAdvancements();
        var set = listeners.get(advancements);
        if (set != null) {
            for (var listener : new ArrayList<>(set)) {
                listener.grantCriterion(advancements);
            }
        }
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance(ResourceLocation id) {
            super(id);
        }
    }
}
