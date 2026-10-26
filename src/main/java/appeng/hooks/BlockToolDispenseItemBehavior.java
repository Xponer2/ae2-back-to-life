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

package appeng.hooks;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import appeng.hooks.IBlockTool;
import appeng.util.UseOnContext;

public final class BlockToolDispenseItemBehavior extends BehaviorDefaultDispenseItem {

    @Override
    protected ItemStack dispenseStack(IBlockSource dispenser, ItemStack dispensedItem) {
        if (dispensedItem.getItem() instanceof IBlockTool tm) {
            final var direction = dispenser.getBlockState().getValue(BlockDispenser.FACING);

            if (dispenser.getWorld() instanceof WorldServer level) {
                // 1.21 DirectionalPlaceContext: the block in front of the dispenser, clicked on its near side
                var pos = dispenser.getBlockPos().offset(direction);
                var hit = new RayTraceResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                        direction.getOpposite(), pos);
                tm.useOn(new UseOnContext(level, null, EnumHand.MAIN_HAND, dispensedItem, hit));
            }
        }
        return dispensedItem;
    }
}
