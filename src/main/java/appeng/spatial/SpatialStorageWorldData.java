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

import java.util.List;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import appeng.core.AELog;
import appeng.core.worlddata.AESavedData;

/**
 * Extra data attached to the spatial storage level.
 */
public class SpatialStorageWorldData extends AESavedData {

    /**
     * ID of this data when it is attached to a level.
     */
    public static final String ID = "ae2_spatial_storage";

    // Used to allow forward compatibility
    private static final int CURRENT_FORMAT = 2;

    private static final String TAG_FORMAT = "format";

    private static final String TAG_PLOTS = "plots";

    private final Int2ObjectOpenHashMap<SpatialStoragePlot> plots = new Int2ObjectOpenHashMap<>();

    /**
     * Used by the 1.12.2 map storage to load the data (needs a public constructor taking the name).
     */
    public SpatialStorageWorldData(String name) {
        super(name);
    }

    public SpatialStoragePlot getPlotById(int id) {
        return plots.get(id);
    }

    public List<SpatialStoragePlot> getPlots() {
        return ImmutableList.copyOf(plots.values());
    }

    public SpatialStoragePlot allocatePlot(BlockPos size, int owner) {

        int nextId = 1;
        for (int id : plots.keySet()) {
            if (id >= nextId) {
                nextId = id + 1;
            }
        }

        SpatialStoragePlot plot = new SpatialStoragePlot(nextId, size, owner);
        plots.put(nextId, plot);
        setDirty();
        return plot;
    }

    public void removePlot(int plotId) {
        plots.remove(plotId);
        setDirty();
    }

    public void setLastTransition(int plotId, TransitionInfo info) {
        SpatialStoragePlot plot = plots.get(plotId);
        if (plot != null) {
            plot.setLastTransition(info);
        }
        setDirty();
    }

    @Override
    public void load(NBTTagCompound tag) {
        var result = this;
        result.plots.clear();
        int version = tag.getInteger(TAG_FORMAT);
        if (version != CURRENT_FORMAT) {
            // Currently no new format has been defined, as such anything but the current
            // version is invalid
            throw new IllegalStateException("Invalid AE2 spatial info version: " + version);
        }

        NBTTagList plotsTag = tag.getTagList(TAG_PLOTS, Constants.NBT.TAG_COMPOUND);
        for (NBTBase plotTag : plotsTag) {
            SpatialStoragePlot plot = SpatialStoragePlot.fromTag((NBTTagCompound) plotTag);

            if (result.plots.containsKey(plot.getId())) {
                AELog.warn("Overwriting duplicate plot id %s", plot.getId());
            }
            result.plots.put(plot.getId(), plot);
        }
    }

    @Override
    public NBTTagCompound save(NBTTagCompound tag) {
        tag.setInteger(TAG_FORMAT, CURRENT_FORMAT);

        NBTTagList plotTags = new NBTTagList();
        for (SpatialStoragePlot plot : plots.values()) {
            plotTags.appendTag(plot.toTag());
        }
        tag.setTag(TAG_PLOTS, plotTags);

        return tag;
    }

}
