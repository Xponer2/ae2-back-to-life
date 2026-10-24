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

package appeng.client.render.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

// TODO: Investigate use of CubeBuilder instead
@SideOnly(Side.CLIENT)
final class RenderHelper {

    // Corners as {x, y, z}
    private static EnumMap<EnumFacing, List<float[]>> cornersForFacing = generateCornersForFacings();

    private RenderHelper() {

    }

    static List<float[]> getFaceCorners(EnumFacing side) {
        return cornersForFacing.get(side);
    }

    private static EnumMap<EnumFacing, List<float[]>> generateCornersForFacings() {
        EnumMap<EnumFacing, List<float[]>> result = new EnumMap<>(EnumFacing.class);

        for (EnumFacing facing : EnumFacing.values()) {
            List<float[]> corners;

            float offset = facing.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? 0 : 1;

            corners = switch (facing.getAxis()) {
                case X -> Arrays.asList(new float[] { offset, 1, 1 }, new float[] { offset, 0, 1 },
                        new float[] { offset, 0, 0 }, new float[] { offset, 1, 0 });
                case Y -> Arrays.asList(new float[] { 1, offset, 1 }, new float[] { 1, offset, 0 },
                        new float[] { 0, offset, 0 }, new float[] { 0, offset, 1 });
                case Z -> Arrays.asList(new float[] { 0, 1, offset }, new float[] { 0, 0, offset },
                        new float[] { 1, 0, offset }, new float[] { 1, 1, offset });
            };

            if (facing.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE) {
                Collections.reverse(corners);
            }

            result.put(facing, Collections.unmodifiableList(corners));
        }

        return result;
    }

}
