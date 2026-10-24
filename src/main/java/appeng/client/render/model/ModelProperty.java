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

import java.util.function.Predicate;

import com.google.common.base.Predicates;

/**
 * Key of a value in {@link ModelData}. Replacement for NeoForge's ModelProperty, compared by identity.
 * <p/>
 * Not client-only: block entities and parts build their model data on both sides.
 */
public class ModelProperty<T> implements Predicate<T> {
    private final Predicate<T> predicate;

    public ModelProperty() {
        this(Predicates.alwaysTrue());
    }

    public ModelProperty(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(T value) {
        return predicate.test(value);
    }
}
