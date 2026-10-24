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

import javax.vecmath.Vector3f;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.client.render.PoseStack;
import appeng.client.render.model.DriveBakedModel;

/**
 * Renders the drive cell status indicators.
 */
@SideOnly(Side.CLIENT)
public class DriveLedBlockEntityRenderer extends AEBlockEntityRenderer<DriveBlockEntity> {

    @Override
    public void render(DriveBlockEntity drive, float partialTicks, PoseStack ms, int combinedLightIn) {

        if (drive.getCellCount() != 10) {
            throw new IllegalStateException("Expected drive to have 10 slots");
        }

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        var blockOrientation = BlockOrientation.get(drive);
        ms.mulPose(blockOrientation);
        ms.translate(-0.5, -0.5, -0.5);

        var buffer = CellLedRenderer.begin();
        Vector3f slotTranslation = new Vector3f();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 2; col++) {
                DriveBakedModel.getSlotOrigin(row, col, slotTranslation);

                int slot = row * 2 + col;
                CellLedRenderer.renderLed(drive, slot, buffer, slotTranslation.x, slotTranslation.y,
                        slotTranslation.z, partialTicks);
            }
        }
        CellLedRenderer.end();

        ms.popPose();
    }

}
