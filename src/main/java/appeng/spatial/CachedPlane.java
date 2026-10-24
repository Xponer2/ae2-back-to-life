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

package appeng.spatial;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

import appeng.api.ids.AETags;
import appeng.api.movable.BlockEntityMoveStrategies;
import appeng.api.movable.IBlockEntityMoveStrategy;
import appeng.core.AELog;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.BlockEntityType;
import appeng.server.services.compass.ServerCompassService;
import appeng.util.Platform;

public class CachedPlane {
    private final int x_size;
    private final int z_size;
    private final int cx_size;
    private final int cz_size;
    private final int x_offset;
    private final int y_offset;
    private final int z_offset;
    private final int y_size;
    private final Chunk[][] myChunks;
    private final Column[][] myColumns;
    private final List<BlockEntityMoveRecord> blockEntities = new ArrayList<>();
    private final List<ScheduledTick<Block>> ticks = new ArrayList<>();
    private final WorldServer level;
    private final List<BlockPos> updates = new ArrayList<>();
    private final IBlockState matrixBlockState;
    private final List<PoiMoveRecord> poiMoveRecords = new ArrayList<>();

    public CachedPlane(WorldServer level, int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {

        Block matrixFrameBlock = AEBlocks.MATRIX_FRAME.block();
        if (matrixFrameBlock != null) {
            this.matrixBlockState = matrixFrameBlock.getDefaultState();
        } else {
            this.matrixBlockState = null;
        }

        this.level = level;

        this.x_size = maxX - minX + 1;
        this.y_size = maxY - minY + 1;
        this.z_size = maxZ - minZ + 1;

        this.x_offset = minX;
        this.y_offset = minY;
        this.z_offset = minZ;

        final int minCX = minX >> 4;
        final int minCY = minY >> 4;
        final int minCZ = minZ >> 4;
        final int maxCX = maxX >> 4;
        final int maxCY = maxY >> 4;
        final int maxCZ = maxZ >> 4;

        this.cx_size = maxCX - minCX + 1;
        final int cy_size = maxCY - minCY + 1;
        this.cz_size = maxCZ - minCZ + 1;

        this.myChunks = new Chunk[this.cx_size][this.cz_size];
        this.myColumns = new Column[this.x_size][this.z_size];

        for (int x = 0; x < this.x_size; x++) {
            for (int z = 0; z < this.z_size; z++) {
                this.myColumns[x][z] = new Column(level.getChunk(minX + x >> 4, minZ + z >> 4), minX + x & 0xF,
                        minZ + z & 0xF);
            }
        }

        for (int cx = 0; cx < this.cx_size; cx++) {
            for (int cz = 0; cz < this.cz_size; cz++) {
                final Chunk c = level.getChunk(minCX + cx, minCZ + cz);
                this.myChunks[cx][cz] = c;

                // Make a copy of the BE list in the chunk. This allows us to immediately remove BE's we're moving.
                // We cannot simply copy the map entries since the map may reuse those if we remove entries.
                var chunkBlockEntities = c.getBlockEntities().entrySet();
                var blockEntities = new ArrayList<TileEntity>(chunkBlockEntities.size());
                for (var entity : chunkBlockEntities) {
                    var pos = entity.getKey();
                    if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                            && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                        blockEntities.add(entity.getValue());
                    }
                }

                for (var blockEntity : blockEntities) {
                    var pos = blockEntity.getPos();

                    // If the block entities containing block is blacklisted, it will be skipped
                    // automatically later, so we have to avoid removing it here
                    if (AETags.SPATIAL_BLACKLIST.contains(Platform.getBlockState(blockEntity))) {
                        continue;
                    }

                    var strategy = BlockEntityMoveStrategies.get(blockEntity);
                    var savedData = strategy.beginMove(blockEntity);
                    var section = c.getSection(c.getSectionIndex(pos.getY()));

                    // Coordinate within the section
                    int sx = pos.getX() & (LevelChunkSection.SECTION_WIDTH - 1);
                    int sy = pos.getY() & (LevelChunkSection.SECTION_HEIGHT - 1);
                    int sz = pos.getZ() & (LevelChunkSection.SECTION_WIDTH - 1);
                    var state = section.getBlockState(sx, sy, sz);

                    if (savedData != null) {
                        this.blockEntities.add(
                                new BlockEntityMoveRecord(strategy, blockEntity, savedData, pos, state));

                        // Set the state to AIR now since that prevents it from being resurrected recursively
                        section.setBlockState(sx, sy, sz, Blocks.AIR.getDefaultState());
                        c.removeBlockEntity(pos);
                    } else {
                        // don't skip air, just let the code replace it...
                        if (state.isAir()) {
                            level.removeBlock(pos, false);
                        } else {
                            this.myColumns[pos.getX() - minX][pos.getZ() - minZ].setSkip(pos.getY());
                        }
                    }
                }

                var pending = (LevelChunkTicks<Block>) c.getBlockTicks();
                pending.getAll().forEach(entry -> {
                    var pos = entry.pos();
                    if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                            && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                        this.ticks.add(entry);
                    }
                });

                // Find any Villager point of interests that are within this section, remove them and queue
                // re-adding them to the destination locations
                for (int cy = 0; cy < cy_size; ++cy) {
                    var poiSection = level.getPoiManager()
                            .getOrLoad(SectionPos.of(c.getPos(), minCY + cy).toLong())
                            .orElse(null);
                    if (poiSection == null) {
                        continue;
                    }
                    var poiRecords = poiSection
                            .getRecords(poiType -> true, PoiManager.Occupancy.ANY)
                            .iterator();
                    while (poiRecords.hasNext()) {
                        var poiRecord = poiRecords.next();
                        var pos = poiRecord.getPos();
                        if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                                && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                            this.poiMoveRecords.add(new PoiMoveRecord(
                                    poiRecord.getPos().offset(-x_offset, -y_offset, -z_offset),
                                    poiRecord.getPoiType()));
                            poiSection.remove(poiRecord.getPos());
                        }
                    }
                }
            }
        }

    }

    void swap(CachedPlane dst) {
        if (dst.x_size == this.x_size && dst.y_size == this.y_size && dst.z_size == this.z_size) {
            AELog.info("Block Copy Scale: " + this.x_size + ", " + this.y_size + ", " + this.z_size);

            long startTime = System.nanoTime();

            for (int x = 0; x < this.x_size; x++) {
                for (int z = 0; z < this.z_size; z++) {
                    final Column srcCol = this.myColumns[x][z];
                    final Column dstCol = dst.myColumns[x][z];

                    for (int y = 0; y < this.y_size; y++) {
                        var src_y = this.y_offset + y;
                        var dst_y = dst.y_offset + y;

                        if (srcCol.doNotSkip(src_y) && dstCol.doNotSkip(dst_y)) {
                            var srcSection = srcCol.getSection(src_y);
                            var dstSection = dstCol.getSection(dst_y);

                            var srcState = srcSection.getBlockState(srcCol.x, SectionPos.sectionRelative(src_y),
                                    srcCol.z);
                            if (srcState == CachedPlane.this.matrixBlockState) {
                                srcState = Blocks.AIR.getDefaultState();
                            }
                            var dstState = dstSection.getBlockState(dstCol.x, SectionPos.sectionRelative(dst_y),
                                    dstCol.z);
                            if (dstState == CachedPlane.this.matrixBlockState) {
                                dstState = Blocks.AIR.getDefaultState();
                            }

                            srcSection.setBlockState(srcCol.x, SectionPos.sectionRelative(src_y), srcCol.z, dstState);
                            dstSection.setBlockState(dstCol.x, SectionPos.sectionRelative(dst_y), dstCol.z, srcState);
                        } else {
                            this.markForUpdate(this.x_offset + x, src_y, this.z_offset + z);
                            dst.markForUpdate(dst.x_offset + x, dst_y, dst.z_offset + z);
                        }
                    }
                }
            }

            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            AELog.info("Block Copy Time: " + duration);

            for (var moveRecord : this.blockEntities) {
                var pos = moveRecord.blockEntity().getPos();
                dst.addBlockEntity(pos.getX() - this.x_offset, pos.getY() - this.y_offset,
                        pos.getZ() - this.z_offset,
                        moveRecord);
            }

            for (var moveRecord : dst.blockEntities) {
                var pos = moveRecord.blockEntity().getPos();
                this.addBlockEntity(pos.getX() - dst.x_offset, pos.getY() - dst.y_offset,
                        pos.getZ() - dst.z_offset, moveRecord);
            }

            for (var entry : this.ticks) {
                var movedPos = entry.pos().offset(-this.x_offset, -this.y_offset, -this.z_offset);
                dst.addTick(movedPos, entry);
            }

            for (var entry : dst.ticks) {
                var movedPos = entry.pos().offset(-dst.x_offset, -dst.y_offset, -dst.z_offset);
                addTick(movedPos, entry);
            }

            for (var record : this.poiMoveRecords) {
                dst.addPoi(record);
            }

            for (var record : dst.poiMoveRecords) {
                addPoi(record);
            }

            startTime = System.nanoTime();
            this.updateChunks();
            dst.updateChunks();
            endTime = System.nanoTime();

            duration = endTime - startTime;
            AELog.info("Update Time: " + duration);
        }
    }

    private void markForUpdate(int x, int y, int z) {
        this.updates.add(new BlockPos(x, y, z));
        for (EnumFacing d : EnumFacing.VALUES) {
            this.updates.add(new BlockPos(x + d.getXOffset(), y + d.getYOffset(), z + d.getZOffset()));
        }
    }

    private void addTick(BlockPos pos, ScheduledTick<Block> tick) {
        this.level.getBlockTicks().schedule(new ScheduledTick<>(
                tick.type(), pos, tick.triggerTick(), tick.priority(), tick.subTickOrder()));
    }

    private void addBlockEntity(int x, int y, int z, BlockEntityMoveRecord moveRecord) {
        try {
            var originalPos = moveRecord.pos();

            var c = this.myColumns[x][z];
            if (!c.doNotSkip(y + this.y_offset)) {
                AELog.warn(
                        "Block entity %s was queued to be moved from %s, but it's position then skipped during the move.",
                        moveRecord.blockEntity(), originalPos);
                return;
            }

            var newPosition = new BlockPos(x + this.x_offset, y + this.y_offset, z + this.z_offset);

            // Restore original block state to the section directly. The chunk would create the BE and notify neighbors.
            var chunk = this.level.getChunk(newPosition);
            var section = chunk.getSection(chunk.getSectionIndex(newPosition.getY()));
            section.setBlockState(
                    newPosition.getX() & (LevelChunkSection.SECTION_WIDTH - 1),
                    newPosition.getY() & (LevelChunkSection.SECTION_HEIGHT - 1),
                    newPosition.getZ() & (LevelChunkSection.SECTION_WIDTH - 1),
                    moveRecord.state);

            var strategy = moveRecord.strategy();
            boolean success;
            try {
                success = strategy.completeMove(moveRecord.blockEntity(), moveRecord.state(), moveRecord.savedData(),
                        this.level,
                        newPosition);
            } catch (Throwable e) {
                AELog.warn(e);
                success = false;
            }

            if (!success) {
                attemptRecovery(x, y, z, moveRecord, c);
            }
        } catch (Throwable e) {
            AELog.warn(e);
        }
    }

    private void addPoi(PoiMoveRecord record) {
        level.getPoiManager().add(record.relativePos.offset(this.x_offset, this.y_offset, this.z_offset),
                record.poiType);
    }

    private void attemptRecovery(int x, int y, int z, BlockEntityMoveRecord moveRecord, Column c) {
        var pos = new BlockPos(x, y, z);
        var type = moveRecord.blockEntity().getType();
        AELog.debug("Trying to recover BE %s @ %s", BlockEntityType.getKey(type), pos);

        // attempt recovery, but do not reuse the same TE instance since we did destroy it
        var blockState = Platform.getBlockState(moveRecord.blockEntity());
        var recoveredEntity = TileEntity.loadStatic(pos, blockState, moveRecord.savedData());
        if (recoveredEntity != null) {
            // We need to restore the block state too before re-setting the entity
            this.level.setBlockState(pos, blockState, 3);
            c.c.addAndRegisterBlockEntity(recoveredEntity);
            this.level.notifyBlockUpdate(pos, this.level.getBlockState(pos), this.level.getBlockState(pos), z);
        } else {
            AELog.warn("Failed to recover BE %s @ %s", BlockEntityType.getKey(type), pos);
        }
    }

    private void updateChunks() {

        LevelLightEngine lightManager = level.getLightEngine();

        // update shit..
        if (lightManager instanceof ThreadedLevelLightEngine serverLightManager) {
            for (int x = 0; x < this.cx_size; x++) {
                for (int z = 0; z < this.cz_size; z++) {
                    final Chunk c = this.myChunks[x][z];
                    serverLightManager.lightChunk(c, false);
                    c.setUnsaved(true);
                }
            }
        }

        // send shit...
        for (int x = 0; x < this.cx_size; x++) {
            for (int z = 0; z < this.cz_size; z++) {

                final Chunk c = this.myChunks[x][z];

                ServerCompassService.updateArea(this.getLevel(), c);

                var cdp = Platform.getFullChunkPacket(c);
                level.getChunkProvider().chunkMap.getPlayers(c.getPos(), false)
                        .forEach(spe -> spe.connection.send(cdp));
            }
        }

        // FIXME check if this makes any sense at all to send changes to players asap
        level.getChunkProvider().tick(() -> false, false);
    }

    List<BlockPos> getUpdates() {
        return this.updates;
    }

    WorldServer getLevel() {
        return this.level;
    }

    private static class BlockStorageData {
        public IBlockState state;
    }

    private static class Column {
        private final int x;
        private final int z;

        private final Chunk c;
        private List<Integer> skipThese = null;
        private Int2ObjectMap<IBlockState> savedBlockStates = null;

        public Column(Chunk chunk, int x, int z) {
            this.x = x;
            this.z = z;
            this.c = chunk;
        }

        private boolean doNotSkip(int y) {
            var blockState = getSection(y).getBlockState(this.x, SectionPos.sectionRelative(y), this.z);
            if (AETags.SPATIAL_BLACKLIST.contains(blockState)) {
                return false;
            }

            return this.skipThese == null || !this.skipThese.contains(y);
        }

        private void setSkip(int y) {
            if (this.skipThese == null) {
                this.skipThese = new ArrayList<>();
            }
            this.skipThese.add(y);
        }

        public LevelChunkSection getSection(int y) {
            return c.getSection(c.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(y)));
        }
    }

    private final static class BlockEntityMoveRecord {
        private final IBlockEntityMoveStrategy strategy;
        private final TileEntity blockEntity;
        private final NBTTagCompound savedData;
        private final BlockPos pos;
        private final IBlockState state;

        public BlockEntityMoveRecord(IBlockEntityMoveStrategy strategy, TileEntity blockEntity, NBTTagCompound savedData, BlockPos pos, IBlockState state) {
            this.strategy = strategy;
            this.blockEntity = blockEntity;
            this.savedData = savedData;
            this.pos = pos;
            this.state = state;
        }

        public IBlockEntityMoveStrategy strategy() {
            return strategy;
        }

        public TileEntity blockEntity() {
            return blockEntity;
        }

        public NBTTagCompound savedData() {
            return savedData;
        }

        public BlockPos pos() {
            return pos;
        }

        public IBlockState state() {
            return state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockEntityMoveRecord)) return false;
            BlockEntityMoveRecord that = (BlockEntityMoveRecord) o;
            return java.util.Objects.equals(strategy, that.strategy)
                    && java.util.Objects.equals(blockEntity, that.blockEntity)
                    && java.util.Objects.equals(savedData, that.savedData)
                    && java.util.Objects.equals(pos, that.pos)
                    && java.util.Objects.equals(state, that.state);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(strategy, blockEntity, savedData, pos, state);
        }

        @Override
        public String toString() {
            return "BlockEntityMoveRecord[" + "strategy=" + strategy + ", " + "blockEntity=" + blockEntity + ", " + "savedData=" + savedData + ", " + "pos=" + pos + ", " + "state=" + state + "]";
        }

    }

    private final static class PoiMoveRecord {
        private final BlockPos relativePos;
        private final Holder<PoiType> poiType;

        public PoiMoveRecord(BlockPos relativePos, Holder<PoiType> poiType) {
            this.relativePos = relativePos;
            this.poiType = poiType;
        }

        public BlockPos relativePos() {
            return relativePos;
        }

        public Holder<PoiType> poiType() {
            return poiType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PoiMoveRecord)) return false;
            PoiMoveRecord that = (PoiMoveRecord) o;
            return java.util.Objects.equals(relativePos, that.relativePos)
                    && java.util.Objects.equals(poiType, that.poiType);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(relativePos, poiType);
        }

        @Override
        public String toString() {
            return "PoiMoveRecord[" + "relativePos=" + relativePos + ", " + "poiType=" + poiType + "]";
        }

    }
}
