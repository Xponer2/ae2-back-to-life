/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util.shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.math.AxisAlignedBB;

public final class Shapes {
    private static final VoxelShape BLOCK = new VoxelShape(
            Collections.singletonList(new AxisAlignedBB(0, 0, 0, 1, 1, 1)));

    private Shapes() {
    }

    public static VoxelShape empty() {
        return VoxelShape.EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK;
    }

    public static VoxelShape create(AxisAlignedBB box) {
        return new VoxelShape(Collections.singletonList(box));
    }

    public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return create(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return create(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Same as Minecraft's {@code Block.box}: coordinates in 1/16th of a block.
     */
    public static VoxelShape box16(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return create(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    public static VoxelShape or(VoxelShape first, VoxelShape... others) {
        var boxes = new ArrayList<>(first.toAabbs());
        for (var other : others) {
            boxes.addAll(other.toAabbs());
        }
        return new VoxelShape(boxes);
    }

    public static VoxelShape or(Collection<VoxelShape> shapes) {
        var boxes = new ArrayList<AxisAlignedBB>();
        for (var s : shapes) {
            boxes.addAll(s.toAabbs());
        }
        return boxes.isEmpty() ? VoxelShape.EMPTY : new VoxelShape(boxes);
    }

    public static VoxelShape of(List<AxisAlignedBB> boxes) {
        return boxes.isEmpty() ? VoxelShape.EMPTY : new VoxelShape(new ArrayList<>(boxes));
    }
}
