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

package appeng.client.render.crafting;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.blockentity.crafting.CraftingMonitorBlockEntity;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;
import appeng.client.render.tesr.AEBlockEntityRenderer;

/**
 * Renders the item currently being crafted
 */
@SideOnly(Side.CLIENT)
public class CraftingMonitorRenderer extends AEBlockEntityRenderer<CraftingMonitorBlockEntity> {

    @Override
    public void render(CraftingMonitorBlockEntity be, float partialTicks, PoseStack poseStack, int combinedLight) {
        var orientation = be.getOrientation();

        var jobProgress = be.getJobProgress();
        if (jobProgress != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5); // Move to the center of the block

            BlockEntityRenderHelper.rotateToFace(poseStack, orientation);

            poseStack.translate(0, 0.02, 0.5);

            BlockEntityRenderHelper.renderItem2dWithAmount(
                    poseStack,
                    jobProgress.what(),
                    jobProgress.amount(), false,
                    0.3f,
                    -0.18f,
                    be.getColor().contrastTextColor, be.getWorld());

            poseStack.popPose();
        }
    }
}
