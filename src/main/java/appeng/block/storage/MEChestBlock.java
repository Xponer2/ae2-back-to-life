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

package appeng.block.storage;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.api.storage.cells.CellState;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.localization.PlayerMessages;

public class MEChestBlock extends AEBaseEntityBlock<MEChestBlockEntity> {

    public final static PropertyBool LIGHTS_ON = PropertyBool.create("lights_on");

    public MEChestBlock() {
        super(metalProps());
        this.setDefaultState(this.getDefaultState().withProperty(LIGHTS_ON, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHTS_ON);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.full();
    }

    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, MEChestBlockEntity be) {
        var cellState = CellState.ABSENT;

        if (be.getCellCount() >= 1) {
            cellState = be.getCellStatus(0);
        }

        return currentState.withProperty(LIGHTS_ON, be.isPowered() && cellState != CellState.ABSENT);
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (level.getTileEntity(pos) instanceof MEChestBlockEntity be) {
            if (!level.isRemote) {
                if (hitResult.sideHit == be.getTop()) {
                    if (!be.openGui(player)) {
                        player.sendStatusMessage(PlayerMessages.ChestCannotReadStorageCell.text(), true);
                    }
                } else {
                    be.openCellInventoryMenu(player);
                }
            }

            return EnumActionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
