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

package appeng.block.crafting;

import appeng.block.BlockProperties;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.MolecularAssemblerMenu;
import appeng.menu.locator.MenuLocators;

public class MolecularAssemblerBlock extends AEBaseEntityBlock<MolecularAssemblerBlockEntity> {

    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public MolecularAssemblerBlock(BlockProperties props) {
        super(props);
        setDefaultState(getDefaultState().withProperty(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, MolecularAssemblerBlockEntity be) {
        return currentState.withProperty(POWERED, be.isPowered());
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        var be = this.getTileEntity(level, pos);
        if (be != null) {
            if (!level.isRemote) {
                MenuOpener.open(MolecularAssemblerMenu.TYPE, player,
                        MenuLocators.forBlockEntity(be));
            }
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.PASS;
    }

}
