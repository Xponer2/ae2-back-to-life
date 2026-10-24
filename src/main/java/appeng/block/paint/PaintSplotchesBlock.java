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

package appeng.block.paint;

import appeng.core.CreativeTabOutput;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.Fluid;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.PaintSplotchesBlockEntity;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class PaintSplotchesBlock extends AEBaseEntityBlock<PaintSplotchesBlockEntity> {

    /**
     * Lumen paint splotches contribute light-level 12, two or more have light-level 15. We model this with 0 = 0, 1 =
     * 12, 2 = 15.
     */
    public static final PropertyInteger LIGHT_LEVEL = PropertyInteger.create("light_level", 0, 2);

    public PaintSplotchesBlock() {
        super(defaultProps(MapColor.NONE, SoundType.WET_GRASS).noOcclusion().air().lightLevel(state -> {
            var lightLevel = state.getValue(LIGHT_LEVEL);
            return switch (lightLevel) {
                default -> 0;
                case 1 -> 12;
                case 2 -> 15;
            };
        }));
        setDefaultState(getDefaultState().withProperty(LIGHT_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT_LEVEL);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facingNoPlayerRotation();
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // do nothing
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos,
            boolean isMoving) {
        final PaintSplotchesBlockEntity tp = this.getTileEntity(level, pos);

        if (tp != null) {
            tp.neighborChanged();
        }
    }

    @Override
    public void handlePrecipitation(IBlockState state, World level, BlockPos pos, Biome.Precipitation precipitation) {
        if (!level.isRemote && precipitation == Biome.Precipitation.RAIN) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public boolean canBeReplaced(IBlockState state, BlockPlaceContext useContext) {
        return true;
    }

    @Override
    public boolean canBeReplaced(IBlockState state, Fluid fluid) {
        return true;
    }

}
