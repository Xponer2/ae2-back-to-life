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

package appeng.client.render.cablebus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import com.google.common.base.Preconditions;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.BakedQuads;

/**
 * Builds the quads for a cube.
 */
@SideOnly(Side.CLIENT)
public class CubeBuilder {

    private final List<BakedQuad> output;

    private final EnumMap<EnumFacing, TextureAtlasSprite> textures = new EnumMap<>(EnumFacing.class);

    private EnumSet<EnumFacing> drawFaces = EnumSet.allOf(EnumFacing.class);

    private final EnumMap<EnumFacing, float[]> customUv = new EnumMap<>(EnumFacing.class);

    private byte[] uvRotations = new byte[EnumFacing.values().length];

    private final boolean[] flipU = new boolean[EnumFacing.values().length];
    private final boolean[] flipV = new boolean[EnumFacing.values().length];

    private int color = 0xFFFFFFFF;

    private boolean emissiveMaterial;

    public CubeBuilder(List<BakedQuad> output) {
        this.output = output;
    }

    public CubeBuilder() {
        this(new ArrayList<>(6));
    }

    public void addCube(float x1, float y1, float z1, float x2, float y2, float z2) {
        x1 /= 16.0f;
        y1 /= 16.0f;
        z1 /= 16.0f;
        x2 /= 16.0f;
        y2 /= 16.0f;
        z2 /= 16.0f;

        for (EnumFacing face : this.drawFaces) {
            this.putFace(face, x1, y1, z1, x2, y2, z2);
        }
    }

    public void addQuad(EnumFacing face, float x1, float y1, float z1, float x2, float y2, float z2) {
        this.putFace(face, x1, y1, z1, x2, y2, z2);
    }

    public void setFlipU(EnumFacing side, boolean enable) {
        flipU[side.ordinal()] = enable;
    }

    public void setFlipV(EnumFacing side, boolean enable) {
        flipV[side.ordinal()] = enable;
    }

    private static final class UvVector {
        float u1;
        float u2;
        float v1;
        float v2;
    }

    private void putFace(EnumFacing face, float x1, float y1, float z1, float x2, float y2, float z2) {
        var texture = this.textures.get(face);

        var uv = new UvVector();

        // The user might have set specific UV coordinates for this face
        var customUv = this.customUv.get(face);
        if (customUv != null) {
            uv.u1 = BakedQuads.getU(texture, customUv[0]);
            uv.v1 = BakedQuads.getV(texture, customUv[1]);
            uv.u2 = BakedQuads.getU(texture, customUv[2]);
            uv.v2 = BakedQuads.getV(texture, customUv[3]);
        } else {
            uv = this.getStandardUv(face, texture, x1, y1, z1, x2, y2, z2);
        }

        var uvs = new float[4][];
        setFaceUV(face, uvs, uv);

        var positions = switch (face) {
            case DOWN -> square(face, x1, z1, x2, z2, y1);
            case UP -> square(face, x1, 1 - z2, x2, 1 - z1, 1 - y2);
            case NORTH -> square(face, 1 - x2, y1, 1 - x1, y2, z1);
            case SOUTH -> square(face, x1, y1, x2, y2, 1 - z2);
            case WEST -> square(face, z1, y1, z2, y2, x1);
            case EAST -> square(face, 1 - z2, y1, 1 - z1, y2, 1 - x2);
        };

        var colors = new int[] { color, color, color, color };
        // Force Brightness to 15, this is for full bright mode
        output.add(BakedQuads.build(positions, uvs, colors, null, face, -1, texture, true, emissiveMaterial));
    }

    /**
     * Same vertex positions as the Fabric {@code QuadEmitter.square} that the 1.21 version used.
     */
    private static float[][] square(EnumFacing nominalFace, float left, float bottom, float right, float top,
            float depth) {
        if (Math.abs(depth) < 1e-5f) {
            depth = 0; // avoid any inconsistency for face quads
        }

        switch (nominalFace) {
            case UP:
                depth = 1 - depth;
                top = 1 - top;
                bottom = 1 - bottom;

            case DOWN:
                return new float[][] {
                        { left, depth, top },
                        { left, depth, bottom },
                        { right, depth, bottom },
                        { right, depth, top } };

            case EAST:
                depth = 1 - depth;
                left = 1 - left;
                right = 1 - right;

            case WEST:
                return new float[][] {
                        { depth, top, left },
                        { depth, bottom, left },
                        { depth, bottom, right },
                        { depth, top, right } };

            case SOUTH:
                depth = 1 - depth;
                left = 1 - left;
                right = 1 - right;

            case NORTH:
            default:
                return new float[][] {
                        { 1 - left, top, depth },
                        { 1 - left, bottom, depth },
                        { 1 - right, bottom, depth },
                        { 1 - right, top, depth } };
        }
    }

    private void setFaceUV(EnumFacing face, float[][] uvs, UvVector uv) {
        var rotation = uvRotations[face.ordinal()];

        if (flipU[face.ordinal()]) {
            var tmp = uv.u1;
            uv.u1 = uv.u2;
            uv.u2 = tmp;
        }

        if (flipV[face.ordinal()]) {
            var tmp = uv.v1;
            uv.v1 = uv.v2;
            uv.v2 = tmp;
        }

        switch (face) {
            case DOWN, UP -> {
                uvs[(0 + 4 - rotation) % 4] = new float[] { uv.u1, uv.v1 };
                uvs[(1 + 4 - rotation) % 4] = new float[] { uv.u1, uv.v2 };
                uvs[(2 + 4 - rotation) % 4] = new float[] { uv.u2, uv.v2 };
                uvs[(3 + 4 - rotation) % 4] = new float[] { uv.u2, uv.v1 };
            }
            case NORTH, SOUTH, WEST, EAST -> {
                uvs[(0 + 4 - rotation) % 4] = new float[] { uv.u1, uv.v2 };
                uvs[(1 + 4 - rotation) % 4] = new float[] { uv.u1, uv.v1 };
                uvs[(2 + 4 - rotation) % 4] = new float[] { uv.u2, uv.v1 };
                uvs[(3 + 4 - rotation) % 4] = new float[] { uv.u2, uv.v2 };
            }
        }
    }

    private UvVector getStandardUv(EnumFacing face, TextureAtlasSprite texture, float x1, float y1, float z1,
            float x2,
            float y2, float z2) {
        UvVector uv = new UvVector();
        if (face.getAxis() != EnumFacing.Axis.Y) {
            uv.v1 = BakedQuads.getV(texture, 1 - y1);
            uv.v2 = BakedQuads.getV(texture, 1 - y2);
        } else {
            uv.v1 = BakedQuads.getV(texture, z1);
            uv.v2 = BakedQuads.getV(texture, z2);
        }

        switch (face) {
            case DOWN, UP, SOUTH -> {
                uv.u1 = BakedQuads.getU(texture, x1);
                uv.u2 = BakedQuads.getU(texture, x2);
            }
            case NORTH -> {
                uv.u1 = BakedQuads.getU(texture, 1 - x2);
                uv.u2 = BakedQuads.getU(texture, 1 - x1);
            }
            case WEST -> {
                uv.u1 = BakedQuads.getU(texture, z1);
                uv.u2 = BakedQuads.getU(texture, z2);
            }
            case EAST -> {
                uv.u1 = BakedQuads.getU(texture, 1 - z2);
                uv.u2 = BakedQuads.getU(texture, 1 - z1);
            }
        }

        return uv;
    }

    public void setTexture(TextureAtlasSprite texture) {
        for (EnumFacing face : EnumFacing.values()) {
            this.textures.put(face, texture);
        }
    }

    public void setTextures(TextureAtlasSprite up, TextureAtlasSprite down, TextureAtlasSprite north,
            TextureAtlasSprite south, TextureAtlasSprite east, TextureAtlasSprite west) {
        this.textures.put(EnumFacing.UP, up);
        this.textures.put(EnumFacing.DOWN, down);
        this.textures.put(EnumFacing.NORTH, north);
        this.textures.put(EnumFacing.SOUTH, south);
        this.textures.put(EnumFacing.EAST, east);
        this.textures.put(EnumFacing.WEST, west);
    }

    public void setTexture(EnumFacing facing, TextureAtlasSprite sprite) {
        this.textures.put(facing, sprite);
    }

    public void setDrawFaces(EnumSet<EnumFacing> drawFaces) {
        this.drawFaces = drawFaces;
    }

    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Sets the vertex color for future vertices to the given RGB value, and forces the alpha component to 255.
     */
    public void setColorRGB(int color) {
        this.setColor(color | 0xFF000000);
    }

    public void setColorRGB(float r, float g, float b) {
        this.setColorRGB((int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255));
    }

    public void setEmissiveMaterial(boolean renderFullBright) {
        this.emissiveMaterial = renderFullBright;
    }

    /**
     * UV coordinates are fractions of the sprite (0..1), as in 1.21.
     */
    public void setCustomUv(EnumFacing facing, float u1, float v1, float u2, float v2) {
        this.customUv.put(facing, new float[] { u1, v1, u2, v2 });
    }

    public void setUvRotation(EnumFacing facing, int rotation) {
        Preconditions.checkArgument(rotation >= 0 && rotation <= 3, "rotation");
        this.uvRotations[facing.ordinal()] = (byte) rotation;
    }

    public List<BakedQuad> getOutput() {
        return this.output;
    }

}
