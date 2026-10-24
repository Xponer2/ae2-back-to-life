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

package appeng.client.render;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.util.AEColor;

/**
 * Automatically exposes the color of a colorable block entity using tint indices 0-2
 */
@SideOnly(Side.CLIENT)
public class ColorableBlockEntityBlockColor implements IBlockColor {

    public static final ColorableBlockEntityBlockColor INSTANCE = new ColorableBlockEntityBlockColor();

    @Override
    public int colorMultiplier(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos,
            int tintIndex) {
        AEColor color = AEColor.TRANSPARENT; // Default to a neutral color

        if (level != null && pos != null) {
            TileEntity te = level.getTileEntity(pos);
            if (te instanceof IColorableBlockEntity colorable) {
                color = colorable.getColor();
            }
        }

        return color.getVariantByTintIndex(tintIndex);
    }
}
