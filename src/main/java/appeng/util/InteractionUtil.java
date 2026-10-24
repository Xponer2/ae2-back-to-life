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

package appeng.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import appeng.datagen.providers.tags.ConventionTags;
import appeng.items.tools.NetworkToolItem;

/**
 * Utility functions revolving around using or placing items.
 */
public final class InteractionUtil {

    private InteractionUtil() {
    }

    /**
     * Checks if the given tool is a wrench capable of disassembling.
     */
    public static boolean canWrenchDisassemble(ItemStack tool) {
        // TODO FABRIC 117 Currently Fabric cannot dynamically distinguish tools / tool actions
        return ConventionTags.WRENCH.contains(tool);
    }

    /**
     * Checks if the given tool is a wrench capable of rotating.
     */
    public static boolean canWrenchRotate(ItemStack tool) {
        // TODO FABRIC 117 Currently Fabric cannot dynamically distinguish tools / tool actions
        // Special case to stop the network tool from rotating things instead of opening the appropriate UI
        if (tool.getItem() instanceof NetworkToolItem) {
            return false;
        }

        return ConventionTags.WRENCH.contains(tool);
    }

    /**
     * Checks if the given player is in the alternate use mode commonly expressed by "crouching" (holding shift).
     * Although there's also {@link EntityPlayer#isCrouching()}, this actually is only the visual pose, while
     * {@link EntityPlayer#isShiftKeyDown()} signifies that the player is holding shift.
     */
    public static boolean isInAlternateUseMode(EntityPlayer player) {
        return player.isSneaking();
    }

    public static LookDirection getPlayerRay(EntityPlayer playerIn, double reachDistance) {
        var x = playerIn.prevPosX + (playerIn.posX - playerIn.prevPosX);
        var y = playerIn.prevPosY + (playerIn.posY - playerIn.prevPosY) + playerIn.getEyeHeight();
        var z = playerIn.prevPosZ + (playerIn.posZ - playerIn.prevPosZ);

        var playerPitch = playerIn.xRotO + (playerIn.rotationPitch - playerIn.xRotO);
        var playerYaw = playerIn.yRotO + (playerIn.rotationYaw - playerIn.yRotO);

        var yawRayX = MathHelper.sin(-playerYaw * 0.017453292f - (float) Math.PI);
        var yawRayZ = MathHelper.cos(-playerYaw * 0.017453292f - (float) Math.PI);

        var pitchMultiplier = -MathHelper.cos(-playerPitch * 0.017453292F);
        var eyeRayY = MathHelper.sin(-playerPitch * 0.017453292F);
        var eyeRayX = yawRayX * pitchMultiplier;
        var eyeRayZ = yawRayZ * pitchMultiplier;

        var from = new Vec3d(x, y, z);
        var to = from.add(eyeRayX * reachDistance, eyeRayY * reachDistance, eyeRayZ * reachDistance);

        return new LookDirection(from, to);
    }
}
