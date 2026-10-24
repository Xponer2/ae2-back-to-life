/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Immutable map of {@link ModelProperty} to values, the 1.12.2 replacement for NeoForge's ModelData.
 * <p/>
 * In 1.12.2 it reaches the baked models as the {@link AEModelData#MODEL_DATA} unlisted property of the extended block
 * state. Implements equals/hashCode by value so that it can be part of model cache keys.
 */
public final class ModelData {
    public static final ModelData EMPTY = new ModelData(Collections.emptyMap());

    private final Map<ModelProperty<?>, Object> properties;

    private ModelData(Map<ModelProperty<?>, Object> properties) {
        this.properties = properties;
    }

    public Set<ModelProperty<?>> getProperties() {
        return properties.keySet();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(ModelProperty<T> prop) {
        return (T) properties.get(prop);
    }

    public boolean has(ModelProperty<?> prop) {
        return properties.containsKey(prop);
    }

    public Builder derive() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return properties.equals(((ModelData) o).properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public String toString() {
        return "ModelData" + properties;
    }

    public static final class Builder {
        private final Map<ModelProperty<?>, Object> properties = new HashMap<>();

        private Builder(@Nullable ModelData parent) {
            if (parent != null) {
                properties.putAll(parent.properties);
            }
        }

        public <T> Builder with(ModelProperty<T> property, T value) {
            Preconditions.checkState(property.test(value), "The provided value is invalid for this property.");
            properties.put(property, value);
            return this;
        }

        public ModelData build() {
            if (properties.isEmpty()) {
                return EMPTY;
            }
            return new ModelData(Collections.unmodifiableMap(new HashMap<>(properties)));
        }
    }
}
