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

import java.util.Random;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The sky of the spatial storage dimension: a black skybox with sparkles that fade in and out. Set as the sky renderer
 * of the spatial storage world provider.
 */
@SideOnly(Side.CLIENT)
public class SpatialSkyRender extends IRenderHandler {

    private static final SpatialSkyRender INSTANCE = new SpatialSkyRender();

    private final Random random = new Random();

    // Display list for the sparkles, the 1.21 sparkle vertex buffer
    private int sparkleList = -1;

    private long cycle = 0;

    public static SpatialSkyRender getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        final long now = System.currentTimeMillis();
        if (sparkleList < 0) {
            sparkleList = GLAllocation.generateDisplayLists(1);
        }
        if (now - this.cycle > 2000) {
            this.cycle = now;
            this.rebuildSparkles();
        }

        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        RenderHelper.disableStandardItemLighting();

        var tessellator = Tessellator.getInstance();
        var builder = tessellator.getBuffer();

        // This renders a skybox around the player at a far, fixed distance from them.
        // The skybox is pitch black and untextured
        for (int i = 0; i < 6; ++i) {
            GlStateManager.pushMatrix();
            switch (i) {
                case 1 -> GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                case 2 -> GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                case 3 -> GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                case 4 -> GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                case 5 -> GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                default -> {
                }
            }

            // This is very similar to how the End sky is rendered, just untextured
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            builder.pos(-100.0, -100.0, -100.0).color(0f, 0f, 0f, 1f).endVertex();
            builder.pos(-100.0, -100.0, 100.0).color(0f, 0f, 0f, 1f).endVertex();
            builder.pos(100.0, -100.0, 100.0).color(0f, 0f, 0f, 1f).endVertex();
            builder.pos(100.0, -100.0, -100.0).color(0f, 0f, 0f, 1f).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        // Cycle the sparkles between 0 and 0.25 color value over 2 seconds
        float fade = now - this.cycle;
        fade /= 1000;
        fade = 0.25f * (1.0f - Math.abs((fade - 1.0f) * (fade - 1.0f)));

        if (fade > 0.0f) {
            GlStateManager.enableBlend();
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.color(fade, fade, fade, 1.0f);
            GlStateManager.callList(this.sparkleList);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableFog();
    }

    private void rebuildSparkles() {
        GlStateManager.glNewList(this.sparkleList, GL11.GL_COMPILE);
        var tessellator = Tessellator.getInstance();
        // Position only, so that the color set when calling the list tints the sparkles
        var vb = tessellator.getBuffer();
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        for (int i = 0; i < 50; ++i) {
            float iX = this.random.nextFloat() * 2.0f - 1.0f;
            float iY = this.random.nextFloat() * 2.0f - 1.0f;
            float iZ = this.random.nextFloat() * 2.0f - 1.0f;
            float d3 = 0.05F + this.random.nextFloat() * 0.1f;
            float dist = iX * iX + iY * iY + iZ * iZ;

            if (dist < 1.0f && dist > 0.01f) {
                dist = 1.0f / MathHelper.sqrt(dist);
                iX *= dist;
                iY *= dist;
                iZ *= dist;
                float x = iX * 100.0f;
                float y = iY * 100.0f;
                float z = iZ * 100.0f;
                float d8 = (float) MathHelper.atan2(iX, iZ);
                float d9 = MathHelper.sin(d8);
                float d10 = MathHelper.cos(d8);
                float d11 = (float) MathHelper.atan2(MathHelper.sqrt(iX * iX + iZ * iZ), iY);
                float d12 = MathHelper.sin(d11);
                float d13 = MathHelper.cos(d11);
                float d14 = this.random.nextFloat() * (float) Math.PI * 2.0f;
                float d15 = MathHelper.sin(d14);
                float d16 = MathHelper.cos(d14);

                for (int j = 0; j < 4; ++j) {
                    float d17 = 0.0f;
                    float d18 = ((j & 2) - 1) * d3;
                    float d19 = ((j + 1 & 2) - 1) * d3;
                    float d20 = d18 * d16 - d19 * d15;
                    float d21 = d19 * d16 + d18 * d15;
                    float d22 = d20 * d12 + d17 * d13;
                    float d23 = d17 * d12 - d20 * d13;
                    float d24 = d23 * d9 - d21 * d10;
                    float d25 = d21 * d9 + d23 * d10;
                    vb.pos(x + d24, y + d22, z + d25).endVertex();
                }
            }
        }

        tessellator.draw();
        GlStateManager.glEndList();
    }
}
