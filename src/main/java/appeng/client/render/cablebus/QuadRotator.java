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

package appeng.client.render.cablebus;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.client.render.BakedQuads;

/**
 * Assuming a default-orientation of forward=NORTH and up=UP, this class rotates a given list of quads to the desired
 * facing
 */
@SideOnly(Side.CLIENT)
public class QuadRotator {

    private static final EnumMap<BlockOrientation, QuadRotator> TRANSFORMS = new EnumMap<>(
            BlockOrientation.class);

    static {
        for (BlockOrientation rotation : BlockOrientation.values()) {
            TRANSFORMS.put(rotation, new QuadRotator(rotation));
        }
    }

    public static final QuadRotator NULL_TRANSFORM = TRANSFORMS.get(BlockOrientation.NORTH_UP);

    private final BlockOrientation rotation;

    private QuadRotator(BlockOrientation rotation) {
        this.rotation = rotation;
    }

    public static QuadRotator get(EnumFacing facing, int spin) {
        return get(BlockOrientation.get(facing, spin));
    }

    public static QuadRotator get(BlockOrientation rotation) {
        return TRANSFORMS.get(rotation);
    }

    public boolean isRedundant() {
        return rotation.isRedundant();
    }

    public BakedQuad transform(BakedQuad quad) {
        return BakedQuads.rotate(quad, rotation);
    }

    public List<BakedQuad> transform(List<BakedQuad> quads) {
        return BakedQuads.rotate(quads, rotation);
    }
}
