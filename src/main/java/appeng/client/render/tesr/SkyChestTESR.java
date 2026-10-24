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

import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.storage.SkyChestBlock;
import appeng.block.storage.SkyChestBlock.SkyChestType;
import appeng.blockentity.storage.SkyChestBlockEntity;
import appeng.client.render.PoseStack;
import appeng.core.AppEng;

// This is mostly a copy&paste job of the vanilla chest TESR
@SideOnly(Side.CLIENT)
public class SkyChestTESR extends AEBlockEntityRenderer<SkyChestBlockEntity> {

    // The textures are in the block sheet due to the item model requiring them there. The chest model uses the plain
    // texture files with the 64x64 layout of the 1.21 chest.
    public static final ResourceLocation TEXTURE_STONE = new ResourceLocation(AppEng.MOD_ID,
            "textures/block/skychest.png");
    public static final ResourceLocation TEXTURE_BLOCK = new ResourceLocation(AppEng.MOD_ID,
            "textures/block/skyblockchest.png");

    private static final float SCALE = 1 / 16.0f;

    private final ModelRenderer lid;
    private final ModelRenderer bottom;
    private final ModelRenderer lock;

    public SkyChestTESR() {
        // Same parts as the 1.21 layer definition (y up, no flipping)
        var model = new ModelBase() {
        };
        model.textureWidth = 64;
        model.textureHeight = 64;

        this.bottom = new ModelRenderer(model, 0, 19);
        this.bottom.addBox(1.0F, 0.0F, 1.0F, 14, 10, 14);

        this.lid = new ModelRenderer(model, 0, 0);
        this.lid.addBox(1.0F, 0.0F, 0.0F, 14, 5, 14);
        this.lid.setRotationPoint(0.0F, 10.0F, 1.0F);

        this.lock = new ModelRenderer(model, 0, 0);
        this.lock.addBox(7.0F, -1.0F, 15.0F, 2, 4, 1);
        this.lock.setRotationPoint(0.0F, 9.0F, 0.0F);
    }

    @Override
    public boolean isGlobalRenderer(SkyChestBlockEntity te) {
        return false;
    }

    @Override
    public void render(SkyChestBlockEntity blockEntity, float partialTicks, PoseStack matrixStackIn,
            int combinedLightIn) {
        matrixStackIn.pushPose();
        float f = blockEntity.getFront().getHorizontalAngle();
        matrixStackIn.translate(0.5D, 0.5D, 0.5D);
        matrixStackIn.rotateY(-f);
        matrixStackIn.translate(-0.5D, -0.5D, -0.5D);

        float f1 = blockEntity.getOpenNess(partialTicks);
        f1 = 1.0F - f1;
        f1 = 1.0F - f1 * f1 * f1;

        this.bindTexture(this.getRenderMaterial(blockEntity));
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderModels(f1);
        GlStateManager.disableRescaleNormal();

        matrixStackIn.popPose();
    }

    private void renderModels(float lidAngle) {
        this.lid.rotateAngleX = -(lidAngle * 1.5707964F);
        this.lock.rotateAngleX = this.lid.rotateAngleX;
        this.lid.render(SCALE);
        this.lock.render(SCALE);
        this.bottom.render(SCALE);
    }

    protected ResourceLocation getRenderMaterial(SkyChestBlockEntity blockEntity) {
        SkyChestType type = SkyChestType.BLOCK;
        if (blockEntity.getWorld() != null) {
            Block blockType = blockEntity.getBlockType();

            if (blockType instanceof SkyChestBlock) {
                type = ((SkyChestBlock) blockType).type;
            }
        }
        return switch (type) {
            case STONE -> TEXTURE_STONE;
            case BLOCK -> TEXTURE_BLOCK;
        };
    }
}
