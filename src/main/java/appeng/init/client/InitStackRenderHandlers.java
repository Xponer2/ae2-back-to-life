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

package appeng.init.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.client.gui.GuiGraphics;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;
import appeng.util.Platform;

@SideOnly(Side.CLIENT)
public class InitStackRenderHandlers {
    private InitStackRenderHandlers() {
    }

    public static void init() {
        AEKeyRendering.register(AEKeyType.items(), AEItemKey.class, new ItemKeyRenderHandler());
        AEKeyRendering.register(AEKeyType.fluids(), AEFluidKey.class, new FluidKeyRenderHandler());
    }

    private static class ItemKeyRenderHandler implements AEKeyRenderHandler<AEItemKey> {
        @Override
        public void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, AEItemKey stack) {
            var displayStack = stack.getReadOnlyStack();
            var renderItem = minecraft.getRenderItem();
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(displayStack, x, y);
            renderItem.renderItemOverlayIntoGUI(minecraft.fontRenderer, displayStack, x, y, "");
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }

        @Override
        public void drawOnBlockFace(PoseStack poseStack, AEItemKey what, float scale, int combinedLight,
                World level) {
            poseStack.pushPose();
            // Push it out of the block face a bit to avoid z-fighting
            poseStack.translate(0, 0, 0.01f);
            // The Z-scaling by 0.001 causes the model to be visually "flattened"
            // This cannot replace a proper projection, but it's cheap and gives the desired effect.
            poseStack.scale(scale, scale, 0.001f);

            // Keep the normals unit length despite the flattening (1.21 doesn't scale the normal matrix)
            boolean normalize = GL11.glIsEnabled(GL11.GL_NORMALIZE);
            GlStateManager.enableNormalize();
            BlockEntityRenderHelper.renderItem(what.getReadOnlyStack(), ItemCameraTransforms.TransformType.GUI,
                    combinedLight);
            if (!normalize) {
                GlStateManager.disableNormalize(); // not popAttrib: it would not update the GlStateManager cache
            }

            poseStack.popPose();
        }

        @Override
        public ITextComponent getDisplayName(AEItemKey stack) {
            return stack.getDisplayName();
        }

        @Override
        public List<ITextComponent> getTooltip(AEItemKey stack) {
            var minecraft = Minecraft.getMinecraft();
            var lines = stack.getReadOnlyStack().getTooltip(minecraft.player,
                    minecraft.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED
                            : ITooltipFlag.TooltipFlags.NORMAL);
            List<ITextComponent> result = new ArrayList<>(lines.size());
            for (var line : lines) {
                result.add(new TextComponentString(line));
            }
            return result;
        }
    }

    private static class FluidKeyRenderHandler implements AEKeyRenderHandler<AEFluidKey> {
        @Override
        public void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, AEFluidKey what) {
            var fluidStack = what.toStack(1);
            var sprite = getStillSprite(what);
            int color = what.getFluid().getColor(fluidStack);

            minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.enableBlend();
            GlStateManager.color((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f,
                    (color >>> 24 & 0xFF) / 255f);

            var tessellator = Tessellator.getInstance();
            var buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(x, y + 16, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
            buffer.pos(x + 16, y + 16, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(x + 16, y, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(x, y, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            tessellator.draw();

            GlStateManager.color(1, 1, 1, 1);
        }

        @Override
        public void drawOnBlockFace(PoseStack poseStack, AEFluidKey what, float scale, int combinedLight,
                World level) {
            var fluidStack = what.toStack(1);
            var sprite = getStillSprite(what);
            int color = what.getFluid().getColor(fluidStack);

            poseStack.pushPose();
            // Push it out of the block face a bit to avoid z-fighting
            poseStack.translate(0, 0, 0.01f);

            // In comparison to items, make it _slightly_ smaller because item icons
            // usually don't extend to the full size.
            scale -= 0.05f;

            // y is flipped here
            var x0 = -scale / 2;
            var y0 = scale / 2;
            var x1 = scale / 2;
            var y1 = -scale / 2;

            float r = (color >> 16 & 0xFF) / 255f;
            float g = (color >> 8 & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = (color >>> 24 & 0xFF) / 255f;
            int sky = combinedLight >> 16 & 0xFFFF;
            int block = combinedLight & 0xFFFF;

            // Solid, unshaded (1.21 RenderType.solid())
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.disableLighting();
            var tessellator = Tessellator.getInstance();
            var buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            buffer.pos(x0, y1, 0).color(r, g, b, a).tex(sprite.getMinU(), sprite.getMaxV()).lightmap(sky, block)
                    .endVertex();
            buffer.pos(x1, y1, 0).color(r, g, b, a).tex(sprite.getMaxU(), sprite.getMaxV()).lightmap(sky, block)
                    .endVertex();
            buffer.pos(x1, y0, 0).color(r, g, b, a).tex(sprite.getMaxU(), sprite.getMinV()).lightmap(sky, block)
                    .endVertex();
            buffer.pos(x0, y0, 0).color(r, g, b, a).tex(sprite.getMinU(), sprite.getMinV()).lightmap(sky, block)
                    .endVertex();
            tessellator.draw();
            GlStateManager.enableLighting();

            poseStack.popPose();
        }

        private static TextureAtlasSprite getStillSprite(AEFluidKey what) {
            var fluidStack = what.toStack(1);
            var texture = what.getFluid().getStill(fluidStack);
            return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString());
        }

        @Override
        public ITextComponent getDisplayName(AEFluidKey stack) {
            return stack.getDisplayName();
        }

        @Override
        public List<ITextComponent> getTooltip(AEFluidKey stack) {
            var tooltip = new ArrayList<ITextComponent>();
            tooltip.add(new TextComponentString(stack.toStack(1).getLocalizedName()));

            // Heuristic: If the last line doesn't include the modname, add it ourselves
            var modName = Platform.formatModName(stack.getModId());
            if (!tooltip.get(tooltip.size() - 1).getUnformattedText().equals(modName)) {
                tooltip.add(new TextComponentString(modName));
            }

            return tooltip;
        }
    }
}
