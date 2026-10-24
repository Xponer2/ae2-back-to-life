/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.client.render.model;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;

/**
 * Used as the cache key for caching automatically rotated baked models.
 */
@SideOnly(Side.CLIENT)
final class AutoRotatingCacheKey {
    private final IBlockState blockState;
    private final BlockOrientation orientation;
    @Nullable
    private final EnumFacing side;

    AutoRotatingCacheKey(IBlockState blockState, BlockOrientation orientation, @Nullable EnumFacing side) {
        this.blockState = blockState;
        this.orientation = orientation;
        this.side = side;
    }

    public IBlockState getBlockState() {
        return this.blockState;
    }

    public BlockOrientation getOrientation() {
        return orientation;
    }

    @Nullable
    public EnumFacing getSide() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        AutoRotatingCacheKey cacheKey = (AutoRotatingCacheKey) o;
        return this.blockState.equals(cacheKey.blockState) && this.orientation == cacheKey.orientation
                && this.side == cacheKey.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockState, orientation, side);
    }
}
