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

package appeng.client.render.overlay;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * This is based on the area render of https://github.com/TeamPneumatic/pnc-repressurized/
 * <p/>
 * 1.12.2 has no render types, so these set up the same GL state as the 1.21 render types: untextured, unlit
 * position+color geometry, either faces (crumbling transparency, depth test but no depth writes) or 3px lines
 * (additive, or translucent with an inverted depth test for the lines hidden behind blocks).
 */
@SideOnly(Side.CLIENT)
public final class OverlayRenderType {

    private OverlayRenderType() {
    }

    private static void begin() {
        // Restored explicitly in end(): popAttrib would not update the GlStateManager cache
        lightingWasEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableDepth();
    }

    /**
     * Faces: crumbling transparency, LEQUAL depth test, color writes only.
     */
    public static void beginBlockHilightFace() {
        end();
        begin();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR,
                GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(false);
        active = true;
    }

    public static void beginBlockHilightLine() {
        beginLines(false);
    }

    public static void beginBlockHilightLineOccluded() {
        beginLines(true);
    }

    private static void beginLines(boolean occluded) {
        end();
        begin();
        GlStateManager.glLineWidth(3.0f);
        if (occluded) {
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthFunc(GL11.GL_GREATER);
            GlStateManager.depthMask(false);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
        }
        active = true;
    }

    private static boolean active;
    private static boolean lightingWasEnabled;

    /**
     * Restores the GL state changed by the begin methods.
     */
    public static void end() {
        if (!active) {
            return;
        }
        active = false;
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        if (lightingWasEnabled) {
            GlStateManager.enableLighting();
        }
    }

    public static int[] decomposeColor(int color) {
        int[] res = new int[4];
        res[0] = color >> 24 & 0xff;
        res[1] = color >> 16 & 0xff;
        res[2] = color >> 8 & 0xff;
        res[3] = color & 0xff;
        return res;
    }

}
