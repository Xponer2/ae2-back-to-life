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

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import appeng.api.ids.AETags;
import appeng.api.movable.BlockEntityMoveStrategies;
import appeng.api.movable.IBlockEntityMoveStrategy;
import appeng.core.AELog;
import appeng.core.definitions.AEBlocks;
import appeng.server.services.compass.ServerCompassService;
import appeng.util.Platform;

/**
 * A box of blocks that is swapped with another box of the same size (spatial IO). Works directly on the chunk sections
 * like 1.21; 1.12.2 has no points of interest to move.
 */
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
    private final List<NextTickListEntry> ticks = new ArrayList<>();
    private final WorldServer level;
    private final List<BlockPos> updates = new ArrayList<>();
    private final IBlockState matrixBlockState;

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
        final int minCZ = minZ >> 4;
        final int maxCX = maxX >> 4;
        final int maxCZ = maxZ >> 4;

        this.cx_size = maxCX - minCX + 1;
        this.cz_size = maxCZ - minCZ + 1;

        this.myChunks = new Chunk[this.cx_size][this.cz_size];
        this.myColumns = new Column[this.x_size][this.z_size];

        for (int x = 0; x < this.x_size; x++) {
            for (int z = 0; z < this.z_size; z++) {
                this.myColumns[x][z] = new Column(level.getChunk(minX + x >> 4, minZ + z >> 4), minX + x & 0xF,
                        minZ + z & 0xF, level.provider.hasSkyLight());
            }
        }

        final long now = level.getTotalWorldTime();
        for (int cx = 0; cx < this.cx_size; cx++) {
            for (int cz = 0; cz < this.cz_size; cz++) {
                final Chunk c = level.getChunk(minCX + cx, minCZ + cz);
                this.myChunks[cx][cz] = c;

                // Make a copy of the BE list in the chunk. This allows us to immediately remove BE's we're moving.
                var blockEntities = new ArrayList<TileEntity>();
                for (var entry : c.getTileEntityMap().entrySet()) {
                    var pos = entry.getKey();
                    if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                            && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                        blockEntities.add(entry.getValue());
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
                    var column = this.myColumns[pos.getX() - minX][pos.getZ() - minZ];
                    var section = column.getSection(pos.getY());
                    var state = section.get(column.x, pos.getY() & 15, column.z);

                    if (savedData != null) {
                        this.blockEntities.add(
                                new BlockEntityMoveRecord(strategy, blockEntity, savedData, pos, state));

                        // Set the state to AIR now since that prevents it from being resurrected recursively
                        section.set(column.x, pos.getY() & 15, column.z, Blocks.AIR.getDefaultState());
                        level.removeTileEntity(pos);
                    } else {
                        // don't skip air, just let the code replace it...
                        if (state.getBlock().isAir(state, level, pos)) {
                            level.setBlockToAir(pos);
                        } else {
                            column.setSkip(pos.getY());
                        }
                    }
                }

                var pending = level.getPendingBlockUpdates(c, false);
                if (pending != null) {
                    for (var entry : pending) {
                        var pos = entry.position;
                        if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                                && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                            var copy = new NextTickListEntry(pos, entry.getBlock());
                            copy.scheduledTime = entry.scheduledTime - now;
                            copy.setPriority(entry.priority);
                            this.ticks.add(copy);
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

                            var srcState = srcSection.get(srcCol.x, src_y & 15, srcCol.z);
                            if (srcState == CachedPlane.this.matrixBlockState) {
                                srcState = Blocks.AIR.getDefaultState();
                            }
                            var srcLight = srcSection.getBlockLight(srcCol.x, src_y & 15, srcCol.z);
                            var dstState = dstSection.get(dstCol.x, dst_y & 15, dstCol.z);
                            if (dstState == CachedPlane.this.matrixBlockState) {
                                dstState = Blocks.AIR.getDefaultState();
                            }
                            var dstLight = dstSection.getBlockLight(dstCol.x, dst_y & 15, dstCol.z);

                            srcSection.set(srcCol.x, src_y & 15, srcCol.z, dstState);
                            srcSection.setBlockLight(srcCol.x, src_y & 15, srcCol.z, dstLight);
                            dstSection.set(dstCol.x, dst_y & 15, dstCol.z, srcState);
                            dstSection.setBlockLight(dstCol.x, dst_y & 15, dstCol.z, srcLight);
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
                var pos = moveRecord.pos();
                dst.addBlockEntity(pos.getX() - this.x_offset, pos.getY() - this.y_offset,
                        pos.getZ() - this.z_offset,
                        moveRecord);
            }

            for (var moveRecord : dst.blockEntities) {
                var pos = moveRecord.pos();
                this.addBlockEntity(pos.getX() - dst.x_offset, pos.getY() - dst.y_offset,
                        pos.getZ() - dst.z_offset, moveRecord);
            }

            for (var entry : this.ticks) {
                var movedPos = entry.position.add(-this.x_offset, -this.y_offset, -this.z_offset);
                dst.addTick(movedPos, entry);
            }

            for (var entry : dst.ticks) {
                var movedPos = entry.position.add(-dst.x_offset, -dst.y_offset, -dst.z_offset);
                addTick(movedPos, entry);
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

    private void addTick(BlockPos relativePos, NextTickListEntry tick) {
        var pos = relativePos.add(this.x_offset, this.y_offset, this.z_offset);
        this.level.scheduleBlockUpdate(pos, tick.getBlock(), (int) tick.scheduledTime, tick.priority);
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
            c.getSection(newPosition.getY()).set(c.x, newPosition.getY() & 15, c.z, moveRecord.state());

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
                attemptRecovery(newPosition, moveRecord);
            }
        } catch (Throwable e) {
            AELog.warn(e);
        }
    }

    private void attemptRecovery(BlockPos pos, BlockEntityMoveRecord moveRecord) {
        var id = TileEntity.getKey(moveRecord.blockEntity().getClass());
        AELog.debug("Trying to recover BE %s @ %s", id, pos);

        // attempt recovery, but do not reuse the same TE instance since we did destroy it
        NBTTagCompound data = moveRecord.savedData().copy();
        data.setInteger("x", pos.getX());
        data.setInteger("y", pos.getY());
        data.setInteger("z", pos.getZ());
        var recoveredEntity = TileEntity.create(this.level, data);
        if (recoveredEntity != null) {
            // We need to restore the block state too before re-setting the entity
            var blockState = moveRecord.state();
            this.level.setBlockState(pos, blockState, 3);
            this.level.setTileEntity(pos, recoveredEntity);
            this.level.notifyBlockUpdate(pos, blockState, blockState, 3);
        } else {
            AELog.warn("Failed to recover BE %s @ %s", id, pos);
        }
    }

    private void updateChunks() {

        // update shit..
        for (int x = 0; x < this.cx_size; x++) {
            for (int z = 0; z < this.cz_size; z++) {
                final Chunk c = this.myChunks[x][z];
                c.resetRelightChecks();
                c.generateSkylightMap();
                c.markDirty();
            }
        }

        // send shit...
        for (int x = 0; x < this.cx_size; x++) {
            for (int z = 0; z < this.cz_size; z++) {

                final Chunk c = this.myChunks[x][z];

                ServerCompassService.updateArea(this.getLevel(), c);

                var entry = level.getPlayerChunkMap().getEntry(c.x, c.z);
                if (entry != null) {
                    entry.sendPacket(Platform.getFullChunkPacket(c));
                }
            }
        }
    }

    List<BlockPos> getUpdates() {
        return this.updates;
    }

    WorldServer getLevel() {
        return this.level;
    }

    private static class Column {
        private final int x;
        private final int z;

        private final Chunk c;
        private final boolean skyLight;
        private List<Integer> skipThese = null;

        public Column(Chunk chunk, int x, int z, boolean skyLight) {
            this.x = x;
            this.z = z;
            this.c = chunk;
            this.skyLight = skyLight;
        }

        private boolean doNotSkip(int y) {
            var blockState = getSection(y).get(this.x, y & 15, this.z);
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

        /**
         * The chunk section containing y, created if it doesn't exist yet.
         */
        public ExtendedBlockStorage getSection(int y) {
            var storage = c.getBlockStorageArray();
            var section = storage[y >> 4];
            if (section == Chunk.NULL_BLOCK_STORAGE) {
                section = storage[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, skyLight);
            }
            return section;
        }
    }

    private final static class BlockEntityMoveRecord {
        private final IBlockEntityMoveStrategy strategy;
        private final TileEntity blockEntity;
        private final NBTTagCompound savedData;
        private final BlockPos pos;
        private final IBlockState state;

        public BlockEntityMoveRecord(IBlockEntityMoveStrategy strategy, TileEntity blockEntity,
                NBTTagCompound savedData, BlockPos pos, IBlockState state) {
            this.strategy = strategy;
            this.blockEntity = blockEntity;
            this.savedData = savedData;
            this.pos = pos.toImmutable();
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
        public String toString() {
            return "BlockEntityMoveRecord[" + "strategy=" + strategy + ", " + "blockEntity=" + blockEntity + ", "
                    + "savedData=" + savedData + ", " + "pos=" + pos + ", " + "state=" + state + "]";
        }
    }
}
