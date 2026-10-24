/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.server.services.compass;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import appeng.blockentity.misc.MysteriousCubeBlockEntity;
import appeng.core.definitions.AEBlocks;

public final class ServerCompassService {

    /**
     * Maximum distance to search in chunks.
     */
    private static final int MAX_RANGE = 174;
    private static final int CHUNK_SIZE = 16;

    private final static class Query {
        private final WorldServer level;
        private final ChunkPos chunk;

        public Query(WorldServer level, ChunkPos chunk) {
            this.level = level;
            this.chunk = chunk;
        }

        public WorldServer level() {
            return level;
        }

        public ChunkPos chunk() {
            return chunk;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Query)) return false;
            Query that = (Query) o;
            return java.util.Objects.equals(level, that.level)
                    && java.util.Objects.equals(chunk, that.chunk);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(level, chunk);
        }

        @Override
        public String toString() {
            return "Query[" + "level=" + level + ", " + "chunk=" + chunk + "]";
        }

    }

    // We use this basic cache to prevent client-side spamming, although the client can request arbitrary
    // chunk positions and range, if malicious.
    private static final LoadingCache<Query, Optional<BlockPos>> CLOSEST_METEORITE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .weakKeys()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Optional<BlockPos> load(ServerCompassService.Query query) {
                    return Optional.ofNullable(findClosestMeteoritePos(query.level, query.chunk));
                }
            });

    public static Optional<BlockPos> getClosestMeteorite(WorldServer level, ChunkPos chunkPos) {
        return CLOSEST_METEORITE_CACHE.getUnchecked(new Query(level, chunkPos));
    }

    @Nullable
    private static BlockPos findClosestMeteoritePos(WorldServer level, ChunkPos originChunkPos) {
        var chunkPos = findClosestMeteoriteChunk(level, originChunkPos);
        if (chunkPos == null) {
            return null;
        }
        var chunk = level.getChunkProvider().getLoadedChunk(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            // Do not chunk-load a chunk to check for a precise block position
            return middleOf(chunkPos);
        }

        // Find the closest BE in the chunk. Usually it will only be one.
        var sourcePos = middleOf(originChunkPos);
        var closestDistanceSq = Double.MAX_VALUE;
        BlockPos chosenPos = middleOf(chunkPos);
        for (var blockEntity : chunk.getTileEntityMap().values()) {
            if (blockEntity instanceof MysteriousCubeBlockEntity) {
                var bePos = new BlockPos(blockEntity.getPos().getX(), 0, blockEntity.getPos().getZ());
                var distSq = sourcePos.distanceSq(bePos);
                if (distSq < closestDistanceSq) {
                    chosenPos = bePos;
                    closestDistanceSq = distSq;
                }
            }
        }
        return chosenPos;
    }

    @Nullable
    private static ChunkPos findClosestMeteoriteChunk(WorldServer level, ChunkPos chunkPos) {
        var cr = CompassRegion.get(level, chunkPos);
        var cx = chunkPos.x;
        var cz = chunkPos.z;

        // Am I standing on it?
        if (cr.hasCompassTarget(cx, cz)) {
            return chunkPos;
        }

        // spiral outward...
        for (int offset = 1; offset < MAX_RANGE; offset++) {
            final int minX = cx - offset;
            final int minZ = cz - offset;
            final int maxX = cx + offset;
            final int maxZ = cz + offset;

            int closest = Integer.MAX_VALUE;
            int chosen_x = cx;
            int chosen_z = cz;

            for (int z = minZ; z <= maxZ; z++) {
                if (cr.hasCompassTarget(minX, z)) {
                    final int closeness = dist(cx, cz, minX, z);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = minX;
                        chosen_z = z;
                    }
                }

                if (cr.hasCompassTarget(maxX, z)) {
                    final int closeness = dist(cx, cz, maxX, z);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = maxX;
                        chosen_z = z;
                    }
                }
            }

            for (int x = minX + 1; x < maxX; x++) {
                if (cr.hasCompassTarget(x, minZ)) {
                    final int closeness = dist(cx, cz, x, minZ);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = x;
                        chosen_z = minZ;
                    }
                }

                if (cr.hasCompassTarget(x, maxZ)) {
                    final int closeness = dist(cx, cz, x, maxZ);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = x;
                        chosen_z = maxZ;
                    }
                }
            }

            if (closest < Integer.MAX_VALUE) {
                return new ChunkPos(chosen_x, chosen_z);
            }
        }

        // didn't find shit...
        return null;
    }

    public static void updateArea(WorldServer level, Chunk chunk) {
        var compassRegion = CompassRegion.get(level, chunk.getPos());

        for (var i = 0; i < chunk.getBlockStorageArray().length; i++) {
            updateArea(compassRegion, chunk, i);
        }
    }

    /**
     * 1.21 {@code ChunkPos.getMiddleBlockPosition(0)}.
     */
    private static BlockPos middleOf(ChunkPos chunkPos) {
        return new BlockPos(chunkPos.getXStart() + 7, 0, chunkPos.getZStart() + 7);
    }

    /**
     * Notifies the compass service that a skystone block has either been placed or replaced at the give position.
     */
    public static void notifyBlockChange(WorldServer level, BlockPos pos) {
        Chunk chunk = level.getChunk(pos);
        var compassRegion = CompassRegion.get(level, chunk.getPos());
        updateArea(compassRegion, chunk, pos.getY() >> 4);
    }

    private static void updateArea(CompassRegion compassRegion, Chunk chunk, int sectionIndex) {
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;

        if (sectionIndex < 0 || sectionIndex >= chunk.getBlockStorageArray().length) {
            return;
        }
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[sectionIndex];
        if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
            compassRegion.setHasCompassTarget(cx, cz, sectionIndex, false);
            return;
        }

        // Look for a mysterious cube (1.21 counts the states in the section palette, which 1.12.2 doesn't expose)
        var cube = AEBlocks.MYSTERIOUS_CUBE.block();
        boolean found = false;
        search: for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (section.get(x, y, z).getBlock() == cube) {
                        found = true;
                        break search;
                    }
                }
            }
        }
        compassRegion.setHasCompassTarget(cx, cz, sectionIndex, found);
    }

    private static int dist(int ax, int az, int bx, int bz) {
        final int up = (bz - az) * CHUNK_SIZE;
        final int side = (bx - ax) * CHUNK_SIZE;

        return up * up + side * side;
    }

}
