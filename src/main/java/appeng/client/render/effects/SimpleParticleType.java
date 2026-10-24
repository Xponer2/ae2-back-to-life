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
 * A particle type without additional data, which is its own options (like the 1.21 SimpleParticleType).
 */
public final class SimpleParticleType extends AEParticleType<SimpleParticleType> implements AEParticleOptions {
    public SimpleParticleType(String name) {
        super(name);
    }

    @Override
    public SimpleParticleType getType() {
        return this;
    }
}
