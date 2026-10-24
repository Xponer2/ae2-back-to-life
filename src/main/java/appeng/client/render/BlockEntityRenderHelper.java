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

package appeng.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.client.AEKeyRendering;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;

/**
 * Helper methods for rendering block entities.
 */
@SideOnly(Side.CLIENT)
public final class BlockEntityRenderHelper {

    /**
     * Packed lightmap value for full brightness (1.21 LightTexture.FULL_BRIGHT).
     */
    public static final int FULL_BRIGHT = 15 << 20 | 15 << 4;

    private BlockEntityRenderHelper() {
    }

    /**
     * Rotate the current coordinate system, so it is on the face of the given block side. This can be used to render on
     * the given face as if it was a 2D canvas, where x+ is facing right and y+ is facing up.
     */
    public static void rotateToFace(PoseStack stack, BlockOrientation orientation) {
        stack.mulPose(orientation);
        stack.rotateY(180);
    }

    /**
     * Sets the lightmap used for the following draws to the given packed light ({@code sky << 20 | block << 4}).
     */
    public static void setLightmap(int packedLight) {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, packedLight & 0xFFFF,
                packedLight >>> 16 & 0xFFFF);
    }

    /**
     * Render an item in 2D.
     */
    public static void renderItem2d(PoseStack poseStack,
            AEKey what,
            float scale,
            int combinedLightIn, World level) {
        AEKeyRendering.drawOnBlockFace(
                poseStack,
                what,
                scale,
                combinedLightIn, level);
    }

    /**
     * Render an item in 2D and the given text below it.
     *
     * @param spacing Specifies how far apart the item and the item stack amount are rendered.
     * @param level
     */
    public static void renderItem2dWithAmount(PoseStack poseStack,
            AEKey what,
            long amount, boolean canCraft,
            float itemScale,
            float spacing,
            int textColor, World level) {
        renderItem2d(poseStack, what, itemScale, FULL_BRIGHT, level);

        var renderedStackSize = amount == 0 && canCraft ? "Craft" : what.formatAmount(amount, AmountFormat.SLOT);

        // Render the item count
        var fr = Minecraft.getMinecraft().fontRenderer;
        var width = fr.getStringWidth(renderedStackSize);
        poseStack.pushPose();
        poseStack.translate(0.0f, spacing, 0.02f);
        poseStack.scale(1.0f / 62.0f, -1.0f / 62.0f, 1.0f / 62.0f);
        poseStack.scale(0.5f, 0.5f, 0);
        poseStack.translate(-0.5f * width, 0.0f, 0.5f);
        drawString(renderedStackSize, 0, 0, textColor);
        poseStack.popPose();
    }

    /**
     * Draws text in the world with full brightness (1.21 {@code Font.drawInBatch} with LightTexture.FULL_BRIGHT).
     */
    public static void drawString(String text, float x, float y, int color) {
        var fr = Minecraft.getMinecraft().fontRenderer;
        GlStateManager.pushAttrib();
        GlStateManager.disableLighting();
        setLightmap(FULL_BRIGHT);
        GlStateManager.depthMask(false);
        GlStateManager.color(1, 1, 1, 1);
        fr.drawString(text, (int) x, (int) y, color);
        GlStateManager.depthMask(true);
        GlStateManager.popAttrib();
    }

    /**
     * Renders an item stack with the current GL transform, like 1.21 {@code ItemRenderer.renderStatic}.
     */
    public static void renderItem(ItemStack stack, ItemCameraTransforms.TransformType transformType,
            int combinedLight) {
        if (stack.isEmpty()) {
            return;
        }
        GlStateManager.pushAttrib();
        RenderHelper.enableStandardItemLighting();
        setLightmap(combinedLight);
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, transformType);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popAttrib();
        // The item renderer binds its own textures
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    /**
     * Renders a baked block model with the current GL transform, lit like the block at the given position (1.21
     * {@code ModelBlockRenderer.tesselateBlock}).
     */
    public static void renderBlockModel(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos,
            boolean checkSides) {
        var tessellator = Tessellator.getInstance();
        var buffer = tessellator.getBuffer();

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        // The translation ensures the vertex buffer positions are relative to 0,0,0 instead of the block pos
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(level, model, state,
                pos, buffer, checkSides);
        buffer.setTranslation(0, 0, 0);
        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
    }
}
