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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;

import appeng.core.definitions.AEBlocks;
import appeng.core.stats.AdvancementTriggers;

public class SpatialStorageHelper {

    private static SpatialStorageHelper instance;

    public static SpatialStorageHelper getInstance() {
        if (instance == null) {
            instance = new SpatialStorageHelper();
        }
        return instance;
    }

    /**
     * Mostly from dimensional doors.. which mostly got it form X-Comp.
     *
     * @param entity to be teleported entity
     * @param link   destination
     * @return teleported entity
     */
    private Entity teleportEntity(Entity entity, TelDestination link) {
        final WorldServer oldLevel;
        final WorldServer newLevel;

        try {
            oldLevel = (WorldServer) entity.world;
            newLevel = link.dim;
        } catch (Throwable e) {
            return entity;
        }

        if (oldLevel == null) {
            return entity;
        }
        if (newLevel == null) {
            return entity;
        }
        if (newLevel == oldLevel) {
            // just set the location. Minecraft will handle eventual passengers
            newLevel.getChunkProvider().provideChunk(MathHelper.floor(link.x) >> 4, MathHelper.floor(link.z) >> 4);
            if (entity instanceof EntityPlayerMP player) {
                player.connection.setPlayerLocation(link.x, link.y, link.z, entity.rotationYaw, entity.rotationPitch);
            } else {
                entity.setPositionAndUpdate(link.x, link.y, link.z);
            }
            return entity;
        }

        // Are we riding something? Teleport it instead.
        if (entity.isRiding()) {
            return this.teleportEntity(entity.getRidingEntity(), link);
        }

        // Is something riding us? Handle it first.
        final List<Entity> passengers = entity.getPassengers();
        final List<Entity> passengersOnOtherSide = new ArrayList<>(passengers.size());
        for (Entity passenger : passengers) {
            passenger.dismountRidingEntity();
            passengersOnOtherSide.add(this.teleportEntity(passenger, link));
        }
        // We keep track of all so we can remount them on the other side.

        // load the chunk!
        newLevel.getChunkProvider().provideChunk(MathHelper.floor(link.x) >> 4, MathHelper.floor(link.z) >> 4);

        if (entity instanceof EntityPlayerMP
                && newLevel.provider.getDimension() == SpatialStorageDimensionIds.getDimensionId()) {
            AdvancementTriggers.SPATIAL_EXPLORER.trigger((EntityPlayerMP) entity);
        }

        // Entities other than players are recreated in the new dimension
        var transported = entity.changeDimension(newLevel.provider.getDimension(), new ITeleporter() {
            @Override
            public void placeEntity(World world, Entity placed, float yaw) {
                placed.setLocationAndAngles(link.x, link.y, link.z, placed.rotationYaw, placed.rotationPitch);
                placed.motionX = placed.motionY = placed.motionZ = 0;
            }

            @Override
            public boolean isVanilla() {
                return false;
            }
        });
        if (transported == null) {
            return entity;
        }

        for (Entity passenger : passengersOnOtherSide) {
            passenger.startRiding(transported, true);
        }

        return transported;
    }

    private void transverseEdges(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, ISpatialVisitor visitor) {
        for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < maxZ; z++) {
                visitor.visit(new BlockPos(minX, y, z));
                visitor.visit(new BlockPos(maxX, y, z));
            }
        }

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                visitor.visit(new BlockPos(x, minY, z));
                visitor.visit(new BlockPos(x, maxY, z));
            }
        }

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                visitor.visit(new BlockPos(x, y, minZ));
                visitor.visit(new BlockPos(x, y, maxZ));
            }
        }
    }

    public void swapRegions(WorldServer srcLevel, int srcX, int srcY, int srcZ,
            WorldServer dstLevel, int dstX, int dstY, int dstZ, int scaleX,
            int scaleY, int scaleZ) {
        Block matrixFrameBlock = AEBlocks.MATRIX_FRAME.block();
        this.transverseEdges(dstX - 1, dstY - 1, dstZ - 1, dstX + scaleX + 1,
                dstY + scaleY + 1, dstZ + scaleZ + 1,
                new WrapInMatrixFrame(matrixFrameBlock.getDefaultState(), dstLevel));

        final AxisAlignedBB srcBox = new AxisAlignedBB(srcX, srcY, srcZ, srcX + scaleX + 1, srcY + scaleY + 1,
                srcZ + scaleZ + 1);

        final AxisAlignedBB dstBox = new AxisAlignedBB(dstX, dstY, dstZ, dstX + scaleX + 1, dstY + scaleY + 1,
                dstZ + scaleZ + 1);

        final CachedPlane cDst = new CachedPlane(dstLevel, dstX, dstY, dstZ, dstX + scaleX, dstY + scaleY,
                dstZ + scaleZ);
        final CachedPlane cSrc = new CachedPlane(srcLevel, srcX, srcY, srcZ, srcX + scaleX, srcY + scaleY,
                srcZ + scaleZ);

        // do nearly all the work... swaps blocks, block entities, and block ticks
        cSrc.swap(cDst);

        // The chunks (and with them their entities) were loaded by the CachedPlanes
        var srcE = srcLevel.getEntitiesWithinAABB(Entity.class, srcBox);
        var dstE = dstLevel.getEntitiesWithinAABB(Entity.class, dstBox);

        for (Entity e : dstE) {
            this.teleportEntity(e, new TelDestination(srcLevel, srcBox, e.posX, e.posY, e.posZ,
                    -dstX + srcX, -dstY + srcY, -dstZ + srcZ));
        }

        for (Entity e : srcE) {
            this.teleportEntity(e, new TelDestination(dstLevel, dstBox, e.posX, e.posY, e.posZ,
                    -srcX + dstX, -srcY + dstY, -srcZ + dstZ));
        }

        for (BlockPos pos : cDst.getUpdates()) {
            cSrc.getLevel().notifyNeighborsOfStateChange(pos, Blocks.AIR, true);
        }

        for (BlockPos pos : cSrc.getUpdates()) {
            cSrc.getLevel().notifyNeighborsOfStateChange(pos, Blocks.AIR, true);
        }

        this.transverseEdges(srcX - 1, srcY - 1, srcZ - 1, srcX + scaleX + 1, srcY + scaleY + 1, srcZ + scaleZ + 1,
                new TriggerUpdates(srcLevel));
        this.transverseEdges(dstX - 1, dstY - 1, dstZ - 1, dstX + scaleX + 1, dstY + scaleY + 1, dstZ + scaleZ + 1,
                new TriggerUpdates(dstLevel));

        this.transverseEdges(srcX, srcY, srcZ, srcX + scaleX, srcY + scaleY, srcZ + scaleZ,
                new TriggerUpdates(srcLevel));
        this.transverseEdges(dstX, dstY, dstZ, dstX + scaleX, dstY + scaleY, dstZ + scaleZ,
                new TriggerUpdates(dstLevel));
    }

    private static class TriggerUpdates implements ISpatialVisitor {

        private final World dst;

        public TriggerUpdates(World dst2) {
            this.dst = dst2;
        }

        @Override
        public void visit(BlockPos pos) {
            final IBlockState state = this.dst.getBlockState(pos);
            state.neighborChanged(this.dst, pos, state.getBlock(), pos);
        }
    }

    private static class WrapInMatrixFrame implements ISpatialVisitor {

        private final World dst;
        private final IBlockState state;

        public WrapInMatrixFrame(IBlockState state, World dst2) {
            this.dst = dst2;
            this.state = state;
        }

        @Override
        public void visit(BlockPos pos) {
            this.dst.setBlockState(pos, this.state);
        }
    }

    private static class TelDestination {
        private final WorldServer dim;
        private final double x;
        private final double y;
        private final double z;

        TelDestination(WorldServer dimension, AxisAlignedBB srcBox, double x, double y,
                double z, int blockEntityX, int blockEntityY, int blockEntityZ) {
            this.dim = dimension;
            this.x = Math.min(srcBox.maxX - 0.5, Math.max(srcBox.minX + 0.5, x + blockEntityX));
            this.y = Math.min(srcBox.maxY - 0.5, Math.max(srcBox.minY + 0.5, y + blockEntityY));
            this.z = Math.min(srcBox.maxZ - 0.5, Math.max(srcBox.minZ + 0.5, z + blockEntityZ));
        }
    }

}
