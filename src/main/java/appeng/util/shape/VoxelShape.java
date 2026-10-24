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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * Minimal replacement for Minecraft's VoxelShape on 1.12.2: an immutable union of axis aligned boxes (in block-local
 * coordinates, 0..1).
 */
public final class VoxelShape {
    static final VoxelShape EMPTY = new VoxelShape(Collections.emptyList());

    private final List<AxisAlignedBB> boxes;
    @Nullable
    private AxisAlignedBB bounds;

    VoxelShape(List<AxisAlignedBB> boxes) {
        this.boxes = boxes;
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public List<AxisAlignedBB> toAabbs() {
        return Collections.unmodifiableList(boxes);
    }

    public void forAllBoxes(Consumer<AxisAlignedBB> consumer) {
        boxes.forEach(consumer);
    }

    public AxisAlignedBB bounds() {
        if (boxes.isEmpty()) {
            throw new UnsupportedOperationException("No bounds for empty shape.");
        }
        if (bounds == null) {
            AxisAlignedBB result = boxes.get(0);
            for (int i = 1; i < boxes.size(); i++) {
                result = result.union(boxes.get(i));
            }
            bounds = result;
        }
        return bounds;
    }

    public double min(EnumFacing.Axis axis) {
        if (isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        var b = bounds();
        return switch (axis) {
            case X -> b.minX;
            case Y -> b.minY;
            case Z -> b.minZ;
        };
    }

    public double max(EnumFacing.Axis axis) {
        if (isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        var b = bounds();
        return switch (axis) {
            case X -> b.maxX;
            case Y -> b.maxY;
            case Z -> b.maxZ;
        };
    }

    public VoxelShape move(double x, double y, double z) {
        var result = new ArrayList<AxisAlignedBB>(boxes.size());
        for (var box : boxes) {
            result.add(box.offset(x, y, z));
        }
        return new VoxelShape(result);
    }

    public VoxelShape optimize() {
        return this;
    }

    /**
     * Ray-traces against all boxes of this shape positioned at the given block position.
     */
    @Nullable
    public RayTraceResult clip(Vec3d from, Vec3d to, BlockPos pos) {
        RayTraceResult best = null;
        double bestDist = Double.MAX_VALUE;
        for (var box : boxes) {
            var hit = box.offset(pos).calculateIntercept(from, to);
            if (hit != null) {
                double d = hit.hitVec.squareDistanceTo(from);
                if (d < bestDist) {
                    bestDist = d;
                    best = new RayTraceResult(hit.hitVec, hit.sideHit, pos);
                }
            }
        }
        return best;
    }

    @Override
    public String toString() {
        return "VoxelShape" + boxes;
    }
}
