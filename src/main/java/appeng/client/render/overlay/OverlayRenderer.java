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

import java.util.Set;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * This is based on the area render of https://github.com/TeamPneumatic/pnc-repressurized/
 */
public class OverlayRenderer {

    private IOverlayDataSource source;

    OverlayRenderer(IOverlayDataSource source) {
        this.source = source;
    }

    @SideOnly(Side.CLIENT)
    void renderLinesOccluded() {
        render(GL11.GL_LINES, true, 0x30ffffff);
    }

    @SideOnly(Side.CLIENT)
    void renderFaces() {
        render(GL11.GL_QUADS, false, this.source.getOverlayColor());
    }

    @SideOnly(Side.CLIENT)
    void renderLines() {
        render(GL11.GL_LINES, true, this.source.getOverlayColor());
    }

    @SideOnly(Side.CLIENT)
    private void render(int mode, boolean renderLines, int color) {
        int[] cols = OverlayRenderType.decomposeColor(color);
        var tessellator = Tessellator.getInstance();
        var builder = tessellator.getBuffer();
        builder.begin(mode, DefaultVertexFormats.POSITION_COLOR);
        for (ChunkPos pos : this.source.getOverlayChunks()) {
            addVertices(builder, pos.getXStart(), pos.getZStart(), pos, cols, renderLines);
        }
        tessellator.draw();
    }

    @SideOnly(Side.CLIENT)
    private void addVertices(BufferBuilder wr, float offsetX, float offsetZ, ChunkPos pos, int[] cols,
            boolean renderLines) {
        Set<ChunkPos> chunks = this.source.getOverlayChunks();

        // Render around a whole chunk
        float x1 = offsetX;
        float x2 = offsetX + 16f;
        float y1 = 0;
        float y2 = source.getOverlaySourceLocation().getLevel().getHeight();
        float z1 = offsetZ;
        float z2 = offsetZ + 16f;

        boolean noNorth = !chunks.contains(new ChunkPos(pos.x, pos.z - 1));
        boolean noSouth = !chunks.contains(new ChunkPos(pos.x, pos.z + 1));
        boolean noWest = !chunks.contains(new ChunkPos(pos.x - 1, pos.z));
        boolean noEast = !chunks.contains(new ChunkPos(pos.x + 1, pos.z));

        if (noNorth) {
            // Face North, Edge Bottom
            vertex(wr, x1, y1, z1, cols);
            vertex(wr, x2, y1, z1, cols);
            // Face North, Edge Top
            vertex(wr, x2, y2, z1, cols);
            vertex(wr, x1, y2, z1, cols);
        }

        if (noSouth) {
            // Face South, Edge Bottom
            vertex(wr, x2, y1, z2, cols);
            vertex(wr, x1, y1, z2, cols);
            // Face South, Edge Top
            vertex(wr, x1, y2, z2, cols);
            vertex(wr, x2, y2, z2, cols);
        }

        if (noWest) {
            // Face West, Edge Bottom
            vertex(wr, x1, y1, z1, cols);
            vertex(wr, x1, y1, z2, cols);
            // Face West, Edge Top
            vertex(wr, x1, y2, z2, cols);
            vertex(wr, x1, y2, z1, cols);
        }

        if (noEast) {
            // Face East, Edge Bottom
            vertex(wr, x2, y1, z2, cols);
            vertex(wr, x2, y1, z1, cols);
            // Face East, Edge Top
            vertex(wr, x2, y2, z1, cols);
            vertex(wr, x2, y2, z2, cols);
        }

        if (renderLines) {
            if (noNorth || noWest) {
                // Face North, Edge West
                vertex(wr, x1, y1, z1, cols);
                vertex(wr, x1, y2, z1, cols);
            }

            if (noNorth || noEast) {
                // Face North, Edge East
                vertex(wr, x2, y2, z1, cols);
                vertex(wr, x2, y1, z1, cols);
            }

            if (noSouth || noEast) {
                // Face South, Edge East
                vertex(wr, x2, y1, z2, cols);
                vertex(wr, x2, y2, z2, cols);
            }

            if (noSouth || noWest) {
                // Face South, Edge West
                vertex(wr, x1, y2, z2, cols);
                vertex(wr, x1, y1, z2, cols);
            }
        } else {
            // Bottom Face
            vertex(wr, x1, y1, z1, cols);
            vertex(wr, x2, y1, z1, cols);
            vertex(wr, x2, y1, z2, cols);
            vertex(wr, x1, y1, z2, cols);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void vertex(BufferBuilder wr, float x, float y, float z, int[] cols) {
        wr.pos(x, y, z).color(cols[1], cols[2], cols[3], cols[0]).endVertex();
    }
}
