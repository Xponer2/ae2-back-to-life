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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import appeng.items.tools.powered.MatterCannonItem;
import appeng.util.LookDirection;
import appeng.util.Platform;

public final class MatterCannonDispenseItemBehavior extends BehaviorDefaultDispenseItem {

    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack dispensedItem) {
        if (dispensedItem.getItem() instanceof MatterCannonItem tm && source.getWorld() instanceof WorldServer level) {
            var position = BlockDispenser.getDispensePosition(source);
            var direction = source.getBlockState().getValue(BlockDispenser.FACING);

            var src = new Vec3d(
                    position.getX(),
                    position.getY(),
                    position.getZ());
            var dir = new LookDirection(
                    src,
                    src.add(
                            32 * direction.getXOffset(),
                            32 * direction.getYOffset(),
                            32 * direction.getZOffset()));

            var p = Platform.getFakePlayer(level, null);
            Platform.configurePlayer(p, direction, source.getBlockTileEntity());
            tm.fireCannon(level, dispensedItem, p, dir);
        }
        return dispensedItem;
    }
}
