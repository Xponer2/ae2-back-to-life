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

import java.util.EnumMap;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.storage.cells.CellState;
import appeng.client.render.BlockEntityRenderHelper;

/**
 * Utility class to render LEDs for storage cells from a Block entity Renderer.
 */
@SideOnly(Side.CLIENT)
public class CellLedRenderer {

    private static final EnumMap<CellState, float[]> STATE_COLORS;

    // Color to use if the cell is present but unpowered
    private static final float[] UNPOWERED_COLOR = new float[] { 0, 0, 0 };

    // Color used for the cell indicator for blinking during recent activity
    private static final float[] BLINK_COLOR = new float[] { 1, 0.5f, 0.5f };

    static {
        STATE_COLORS = new EnumMap<>(CellState.class);
        for (var cellState : CellState.values()) {
            var color = cellState.getStateColor();
            var colorVector = new float[] {
                    ((color >> 16) & 0xFF) / 255.0f,
                    ((color >> 8) & 0xFF) / 255.0f,
                    (color & 0xFF) / 255.0f };
            STATE_COLORS.put(cellState, colorVector);
        }
    }

    // The positions are based on the upper left slot in a drive
    private static final float L = 5 / 16.f; // left (x-axis)
    private static final float R = 4 / 16.f; // right (x-axis)
    private static final float T = 1 / 16.f; // top (y-axis)
    private static final float B = -0.001f / 16.f; // bottom (y-axis)
    private static final float FR = -0.001f / 16.f; // front (z-axis)
    private static final float BA = 0.999f / 16.f; // back (z-axis)

    // Vertex data for the LED cuboid (has no back)
    // Directions are when looking from the front onto the LED
    private static final float[] LED_QUADS = {
            // Front Face
            R, T, FR, L, T, FR, L, B, FR, R, B, FR,
            // Left Face
            L, T, FR, L, T, BA, L, B, BA, L, B, FR,
            // Right Face
            R, T, BA, R, T, FR, R, B, FR, R, B, BA,
            // Top Face
            R, T, BA, L, T, BA, L, T, FR, R, T, FR,
            // Bottom Face
            R, B, FR, L, B, FR, L, B, BA, R, B, BA, };

    /**
     * Starts a batch of LEDs: untextured, unlit (1.21 position_color shader). Draws with the current GL transform.
     */
    public static BufferBuilder begin() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        BlockEntityRenderHelper.setLightmap(BlockEntityRenderHelper.FULL_BRIGHT);
        var buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        return buffer;
    }

    public static void end() {
        Tessellator.getInstance().draw();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    /**
     * Adds the LED of the given slot, moved by the given offset.
     */
    public static void renderLed(IChestOrDrive drive, int slot, BufferBuilder buffer, float offsetX, float offsetY,
            float offsetZ, float partialTicks) {
        float[] color = getColorForSlot(drive, slot, partialTicks);
        if (color == null) {
            return;
        }

        for (int i = 0; i < LED_QUADS.length; i += 3) {
            float x = LED_QUADS[i] + offsetX;
            float y = LED_QUADS[i + 1] + offsetY;
            float z = LED_QUADS[i + 2] + offsetZ;
            buffer.pos(x, y, z).color(color[0], color[1], color[2], 1.f).endVertex();
        }
    }

    @Nullable
    private static float[] getColorForSlot(IChestOrDrive drive, int slot, float partialTicks) {
        var state = drive.getCellStatus(slot);
        if (state == CellState.ABSENT) {
            return null;
        }

        if (!drive.isPowered()) {
            return UNPOWERED_COLOR;
        }

        float[] col = STATE_COLORS.get(state);
        if (drive.isCellBlinking(slot)) {
            // 200 ms interval (100ms to get to red, then 100ms back)
            long t = System.currentTimeMillis() % 200;
            float f = (t - 100) / 200.0f + 0.5f;
            f = easeInOutCubic(f);
            col = new float[] {
                    col[0] + (BLINK_COLOR[0] - col[0]) * f,
                    col[1] + (BLINK_COLOR[1] - col[1]) * f,
                    col[2] + (BLINK_COLOR[2] - col[2]) * f };
        }

        return col;
    }

    private static float easeInOutCubic(float x) {
        return x < 0.5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
    }

    private CellLedRenderer() {
    }

}
