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

package appeng.block.spatial;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.spatial.SpatialPylonBlockEntity;

public class SpatialPylonBlock extends AEBaseEntityBlock<SpatialPylonBlockEntity> {

    public static final PropertyBool POWERED_ON = PropertyBool.create("powered_on");

    public SpatialPylonBlock() {
        super(glassProps().lightLevel(state -> {
            return state.getValue(POWERED_ON) ? 8 : 0;
        }));
        setDefaultState(getDefaultState().withProperty(POWERED_ON, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED_ON);
    }

    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, SpatialPylonBlockEntity be) {
        var state = be.getClientState();
        return currentState.withProperty(SpatialPylonBlock.POWERED_ON, state.powered());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos,
            boolean isMoving) {
        var tsp = this.getTileEntity(level, pos);
        if (tsp != null) {
            tsp.neighborChanged(fromPos);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
