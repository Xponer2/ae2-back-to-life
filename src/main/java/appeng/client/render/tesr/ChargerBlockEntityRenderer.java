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

package appeng.client.render.tesr;

import java.util.function.Supplier;

import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.client.render.renderable.ItemRenderable;

/**
 * Renders the item being charged.
 */
@SideOnly(Side.CLIENT)
public final class ChargerBlockEntityRenderer {

    private ChargerBlockEntityRenderer() {
    }

    public static final Supplier<ModularTESR<ChargerBlockEntity>> FACTORY = () -> new ModularTESR<>(
            new ItemRenderable<>(ChargerBlockEntityRenderer::getRenderedItem));

    private static Pair<ItemStack, Vector3f> getRenderedItem(ChargerBlockEntity blockEntity) {
        double time = System.currentTimeMillis() / 1000.0;
        float yOffset = (float) Math.sin(time) * 0.02f;
        Vector3f translation = new Vector3f(0.5f, 0.35f + yOffset, 0.5f);
        return new ImmutablePair<>(blockEntity.getInternalInventory().getStackInSlot(0), translation);
    }

}
