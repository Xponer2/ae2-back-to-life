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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.client.render.BakedModelUnwrapper;
import appeng.client.render.BakedQuads;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.DelegateBakedModel;
import appeng.client.render.PoseStack;
import appeng.client.render.model.DriveBakedModel;
import appeng.core.definitions.AEBlocks;

/**
 * The block entity renderer for ME chests takes care of rendering the right model for the inserted cell, as well as the
 * LED.
 */
@SideOnly(Side.CLIENT)
public class ChestBlockEntityRenderer extends AEBlockEntityRenderer<MEChestBlockEntity> {

    @Override
    public void render(MEChestBlockEntity chest, float partialTicks, PoseStack poseStack, int combinedLight) {

        var level = chest.getWorld();
        if (level == null) {
            return;
        }

        var cellItem = chest.getCellItem(0);
        if (cellItem == null) {
            return; // No cell inserted into chest
        }

        // Try to get the right cell chassis model from the drive model since it already
        // loads them all
        DriveBakedModel driveModel = getDriveModel();
        if (driveModel == null) {
            return;
        }

        IBakedModel cellModel = driveModel.getCellChassisModel(cellItem);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        var rotation = BlockOrientation.get(chest);
        poseStack.mulPose(rotation);
        poseStack.translate(-0.5, -0.5, -0.5);

        // The models are created for the top-left slot of the drive model,
        // we need to move them into place for the slot on the ME chest
        poseStack.translate(5 / 16.0, 4 / 16.0, 0);

        // Render the cell model as-if it was a block model
        // We "fake" the position here to make it use the light-value in front of the
        // drive
        FaceRotatingModel rotatedModel = new FaceRotatingModel(cellModel, rotation);
        BlockEntityRenderHelper.renderBlockModel(level, rotatedModel, level.getBlockState(chest.getPos()),
                chest.getPos(), false);

        var ledBuffer = CellLedRenderer.begin();
        CellLedRenderer.renderLed(chest, 0, ledBuffer, 0, 0, 0, partialTicks);
        CellLedRenderer.end();

        poseStack.popPose();
    }

    @Nullable
    private DriveBakedModel getDriveModel() {
        IBakedModel driveModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
                .getModelForState(AEBlocks.DRIVE.block().getDefaultState());
        return BakedModelUnwrapper.unwrap(driveModel, DriveBakedModel.class);
    }

    /**
     * The actual vertex data will be transformed using the matrix stack, but the faces will not be correctly rotated so
     * the incorrect lighting data would be used to apply diffuse lighting and the lightmap texture.
     */
    public static class FaceRotatingModel extends DelegateBakedModel {
        private final BlockOrientation r;

        protected FaceRotatingModel(IBakedModel base, BlockOrientation r) {
            super(base);
            this.r = r;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            if (side != null) {
                side = BakedQuads.resultingRotate(r, side); // This fixes the incorrect lightmap position
            }
            List<BakedQuad> quads = new ArrayList<>(super.getQuads(state, side, rand));

            for (int i = 0; i < quads.size(); i++) {
                BakedQuad quad = quads.get(i);
                var face = quad.getFace() != null ? BakedQuads.rotate(r, quad.getFace()) : null;
                quads.set(i, new BakedQuad(quad.getVertexData(), quad.getTintIndex(), face,
                        quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
            }

            return quads;
        }
    }
}
