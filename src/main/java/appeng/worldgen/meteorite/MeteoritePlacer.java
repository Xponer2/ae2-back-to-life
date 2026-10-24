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

package appeng.worldgen.meteorite;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fluids.IFluidBlock;

import appeng.core.AEConfig;
import appeng.core.definitions.AEBlocks;
import appeng.core.tags.TagManager;
import appeng.decorative.solid.CertusQuartzClusterBlock;
import appeng.worldgen.meteorite.fallout.Fallout;
import appeng.worldgen.meteorite.fallout.FalloutCopy;
import appeng.worldgen.meteorite.fallout.FalloutMode;
import appeng.worldgen.meteorite.fallout.FalloutSand;
import appeng.worldgen.meteorite.fallout.FalloutSnow;

/**
 * Places the part of a meteorite that lies inside the given bounding box (one chunk column, see
 * {@link MeteoriteWorldGen}). Port of the 1.21 placer: same shapes and probabilities.
 */
public final class MeteoritePlacer {
    private static final ResourceLocation REPLACEABLE_TAG = new ResourceLocation("minecraft", "replaceable");

    public static void place(World level, PlacedMeteoriteSettings settings, StructureBoundingBox boundingBox,
            Random random) {
        var placer = new MeteoritePlacer(level, settings, boundingBox, random);
        placer.place();
    }

    private final IBlockState skyStone;
    private final List<IBlockState> quartzBlocks;
    private final List<IBlockState> quartzBuds;
    private final MeteoriteBlockPutter putter = new MeteoriteBlockPutter();
    private final World level;
    private final Random random;
    private final Fallout type;
    private final BlockPos pos;
    private final int x;
    private final int y;
    private final int z;
    private final double meteoriteSize;
    private final double squaredMeteoriteSize;
    private final double crater;
    private final boolean placeCrater;
    private final CraterType craterType;
    private final boolean pureCrater;
    private final boolean craterLake;
    private final StructureBoundingBox boundingBox;

    private MeteoritePlacer(World level, PlacedMeteoriteSettings settings, StructureBoundingBox boundingBox,
            Random random) {
        this.boundingBox = boundingBox;
        this.level = level;
        this.random = random;
        this.pos = settings.getPos();
        this.x = settings.getPos().getX();
        this.y = settings.getPos().getY();
        this.z = settings.getPos().getZ();
        this.meteoriteSize = settings.getMeteoriteRadius();
        this.placeCrater = settings.shouldPlaceCrater();
        this.craterType = settings.getCraterType();
        this.pureCrater = settings.isPureCrater();
        this.craterLake = settings.isCraterLake();
        this.squaredMeteoriteSize = this.meteoriteSize * this.meteoriteSize;

        double realCrater = this.meteoriteSize * 2 + 5;
        this.crater = realCrater * realCrater;

        this.quartzBlocks = getQuartzBudList();
        this.quartzBuds = Stream.of(
                AEBlocks.SMALL_QUARTZ_BUD,
                AEBlocks.MEDIUM_QUARTZ_BUD,
                AEBlocks.LARGE_QUARTZ_BUD).map(def -> def.block().getDefaultState()).collect(Collectors.toList());
        this.skyStone = AEBlocks.SKY_STONE_BLOCK.block().getDefaultState();

        var center = new BlockPos(
                (boundingBox.minX + boundingBox.maxX) / 2,
                (boundingBox.minY + boundingBox.maxY) / 2,
                (boundingBox.minZ + boundingBox.maxZ) / 2);
        this.type = getFallout(level, center, settings.getFallout());
    }

    private List<IBlockState> getQuartzBudList() {
        if (AEConfig.instance().isSpawnFlawlessOnlyEnabled()) {
            return Stream.of(AEBlocks.FLAWLESS_BUDDING_QUARTZ).map(def -> def.block().getDefaultState())
                    .collect(Collectors.toList());
        }
        return Stream.of(
                AEBlocks.QUARTZ_BLOCK,
                AEBlocks.DAMAGED_BUDDING_QUARTZ,
                AEBlocks.CHIPPED_BUDDING_QUARTZ,
                AEBlocks.FLAWED_BUDDING_QUARTZ,
                AEBlocks.FLAWLESS_BUDDING_QUARTZ).map(def -> def.block().getDefaultState())
                .collect(Collectors.toList());
    }

    public void place() {
        // creator
        if (placeCrater) {
            this.placeCrater();
        }

        this.placeMeteorite();

        // collapse blocks...
        if (placeCrater) {
            this.decay();
        }
        if (craterLake) {
            this.placeCraterLake();
        }
    }

    private int clampX(int x) {
        return Math.max(boundingBox.minX, Math.min(boundingBox.maxX, x));
    }

    private int clampZ(int z) {
        return Math.max(boundingBox.minZ, Math.min(boundingBox.maxZ, z));
    }

    private void placeCrater() {
        final int maxY = level.getHeight() - 1;
        MutableBlockPos blockPos = new MutableBlockPos();
        IBlockState filler = craterType.getFiller().getDefaultState();

        for (int j = y - 5; j <= maxY; j++) {
            for (int i = boundingBox.minX; i <= boundingBox.maxX; i++) {
                for (int k = boundingBox.minZ; k <= boundingBox.maxZ; k++) {
                    blockPos.setPos(i, j, k);
                    final double dx = i - x;
                    final double dz = k - z;
                    final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

                    final double distanceFrom = dx * dx + dz * dz;

                    if (j > h + distanceFrom * 0.02) {
                        IBlockState currentBlock = level.getBlockState(blockPos);

                        if (craterType != CraterType.NORMAL && j < y && currentBlock.getMaterial().isSolid()) {
                            this.putter.put(level, blockPos, filler);
                        } else {
                            this.putter.put(level, blockPos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }

        for (var e : level.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(clampX(x - 30), y - 5, clampZ(z - 30), clampX(x + 30), y + 30, clampZ(z + 30)))) {
            e.setDead();
        }
    }

    private void placeMeteorite() {
        // spawn meteor
        this.placeMeteoriteSkyStone();

        // If the meteorite's center is within the BB of the current placer, place the chest
        if (boundingBox.isVecInside(pos)) {
            placeChest();
        }
    }

    private void placeChest() {
        if (AEConfig.instance().isSpawnPressesInMeteoritesEnabled()) {
            this.putter.put(level, pos, AEBlocks.MYSTERIOUS_CUBE.block().getDefaultState());
        }
    }

    private void placeMeteoriteSkyStone() {
        final int meteorXLength = clampX(x - 8);
        final int meteorXHeight = clampX(x + 8);
        final int meteorZLength = clampZ(z - 8);
        final int meteorZHeight = clampZ(z + 8);

        MutableBlockPos pos = new MutableBlockPos();
        for (int i = meteorXLength; i <= meteorXHeight; i++) {
            for (int j = y - 8; j < y + 8; j++) {
                for (int k = meteorZLength; k <= meteorZHeight; k++) {
                    pos.setPos(i, j, k);
                    var dx = i - x;
                    var dy = j - y;
                    var dz = k - z;

                    if (dx * dx * 0.7 + dy * dy * (j > y ? 1.4 : 0.8) + dz * dz * 0.7 < this.squaredMeteoriteSize) {
                        // Leave a tiny room in the center
                        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1) {
                            if (dy == -1) {
                                // Certus
                                var certusIndex = random.nextInt(quartzBlocks.size());
                                this.putter.put(level, pos, quartzBlocks.get(certusIndex));
                                // Add a bud on top if it's not a regular certus block (index 0), and not the center.
                                // (70% chance)
                                if (certusIndex != 0 && (dx != 0 || dz != 0) && random.nextFloat() <= 0.7) {
                                    var bud = quartzBuds.get(random.nextInt(quartzBuds.size())); // vanilla Util.getRandom
                                    var budState = bud.withProperty(CertusQuartzClusterBlock.FACING, EnumFacing.UP);
                                    this.putter.put(level, pos.up(), budState);
                                }
                            }
                        } else {
                            this.putter.put(level, pos, skyStone);
                        }
                    }
                }
            }
        }
    }

    private boolean isReplaceable(IBlockState state, BlockPos pos) {
        return state.getBlock().isReplaceable(level, pos);
    }

    private void decay() {
        double randomShit = 0;

        final int meteorXLength = clampX(x - 30);
        final int meteorXHeight = clampX(x + 30);
        final int meteorZLength = clampZ(z - 30);
        final int meteorZHeight = clampZ(z + 30);

        MutableBlockPos blockPos = new MutableBlockPos();
        MutableBlockPos blockPosUp = new MutableBlockPos();
        MutableBlockPos blockPosDown = new MutableBlockPos();
        for (int i = meteorXLength; i <= meteorXHeight; i++) {
            for (int k = meteorZLength; k <= meteorZHeight; k++) {
                for (int j = y - 9; j < y + 30; j++) {
                    blockPos.setPos(i, j, k);
                    blockPosUp.setPos(i, j + 1, k);
                    blockPosDown.setPos(i, j - 1, k);
                    IBlockState state = level.getBlockState(blockPos);
                    Block blk = state.getBlock();

                    if (this.pureCrater && blk == craterType.getFiller()) {
                        continue;
                    }

                    // TODO reconsider
                    if (isReplaceable(state, blockPos)) {
                        if (!level.isAirBlock(blockPosUp)) {
                            final IBlockState stateUp = level.getBlockState(blockPosUp);
                            level.setBlockState(blockPos, stateUp, 2);
                        } else if (randomShit < 100 * this.crater) {
                            final double dx = i - x;
                            final double dy = j - y;
                            final double dz = k - z;
                            final double dist = dx * dx + dy * dy + dz * dz;

                            final IBlockState xf = level.getBlockState(blockPosDown);
                            if (!isReplaceable(xf, blockPosDown)) {
                                final double extraRange = random.nextDouble() * 0.6;
                                final double height = this.crater * (extraRange + 0.2)
                                        - Math.abs(dist - this.crater * 1.7);

                                if (xf.getMaterial() != Material.AIR && height > 0 && random.nextDouble() > 0.6) {
                                    randomShit++;
                                    this.type.getRandomFall(level, blockPos);
                                }
                            }
                        }
                    } else if (level.isAirBlock(blockPosUp) && random.nextDouble() > 0.4) { // decay.
                        final double dx = i - x;
                        final double dy = j - y;
                        final double dz = k - z;
                        double dr2 = dx * dx + dy * dy + dz * dz;

                        // Don't touch the center room!
                        if (!(Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1) && dr2 < this.crater * 1.6) {
                            this.type.getRandomInset(level, blockPos);
                        }
                    }
                }
            }
        }
    }

    /**
     * If it finds a single water block at y62, it will replace any air blocks below the sea level with water.
     */
    private void placeCraterLake() {
        final int maxY = level.getSeaLevel() - 1;
        MutableBlockPos blockPos = new MutableBlockPos();
        Chunk currentChunk;

        for (int currentX = boundingBox.minX; currentX <= boundingBox.maxX; currentX++) {
            for (int currentZ = boundingBox.minZ; currentZ <= boundingBox.maxZ; currentZ++) {
                blockPos.setPos(currentX, 0, currentZ);
                currentChunk = level.getChunk(blockPos);

                for (int currentY = y - 5; currentY <= maxY; currentY++) {
                    blockPos.setPos(currentX, currentY, currentZ);

                    final double dx = currentX - x;
                    final double dz = currentZ - z;
                    final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

                    final double distanceFrom = dx * dx + dz * dz;

                    if (currentY > h + distanceFrom * 0.02) {
                        IBlockState currentBlock = currentChunk.getBlockState(blockPos);
                        if (currentBlock.getBlock() == Blocks.AIR) {
                            if (currentY == maxY) {
                                // 1.21 schedules a fluid tick for the top layer so the water can spread
                                this.putter.put(level, blockPos, Blocks.FLOWING_WATER.getDefaultState());
                                level.scheduleUpdate(blockPos.toImmutable(), Blocks.FLOWING_WATER, 0);
                            } else {
                                this.putter.put(level, blockPos, Blocks.WATER.getDefaultState());
                            }
                        }
                    } else if (maxY + (maxY - currentY) * 2 + 2 > h + distanceFrom * 0.02) {
                        pillarDownSlopeBlocks(currentChunk, blockPos);
                    }
                }
            }
        }
    }

    private void pillarDownSlopeBlocks(Chunk currentChunk, MutableBlockPos blockPos) {
        MutableBlockPos enclosingBlockPos = new MutableBlockPos(blockPos);

        for (int i = 0; i < 20; i++) {
            if (placeEnclosingBlock(currentChunk, enclosingBlockPos)) {
                break;
            }
            enclosingBlockPos.move(EnumFacing.DOWN);
        }
    }

    private boolean placeEnclosingBlock(Chunk currentChunk, MutableBlockPos enclosingBlockPos) {
        IBlockState currentState = currentChunk.getBlockState(enclosingBlockPos);
        var block = currentState.getBlock();
        boolean isFluid = block instanceof BlockLiquid || block instanceof IFluidBlock;
        if (block == Blocks.AIR || (!isFluid && (isReplaceable(currentState, enclosingBlockPos)
                || TagManager.isInBlockTag(REPLACEABLE_TAG, block, block.getMetaFromState(currentState))))) {

            if (craterType == CraterType.LAVA && random.nextFloat() < 0.075f) {
                this.putter.put(level, enclosingBlockPos, Blocks.MAGMA.getDefaultState());
            } else {
                this.type.getRandomFall(level, enclosingBlockPos);
            }
        } else {
            return true;
        }
        return false;
    }

    private Fallout getFallout(World level, BlockPos pos, FalloutMode mode) {
        return switch (mode) {
            case SAND -> new FalloutSand(level, pos, this.putter, this.skyStone, random);
            case TERRACOTTA -> new FalloutCopy(level, pos, this.putter, this.skyStone, random);
            case ICE_SNOW -> new FalloutSnow(level, pos, this.putter, this.skyStone, random);
            default -> new Fallout(this.putter, this.skyStone, random);
        };
    }

}
