/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.init.client;

import net.minecraft.client.renderer.color.BlockColors;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.util.AEColor;
import appeng.block.networking.CableBusColor;
import appeng.client.render.ColorableBlockEntityBlockColor;
import appeng.client.render.StaticBlockColor;
import appeng.core.definitions.AEBlocks;

@SideOnly(Side.CLIENT)
public final class InitBlockColors {

    private InitBlockColors() {
    }

    /**
     * Called from {@code ColorHandlerEvent.Block}.
     */
    public static void init(BlockColors blockColors) {
        blockColors.registerBlockColorHandler(new StaticBlockColor(AEColor.TRANSPARENT),
                AEBlocks.WIRELESS_ACCESS_POINT.block());
        blockColors.registerBlockColorHandler(new CableBusColor(), AEBlocks.CABLE_BUS.block());
        blockColors.registerBlockColorHandler(ColorableBlockEntityBlockColor.INSTANCE, AEBlocks.ME_CHEST.block());
    }

}
