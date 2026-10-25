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

package appeng.spatial;

import java.time.Instant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;

/**
 * Defines the source level and area of a transition into the spatial storage plot.
 */
public final class TransitionInfo {

    public static final String TAG_WORLD_ID = "world_id";
    public static final String TAG_MIN = "min";
    public static final String TAG_MAX = "max";
    public static final String TAG_TIMESTAMP = "timestamp";

    /**
     * 1.12.2 identifies levels by their dimension id.
     */
    private final int dimension;

    private final BlockPos min;

    private final BlockPos max;

    private final Instant timestamp;

    public TransitionInfo(int dimension, BlockPos min, BlockPos max, Instant timestamp) {
        this.dimension = dimension;
        this.min = min.toImmutable();
        this.max = max.toImmutable();
        this.timestamp = timestamp;
    }

    public int getDimension() {
        return dimension;
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public NBTTagCompound toTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(TAG_WORLD_ID, dimension);
        tag.setTag(TAG_MIN, NBTUtil.createPosTag(min));
        tag.setTag(TAG_MAX, NBTUtil.createPosTag(max));
        tag.setLong(TAG_TIMESTAMP, timestamp.toEpochMilli());
        return tag;
    }

    public static TransitionInfo fromTag(NBTTagCompound tag) {
        int dimension = tag.getInteger(TAG_WORLD_ID);
        BlockPos min = NBTUtil.getPosFromTag(tag.getCompoundTag(TAG_MIN));
        BlockPos max = NBTUtil.getPosFromTag(tag.getCompoundTag(TAG_MAX));
        Instant timestamp = Instant.ofEpochMilli(tag.getLong(TAG_TIMESTAMP));
        return new TransitionInfo(dimension, min, max, timestamp);
    }

}
