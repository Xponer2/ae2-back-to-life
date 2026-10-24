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

package appeng.client.render.effects;

import net.minecraft.util.math.Vec3d;

/**
 * Contains the target point of the lightning arc (the source point is inferred from the particle starting position).
 */
public final class LightningArcParticleData implements AEParticleOptions {

    private final Vec3d target;

    public LightningArcParticleData(Vec3d target) {
        this.target = target;
    }

    public LightningArcParticleData(double x, double y, double z) {
        this(new Vec3d(x, y, z));
    }

    public Vec3d target() {
        return target;
    }

    @Override
    public AEParticleType<?> getType() {
        return ParticleTypes.LIGHTNING_ARC;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof LightningArcParticleData that && target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return "LightningArcParticleData[target=" + target + "]";
    }
}
