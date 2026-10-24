/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

import java.util.Objects;

import net.minecraft.util.EnumFacing;

public final class EnergyParticleData implements AEParticleOptions {

    public static final EnergyParticleData FOR_BLOCK = new EnergyParticleData(false, EnumFacing.UP);

    private final boolean forItem;
    private final EnumFacing direction;

    public EnergyParticleData(boolean forItem, EnumFacing direction) {
        this.forItem = forItem;
        this.direction = direction;
    }

    public boolean forItem() {
        return forItem;
    }

    public EnumFacing direction() {
        return direction;
    }

    @Override
    public AEParticleType<?> getType() {
        return ParticleTypes.ENERGY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnergyParticleData that)) {
            return false;
        }
        return forItem == that.forItem && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(forItem, direction);
    }

    @Override
    public String toString() {
        return "EnergyParticleData[forItem=" + forItem + ", direction=" + direction + "]";
    }
}
