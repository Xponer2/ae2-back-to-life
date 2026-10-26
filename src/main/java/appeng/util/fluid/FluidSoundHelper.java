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

package appeng.util.fluid;

import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fluids.Fluid;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;

import appeng.api.stacks.AEFluidKey;

/**
 * Helps with playing fill/empty sounds for fluids to players.
 */
public final class FluidSoundHelper {

    private FluidSoundHelper() {
    }

    public static void playFillSound(EntityPlayer player, @Nullable AEFluidKey fluid) {
        if (fluid == null) {
            return;
        }

        SoundEvent fillSound = fluid.getFluid().getFillSound(fluid.toStack(Fluid.BUCKET_VOLUME));
        if (fillSound == null) {
            return;
        }

        playSound(player, fillSound);
    }

    public static void playEmptySound(EntityPlayer player, @Nullable AEFluidKey fluid) {
        if (fluid == null) {
            return;
        }

        SoundEvent fillSound = fluid.getFluid().getEmptySound(fluid.toStack(Fluid.BUCKET_VOLUME));
        if (fillSound == null) {
            return;
        }

        playSound(player, fillSound);
    }

    /**
     * @see net.minecraftforge.fluids.FluidUtil#tryFillContainer
     */
    private static void playSound(EntityPlayer player, SoundEvent fillSound) {
        // This should just play the sound for the player themselves
        if (player instanceof EntityPlayerMP serverPlayer) {
            serverPlayer.connection.sendPacket(new SPacketSoundEffect(fillSound, SoundCategory.PLAYERS,
                    player.posX, player.posY, player.posZ, 1.0F, 1.0F));
        } else {
            player.playSound(fillSound, 1.0F, 1.0F);
        }
    }

}
