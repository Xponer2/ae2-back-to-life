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

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.client.render.BakedQuads;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;
import appeng.core.AppEng;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;

/**
 * Renders the dynamic parts of an inscriber (the presses, the animation and the item being smashed)
 */
@SideOnly(Side.CLIENT)
public final class InscriberTESR extends AEBlockEntityRenderer<InscriberBlockEntity> {

    private static final float ITEM_RENDER_SCALE = 1.0f / 1.2f;

    private static final String TEXTURE_INSIDE = AppEng.makeId("block/inscriber_inside").toString();

    @Override
    public void render(InscriberBlockEntity blockEntity, float partialTicks, PoseStack ms, int combinedLight) {

        // render inscriber

        ms.pushPose();
        ms.translate(0.5F, 0.5F, 0.5F);
        BlockOrientation orientation = BlockOrientation.get(blockEntity);
        ms.mulPose(orientation);
        ms.translate(-0.5F, -0.5F, -0.5F);

        // render sides of stamps

        long absoluteProgress = 0;

        if (blockEntity.isSmash()) {
            final long currentTime = System.currentTimeMillis();
            absoluteProgress = currentTime - blockEntity.getClientStart();
            if (absoluteProgress > 800) {
                blockEntity.setSmash(false);
                if (blockEntity.isRepeatSmash()) {
                    blockEntity.setSmash(true);
                }
            }
        }

        final float relativeProgress = absoluteProgress % 800 / 400.0f;
        float progress = relativeProgress;

        if (progress > 1.0f) {
            progress = 1.0f - easeDecompressMotion(progress - 1.0f);
        } else {
            progress = easeCompressMotion(progress);
        }

        float press = 0.2f;
        press -= progress / 5.0f;

        float middle = 0.5f;
        middle += 0.02f;
        final float TwoPx = 2.0f / 16.0f;
        final float base = 0.4f;

        final TextureAtlasSprite tas = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(TEXTURE_INSIDE);

        // Solid, unshaded quads (1.21 RenderType.solid())
        GlStateManager.disableLighting();
        var tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        // Bottom of Top Stamp
        addVertex(buffer, tas, TwoPx, middle + press, TwoPx, 0.875f, 0.125f, combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle + press, TwoPx, 0.125f, 0.125f, combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle + press, 1.0f - TwoPx, 0.125f, 0.875f, combinedLight);
        addVertex(buffer, tas, TwoPx, middle + press, 1.0f - TwoPx, 0.875f, 0.875f, combinedLight);

        // Front of Top Stamp
        addVertex(buffer, tas, TwoPx, middle + base, TwoPx, 0.125f, 0.125f - (press - base), combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle + base, TwoPx, 0.875f, 0.125f - (press - base), combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle + press, TwoPx, 0.875f, 0.125f, combinedLight);
        addVertex(buffer, tas, TwoPx, middle + press, TwoPx, 0.125f, 0.125f, combinedLight);

        // Rear of Top Stamp
        addVertex(buffer, tas, TwoPx, middle + base, 1.0f - TwoPx, 0.125f, 0.125f - (press - base), combinedLight);
        addVertex(buffer, tas, TwoPx, middle + press, 1.0f - TwoPx, 0.125f, 0.125f, combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle + press, 1.0f - TwoPx, 0.875f, 0.125f, combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle + base, 1.0f - TwoPx, 0.875f, 0.125f - (press - base),
                combinedLight);

        // Top of Bottom Stamp
        middle -= 2.0f * 0.02f;
        addVertex(buffer, tas, 1.0f - TwoPx, middle - press, TwoPx, 0.875f, 0.125f, combinedLight);
        addVertex(buffer, tas, TwoPx, middle - press, TwoPx, 0.125f, 0.125f, combinedLight);
        addVertex(buffer, tas, TwoPx, middle - press, 1.0f - TwoPx, 0.125f, 0.875f, combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle - press, 1.0f - TwoPx, 0.875f, 0.875f, combinedLight);

        // Front of Bottom Stamp
        addVertex(buffer, tas, 1.0f - TwoPx, middle - base, TwoPx, 0.125f, 0.125f - (press - base), combinedLight);
        addVertex(buffer, tas, TwoPx, middle - base, TwoPx, 0.875f, 0.125f - (press - base), combinedLight);
        addVertex(buffer, tas, TwoPx, middle - press, TwoPx, 0.875f, 0.125f, combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle - press, TwoPx, 0.125f, 0.125f, combinedLight);

        // Rear of Bottom Stamp
        addVertex(buffer, tas, TwoPx, middle - press, 1.0f - TwoPx, 0.875f, 0.125f, combinedLight);
        addVertex(buffer, tas, TwoPx, middle - base, 1.0f - TwoPx, 0.875f, 0.125f - (press - base), combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle - base, 1.0f - TwoPx, 0.125f, 0.125f - (press - base),
                combinedLight);
        addVertex(buffer, tas, 1.0f - TwoPx, middle - press, 1.0f - TwoPx, 0.125f, 0.125f, combinedLight);

        tessellator.draw();
        GlStateManager.enableLighting();

        // render items.

        var inv = blockEntity.getInternalInventory();

        int items = 0;
        if (!inv.getStackInSlot(0).isEmpty()) {
            items++;
        }
        if (!inv.getStackInSlot(1).isEmpty()) {
            items++;
        }
        if (!inv.getStackInSlot(2).isEmpty()) {
            items++;
        }

        boolean renderPresses;
        if (relativeProgress > 1.0f || items == 0) {
            // When crafting completes, dont render the presses (they mave have been
            // consumed, see below)
            renderPresses = false;

            ItemStack is = inv.getStackInSlot(3);

            if (is.isEmpty()) {
                final InscriberRecipe ir = blockEntity.getTask();
                if (ir != null) {
                    // The "PRESS" type will consume the presses so they should not render after
                    // completing
                    // the press animation
                    renderPresses = ir.getProcessType() == InscriberProcessType.INSCRIBE;
                    is = ir.getResultItem().copy();
                }
            }
            this.renderItem(ms, is, 0.0f, combinedLight);
        } else {
            renderPresses = true;
            this.renderItem(ms, inv.getStackInSlot(2), 0.0f, combinedLight);
        }

        if (renderPresses) {
            this.renderItem(ms, inv.getStackInSlot(0), press, combinedLight);
            this.renderItem(ms, inv.getStackInSlot(1), -press, combinedLight);
        }

        ms.popPose();
    }

    private static void addVertex(BufferBuilder vb, TextureAtlasSprite sprite, float x, float y,
            float z, float texU, float texV, int lightmapUV) {
        vb.pos(x, y, z)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .tex(BakedQuads.getU(sprite, texU), BakedQuads.getV(sprite, texV))
                .lightmap(lightmapUV >> 16 & 0xFFFF, lightmapUV & 0xFFFF)
                .endVertex();
    }

    private void renderItem(PoseStack ms, ItemStack stack, float o, int combinedLight) {
        if (!stack.isEmpty()) {
            ms.pushPose();
            // move to center
            ms.translate(0.5f, 0.5f + o, 0.5f);
            ms.rotateX(90);

            // set scale
            ms.scale(ITEM_RENDER_SCALE, ITEM_RENDER_SCALE, ITEM_RENDER_SCALE);

            // heuristic to scale items down much further than blocks,
            // the assumption here is that the generated item models will return their faces
            // for direction=null, while a block-model will have their faces for
            // cull-faces, but not direction=null
            var model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, null, null);
            var quads = model.getQuads(null, null, 0);
            if (quads != null && !quads.isEmpty()) {
                ms.scale(0.5f, 0.5f, 0.5f);
            }

            BlockEntityRenderHelper.renderItem(stack, ItemCameraTransforms.TransformType.FIXED, combinedLight);
            ms.popPose();
        }
    }

    // See https://easings.net/#easeOutBack
    private static float easeCompressMotion(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;

        return (float) (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
    }

    // See https://easings.net/#easeInQuint
    private static float easeDecompressMotion(float x) {
        return x * x * x * x * x;
    }

}
