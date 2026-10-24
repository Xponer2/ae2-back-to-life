/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.worldgen.meteorite;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import appeng.core.worlddata.AESavedData;

/**
 * Per dimension record of the meteorites (1.21 keeps them as structure starts in the chunks): for every potential
 * start chunk that was decided, the meteorite settings (or none) and the chunks its blocks were already placed in.
 */
public final class MeteoriteSpawnData extends AESavedData {
    private static final String NAME = "ae2_meteorites";

    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();

    /**
     * Used by the 1.12.2 map storage to load the data (needs a public constructor taking the name).
     */
    public MeteoriteSpawnData(String name) {
        super(name);
    }

    public static MeteoriteSpawnData get(World level) {
        return getOrCreatePerWorld(level, MeteoriteSpawnData.class, NAME, MeteoriteSpawnData::new);
    }

    public static final class Entry {
        private final ChunkPos start;
        @Nullable
        private final PlacedMeteoriteSettings settings;
        private final LongOpenHashSet placedChunks = new LongOpenHashSet();

        Entry(ChunkPos start, @Nullable PlacedMeteoriteSettings settings) {
            this.start = start;
            this.settings = settings;
        }

        public ChunkPos getStart() {
            return start;
        }

        /**
         * @return null if no meteorite spawns at this start chunk
         */
        @Nullable
        public PlacedMeteoriteSettings getSettings() {
            return settings;
        }

        public boolean isPlaced(ChunkPos chunk) {
            return placedChunks.contains(ChunkPos.asLong(chunk.x, chunk.z));
        }
    }

    @Nullable
    public Entry get(ChunkPos start) {
        return entries.get(ChunkPos.asLong(start.x, start.z));
    }

    public Entry add(ChunkPos start, @Nullable PlacedMeteoriteSettings settings) {
        var entry = new Entry(start, settings);
        entries.put(ChunkPos.asLong(start.x, start.z), entry);
        setDirty();
        return entry;
    }

    public void markPlaced(Entry entry, ChunkPos chunk) {
        if (entry.placedChunks.add(ChunkPos.asLong(chunk.x, chunk.z))) {
            setDirty();
        }
    }

    /**
     * All meteorites whose start chunk is within the given distance (in chunks) of a chunk.
     */
    public List<PlacedMeteoriteSettings> getMeteoritesNear(ChunkPos chunk, int radius) {
        var result = new ArrayList<PlacedMeteoriteSettings>();
        for (var entry : entries.values()) {
            if (entry.settings != null && Math.abs(entry.start.x - chunk.x) <= radius
                    && Math.abs(entry.start.z - chunk.z) <= radius) {
                result.add(entry.settings);
            }
        }
        return result;
    }

    @Override
    public void load(NBTTagCompound tag) {
        entries.clear();
        var list = tag.getTagList("meteorites", NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            var entryTag = list.getCompoundTagAt(i);
            var start = new ChunkPos(entryTag.getInteger("x"), entryTag.getInteger("z"));
            PlacedMeteoriteSettings settings = null;
            if (entryTag.hasKey("settings", NBT.TAG_COMPOUND)) {
                settings = PlacedMeteoriteSettings.read(entryTag.getCompoundTag("settings"));
            }
            var entry = new Entry(start, settings);
            for (long chunk : getLongs(entryTag, "placed")) {
                entry.placedChunks.add(chunk);
            }
            entries.put(ChunkPos.asLong(start.x, start.z), entry);
        }
    }

    @Override
    public NBTTagCompound save(NBTTagCompound tag) {
        var list = new NBTTagList();
        for (var entry : entries.values()) {
            var entryTag = new NBTTagCompound();
            entryTag.setInteger("x", entry.start.x);
            entryTag.setInteger("z", entry.start.z);
            if (entry.settings != null) {
                entryTag.setTag("settings", entry.settings.write(new NBTTagCompound()));
                var placed = new NBTTagList();
                for (long chunk : entry.placedChunks) {
                    var chunkTag = new NBTTagCompound();
                    chunkTag.setLong("c", chunk);
                    placed.appendTag(chunkTag);
                }
                entryTag.setTag("placed", placed);
            }
            list.appendTag(entryTag);
        }
        tag.setTag("meteorites", list);
        return tag;
    }

    private static long[] getLongs(NBTTagCompound tag, String key) {
        var list = tag.getTagList(key, NBT.TAG_COMPOUND);
        var result = new long[list.tagCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.getCompoundTagAt(i).getLong("c");
        }
        return result;
    }
}
