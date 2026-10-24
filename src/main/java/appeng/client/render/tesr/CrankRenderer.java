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

package appeng.client.render.tesr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.misc.CrankBlockEntity;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;
import appeng.core.AppEng;
import appeng.init.client.InitAdditionalModels;

@SideOnly(Side.CLIENT)
public class CrankRenderer extends AEBlockEntityRenderer<CrankBlockEntity> {

    public static final ModelResourceLocation BASE_MODEL = InitAdditionalModels
            .standalone(AppEng.makeId("block/crank_base"));

    public static final ModelResourceLocation HANDLE_MODEL = InitAdditionalModels
            .standalone(AppEng.makeId("block/crank_handle"));

    @Override
    public void render(CrankBlockEntity crank, float partialTick, PoseStack stack, int packedLight) {

        var modelManager = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
                .getModelManager();
        var baseModel = modelManager.getModel(BASE_MODEL);
        var handleModel = modelManager.getModel(HANDLE_MODEL);
        var level = crank.getWorld();
        var pos = crank.getPos();
        var blockState = level.getBlockState(pos);

        // Apply GL transformations relative to the center of the block:
        // 1) Block orientation. The cranks top faces NORTH by default
        // and 2) crank rotation
        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);
        stack.mulPose(BlockOrientation.get(crank));
        stack.translate(-0.5, -0.5, -0.5);

        // Render the base model followed by the actual crank model
        BlockEntityRenderHelper.renderBlockModel(level, baseModel, blockState, pos, false);

        // The unrotated cranks orientation is towards NORTH (negative Z axis)
        stack.translate(0.5, 0.5, 0.5);
        stack.rotateZ(-crank.getVisibleRotation());
        stack.translate(-0.5, -0.5, -0.5);

        BlockEntityRenderHelper.renderBlockModel(level, handleModel, blockState, pos, false);

        stack.popPose();
    }

}
