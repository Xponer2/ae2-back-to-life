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

package appeng.block.misc;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.CondenserBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.CondenserMenu;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

public class CondenserBlock extends AEBaseEntityBlock<CondenserBlockEntity> {

    public CondenserBlock() {
        super(metalProps());
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (!level.isRemote) {
            final CondenserBlockEntity tc = this.getTileEntity(level, pos);
            if (tc != null && !InteractionUtil.isInAlternateUseMode(player)) {
                MenuOpener.open(CondenserMenu.TYPE, player, MenuLocators.forBlockEntity(tc));
            }
        }

        return EnumActionResult.SUCCESS;
    }
}
