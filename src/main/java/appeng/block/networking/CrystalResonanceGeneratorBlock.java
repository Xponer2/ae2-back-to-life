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

package appeng.block.networking;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.StringRepresentable;
import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockStateProperties;
import appeng.blockentity.networking.CrystalResonanceGeneratorBlockEntity;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class CrystalResonanceGeneratorBlock extends AEBaseEntityBlock<CrystalResonanceGeneratorBlockEntity> {

    public enum State implements StringRepresentable {
        OFF, ON, HAS_CHANNEL;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }


    public CrystalResonanceGeneratorBlock() {
        super(glassProps().noOcclusion().forceSolidOn());
        this.setDefaultState(this.getDefaultState());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
            }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return getVoxelShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return getVoxelShape(state);
    }

    @NotNull
    private VoxelShape getVoxelShape(IBlockState state) {
        var orientation = getOrientation(state);
        var forward = orientation.getSide(RelativeSide.FRONT);

        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 1;
        double maxY = 1;
        double maxZ = 1;

        switch (forward) {
            case DOWN -> {
                minZ = minX = 2.0 / 16.0;
                maxZ = maxX = 14.0 / 16.0;
                maxY = 1.0;
                minY = 1.0 / 16.0;
            }
            case EAST -> {
                minZ = minY = 2.0 / 16.0;
                maxZ = maxY = 14.0 / 16.0;
                maxX = 15.0 / 16.0;
                minX = 0.0;
            }
            case NORTH -> {
                minY = minX = 2.0 / 16.0;
                maxY = maxX = 14.0 / 16.0;
                maxZ = 1.0;
                minZ = 1.0 / 16.0;
            }
            case SOUTH -> {
                minY = minX = 2.0 / 16.0;
                maxY = maxX = 14.0 / 16.0;
                maxZ = 15.0 / 16.0;
                minZ = 0.0;
            }
            case UP -> {
                minZ = minX = 2.0 / 16.0;
                maxZ = maxX = 14.0 / 16.0;
                maxY = 15.0 / 16.0;
                minY = 0.0;
            }
            case WEST -> {
                minZ = minY = 2.0 / 16.0;
                maxZ = maxY = 14.0 / 16.0;
                maxX = 1.0;
                minX = 1.0 / 16.0;
            }
            default -> {
            }
        }

        return Shapes.create(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
    }


    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facing();
    }

    @Override
    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }


    @Override
    public IBlockState updateShape(IBlockState blockState, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {

        return super.updateShape(blockState, facing, facingState, level, currentPos, facingPos);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
