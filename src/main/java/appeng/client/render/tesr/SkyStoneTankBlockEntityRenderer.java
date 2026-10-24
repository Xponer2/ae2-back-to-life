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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.blockentity.storage.SkyStoneTankBlockEntity;
import appeng.client.render.PoseStack;
import appeng.client.render.cablebus.CubeBuilder;

@SideOnly(Side.CLIENT)
public final class SkyStoneTankBlockEntityRenderer extends AEBlockEntityRenderer<SkyStoneTankBlockEntity> {

    @Override
    public void render(SkyStoneTankBlockEntity tank, float tickDelta, PoseStack ms, int light) {
        var fluid = tank.getTank().getFluid();
        if (fluid != null && fluid.amount > 0) {
            /*
             *
             * // Uncomment to allow the liquid to rotate with the tank ms.pushPose(); ms.translate(0.5, 0.5, 0.5);
             * FacingToRotation.get(tank.getForward(), tank.getUp()).push(ms); ms.translate(-0.5, -0.5, -0.5);
             */
            drawFluidInTank(fluid, (float) fluid.amount / tank.getTank().getCapacity());
            // ms.popPose();
        }
    }

    private static final float TANK_W = 1 / 16f + 0.001f; // avoiding Z-fighting
    public static final int FULL_LIGHT = 0x00F0_00F0;

    /**
     * Draws the fluid with the current GL transform (relative to the block origin), translucent and full-bright.
     */
    public static void drawFluidInTank(FluidStack fluid, float fill) {
        // From Modern Industrialization
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
        int color = fluid.getFluid().getColor(fluid);
        float r = ((color >> 16) & 255) / 256f;
        float g = ((color >> 8) & 255) / 256f;
        float b = (color & 255) / 256f;

        var fillY = TANK_W + MathHelper.clamp(fill, 0, 1) * (1 - TANK_W - TANK_W);

        // Top and bottom positions of the fluid inside the tank
        float topHeight = fillY;
        float bottomHeight = TANK_W;

        // Render gas from top to bottom
        if (fluid.getFluid().isLighterThanAir()) {
            topHeight = 1 - TANK_W;
            bottomHeight = 1 - fillY;
        }

        var builder = new CubeBuilder();

        builder.setTexture(sprite);

        var x1 = TANK_W * 16;
        var z1 = TANK_W * 16;
        var x2 = (1 - TANK_W) * 16;
        var z2 = (1 - TANK_W) * 16;
        var y1 = bottomHeight * 16;
        var y2 = topHeight * 16;

        builder.addCube(x1, y1, z1, x2, y2, z2);

        // 1.21 RenderType.translucentMovingBlock()
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();

        var tessellator = Tessellator.getInstance();
        var buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        for (var bakedQuad : builder.getOutput()) {
            // Our quads have the same size as the block format, the lightmap/normal int is overwritten below
            buffer.addVertexData(bakedQuad.getVertexData());
            buffer.putBrightness4(FULL_LIGHT, FULL_LIGHT, FULL_LIGHT, FULL_LIGHT);
            buffer.putColorRGB_F4(r, g, b);
        }
        tessellator.draw();

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
    }
}
