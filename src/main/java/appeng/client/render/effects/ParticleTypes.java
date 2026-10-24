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

/**
 * The AE particle types. In 1.12.2 these are plain identifiers (there is no particle registry), spawned on the client
 * with {@link AEParticles}.
 */
public final class ParticleTypes {

    private ParticleTypes() {
    }

    public static final SimpleParticleType CRAFTING = new SimpleParticleType("crafting_fx");
    public static final AEParticleType<EnergyParticleData> ENERGY = new AEParticleType<>("energy_fx");
    public static final AEParticleType<LightningArcParticleData> LIGHTNING_ARC = new AEParticleType<>(
            "lightning_arc_fx");
    public static final SimpleParticleType LIGHTNING = new SimpleParticleType("lightning_fx");
    public static final SimpleParticleType MATTER_CANNON = new SimpleParticleType("matter_cannon_fx");
    public static final SimpleParticleType VIBRANT = new SimpleParticleType("vibrant_fx");

}
