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

package appeng.block.qnb;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import appeng.blockentity.qnb.QuantumBridgeBlockEntity;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class QuantumRingBlock extends QuantumBaseBlock {

    private static final VoxelShape SHAPE = createShape(2.0 / 16.0);
    private static final VoxelShape SHAPE_CORNER = createShape(2.0 / 16.0);
    private static final VoxelShape SHAPE_CENTER = createCenterShape(2.0 / 16.0);

    private static final double DEFAULT_MODEL_MIN = 2.0 / 16.0;
    private static final double DEFAULT_MODEL_MAX = 14.0 / 16.0;

    public QuantumRingBlock() {
        super(metalProps());
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        final QuantumBridgeBlockEntity bridge = this.getTileEntity(level, pos);
        if (bridge != null && bridge.isCorner()) {
            return SHAPE_CORNER;
        } else if (bridge != null && bridge.isFormed()) {
            return SHAPE_CENTER;
        }
        return SHAPE;
    }

    private static VoxelShape createShape(double onePixel) {
        return Shapes.create(
                new AxisAlignedBB(onePixel, onePixel, onePixel, 1.0 - onePixel, 1.0 - onePixel, 1.0 - onePixel));
    }

    private static VoxelShape createCenterShape(double offset) {
        VoxelShape centerShape = SHAPE;
        for (EnumFacing facing : EnumFacing.VALUES) {
            double xOffset = Math.abs(facing.getXOffset() * offset);
            double yOffset = Math.abs(facing.getYOffset() * offset);
            double zOffset = Math.abs(facing.getZOffset() * offset);

            VoxelShape extrusion = Shapes.create(
                    new AxisAlignedBB(DEFAULT_MODEL_MIN - xOffset, DEFAULT_MODEL_MIN - yOffset,
                            DEFAULT_MODEL_MIN - zOffset, DEFAULT_MODEL_MAX + xOffset,
                            DEFAULT_MODEL_MAX + yOffset, DEFAULT_MODEL_MAX + zOffset));

            centerShape = Shapes.or(centerShape, extrusion);
        }

        return centerShape;
    }
}
