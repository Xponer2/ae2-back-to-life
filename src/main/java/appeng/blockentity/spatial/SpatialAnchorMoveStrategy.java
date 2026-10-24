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

package appeng.blockentity.spatial;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.movable.DefaultBlockEntityMoveStrategy;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.BlockEntityType;

/**
 * When the spatial anchor is moved into spatial storage, it should briefly chunkload the area within the spatial
 * storage cell to allow the grid there to initialize.
 */
public class SpatialAnchorMoveStrategy extends DefaultBlockEntityMoveStrategy {
    @Override
    public boolean canHandle(BlockEntityType<?> type) {
        return type == AEBlockEntities.SPATIAL_ANCHOR.get();
    }

    @Nullable
    @Override
    public NBTTagCompound beginMove(TileEntity blockEntity) {
        var result = super.beginMove(blockEntity);
        if (result != null && blockEntity instanceof SpatialAnchorBlockEntity spatialAnchor) {
            // Just in case there are still some chunks left, as the level will change.
            spatialAnchor.releaseAll();
        }
        return result;
    }

    @Override
    public boolean completeMove(TileEntity blockEntity, IBlockState state, NBTTagCompound savedData, World newLevel,
            BlockPos newPosition) {
        if (!super.completeMove(blockEntity, state, savedData, newLevel, newPosition)) {
            return false;
        }
        // Notify the new block entity
        if (newLevel.getTileEntity(newPosition) instanceof SpatialAnchorBlockEntity spatialAnchor) {
            spatialAnchor.doneMoving();
        }
        return true;
    }
}
