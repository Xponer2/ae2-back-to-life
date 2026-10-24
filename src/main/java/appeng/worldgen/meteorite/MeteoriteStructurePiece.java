/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2020, AlgorithmX2, All rights reserved.
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
package appeng.worldgen.meteorite;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/**
 * 1.12.2 has no structure pieces: this only keeps the settings of a meteorite together with the area it is placed in
 * (same bounding box as the 1.21 piece). The blocks are placed by {@link MeteoriteWorldGen}.
 */
public class MeteoriteStructurePiece {

    private final PlacedMeteoriteSettings settings;
    private final StructureBoundingBox boundingBox;

    public MeteoriteStructurePiece(PlacedMeteoriteSettings settings) {
        this.settings = settings;
        this.boundingBox = createBoundingBox(settings.getPos());
    }

    private static StructureBoundingBox createBoundingBox(BlockPos origin) {
        int range = MeteoriteStructure.RANGE_CHUNKS * 16;

        ChunkPos chunkPos = new ChunkPos(origin);

        return new StructureBoundingBox(chunkPos.getXStart() - range, origin.getY(),
                chunkPos.getZStart() - range, chunkPos.getXEnd() + range, origin.getY(),
                chunkPos.getZEnd() + range);
    }

    public boolean isFinalized() {
        return settings.getCraterType() != null;
    }

    public PlacedMeteoriteSettings getSettings() {
        return settings;
    }

    public StructureBoundingBox getBoundingBox() {
        return boundingBox;
    }
}
