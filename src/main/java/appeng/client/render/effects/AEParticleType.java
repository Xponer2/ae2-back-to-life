/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.effects;

/**
 * Identifies a kind of AE particle. 1.12.2 has no particle type registry, the client maps these to factories in
 * {@link AEParticles}.
 *
 * @param <T> The options that particles of this type are created from.
 */
public class AEParticleType<T extends AEParticleOptions> {
    private final String name;

    public AEParticleType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ae2:" + name;
    }
}
