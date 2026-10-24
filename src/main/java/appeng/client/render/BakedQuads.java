/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;

/**
 * Helpers to build and transform {@link BakedQuad}s in 1.12.2, where the 1.21 code used the Fabric/NeoForge quad
 * emitters.
 * <p/>
 * Quads are built in {@link DefaultVertexFormats#ITEM} (the format Forge bakes JSON models in), emissive quads in
 * {@link DefaultVertexFormats#BLOCK} with a full-bright lightmap: the Forge lighting pipeline uses the maximum of the
 * quad's lightmap and the world light for quads that carry one (the replacement for {@code neoforge_data} with
 * {@code block_light: 15, sky_light: 15}). Both formats have 7 ints per vertex, like the vanilla block buffer.
 */
@SideOnly(Side.CLIENT)
public final class BakedQuads {

    /**
     * Packed lightmap value for block light 15 / sky light 15 (240 in both shorts).
     */
    public static final int FULL_BRIGHT_LIGHTMAP = 0x00F000F0;

    public static final VertexFormat FORMAT = DefaultVertexFormats.ITEM;
    public static final VertexFormat EMISSIVE_FORMAT = DefaultVertexFormats.BLOCK;

    // Exact 90° rotation matrices (rows) for each block orientation, see BlockOrientation
    private static final EnumMap<BlockOrientation, int[][]> MATRICES = new EnumMap<>(BlockOrientation.class);
    private static final EnumMap<BlockOrientation, EnumFacing[]> ROTATED_FACES = new EnumMap<>(BlockOrientation.class);

    static {
        for (var orientation : BlockOrientation.values()) {
            // Same as the 1.21 quaternion rotateYXZ(-angleY, -angleX, -angleZ): v' = Ry * Rx * Rz * v
            var m = mul(mul(rotY(-orientation.getAngleY()), rotX(-orientation.getAngleX())),
                    rotZ(-orientation.getAngleZ()));
            MATRICES.put(orientation, m);
            var faces = new EnumFacing[6];
            for (var facing : EnumFacing.VALUES) {
                var vec = facing.getDirectionVec();
                int x = m[0][0] * vec.getX() + m[0][1] * vec.getY() + m[0][2] * vec.getZ();
                int y = m[1][0] * vec.getX() + m[1][1] * vec.getY() + m[1][2] * vec.getZ();
                int z = m[2][0] * vec.getX() + m[2][1] * vec.getY() + m[2][2] * vec.getZ();
                faces[facing.ordinal()] = EnumFacing.getFacingFromVector(x, y, z);
            }
            ROTATED_FACES.put(orientation, faces);
        }
    }

    private BakedQuads() {
    }

    /**
     * @return The side the given side of the unrotated model ends up on.
     */
    public static EnumFacing rotate(BlockOrientation orientation, EnumFacing side) {
        return ROTATED_FACES.get(orientation)[side.ordinal()];
    }

    /**
     * @return The side of the unrotated model that ends up on the given side.
     */
    public static EnumFacing resultingRotate(BlockOrientation orientation, EnumFacing side) {
        var faces = ROTATED_FACES.get(orientation);
        for (var facing : EnumFacing.VALUES) {
            if (faces[facing.ordinal()] == side) {
                return facing;
            }
        }
        return side;
    }

    /**
     * Rotates the quads around the block center (0.5, 0.5, 0.5) from the default orientation (front north, top up) to
     * the given one.
     */
    public static List<BakedQuad> rotate(List<BakedQuad> quads, BlockOrientation orientation) {
        if (orientation.isRedundant() || quads.isEmpty()) {
            return quads;
        }
        var result = new ArrayList<BakedQuad>(quads.size());
        for (var quad : quads) {
            result.add(rotate(quad, orientation));
        }
        return result;
    }

    public static BakedQuad rotate(BakedQuad quad, BlockOrientation orientation) {
        if (orientation.isRedundant()) {
            return quad;
        }
        var m = MATRICES.get(orientation);
        var format = quad.getFormat();
        var data = quad.getVertexData().clone();
        var tmp = new float[4];
        for (int e = 0; e < format.getElementCount(); e++) {
            var usage = format.getElement(e).getUsage();
            boolean position = usage == VertexFormatElement.EnumUsage.POSITION;
            if (!position && usage != VertexFormatElement.EnumUsage.NORMAL) {
                continue;
            }
            for (int v = 0; v < 4; v++) {
                LightUtil.unpack(data, tmp, format, v, e);
                float x = tmp[0], y = tmp[1], z = tmp[2];
                if (position) {
                    x -= 0.5f;
                    y -= 0.5f;
                    z -= 0.5f;
                }
                float rx = m[0][0] * x + m[0][1] * y + m[0][2] * z;
                float ry = m[1][0] * x + m[1][1] * y + m[1][2] * z;
                float rz = m[2][0] * x + m[2][1] * y + m[2][2] * z;
                if (position) {
                    rx += 0.5f;
                    ry += 0.5f;
                    rz += 0.5f;
                }
                tmp[0] = rx;
                tmp[1] = ry;
                tmp[2] = rz;
                LightUtil.pack(tmp, data, format, v, e);
            }
        }
        var face = quad.getFace() != null ? rotate(orientation, quad.getFace()) : null;
        return new BakedQuad(data, quad.getTintIndex(), face, quad.getSprite(), quad.shouldApplyDiffuseLighting(),
                format);
    }

    /**
     * Moves the quad by the given offset (in block units).
     */
    public static BakedQuad translate(BakedQuad quad, float x, float y, float z) {
        var format = quad.getFormat();
        var data = quad.getVertexData().clone();
        var tmp = new float[4];
        for (int e = 0; e < format.getElementCount(); e++) {
            if (format.getElement(e).getUsage() != VertexFormatElement.EnumUsage.POSITION) {
                continue;
            }
            for (int v = 0; v < 4; v++) {
                LightUtil.unpack(data, tmp, format, v, e);
                tmp[0] += x;
                tmp[1] += y;
                tmp[2] += z;
                LightUtil.pack(tmp, data, format, v, e);
            }
        }
        return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
                quad.shouldApplyDiffuseLighting(), format);
    }

    /**
     * Converts the quad into one that is always rendered with full brightness.
     */
    public static BakedQuad makeEmissive(BakedQuad quad) {
        var from = quad.getFormat();
        if (from == EMISSIVE_FORMAT && isEmissive(quad)) {
            return quad;
        }
        var to = EMISSIVE_FORMAT;
        var data = new int[to.getIntegerSize() * 4];
        var tmp = new float[4];
        for (int e = 0; e < to.getElementCount(); e++) {
            var element = to.getElement(e);
            int fromIndex = findElement(from, element);
            for (int v = 0; v < 4; v++) {
                if (element.getUsage() == VertexFormatElement.EnumUsage.UV && element.getIndex() == 1) {
                    continue; // lightmap, set below
                }
                if (fromIndex >= 0) {
                    LightUtil.unpack(quad.getVertexData(), tmp, from, v, fromIndex);
                } else if (element.getUsage() == VertexFormatElement.EnumUsage.COLOR) {
                    tmp[0] = tmp[1] = tmp[2] = tmp[3] = 1;
                } else {
                    tmp[0] = tmp[1] = tmp[2] = tmp[3] = 0;
                }
                LightUtil.pack(tmp, data, to, v, e);
            }
        }
        int lightmapOffset = getLightmapIntOffset(to);
        for (int v = 0; v < 4; v++) {
            data[v * to.getIntegerSize() + lightmapOffset] = FULL_BRIGHT_LIGHTMAP;
        }
        return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
                quad.shouldApplyDiffuseLighting(), to);
    }

    public static List<BakedQuad> makeEmissive(List<BakedQuad> quads) {
        var result = new ArrayList<BakedQuad>(quads.size());
        for (var quad : quads) {
            result.add(makeEmissive(quad));
        }
        return result;
    }

    public static boolean isEmissive(BakedQuad quad) {
        var format = quad.getFormat();
        if (format != EMISSIVE_FORMAT) {
            return false;
        }
        return quad.getVertexData()[getLightmapIntOffset(format)] == FULL_BRIGHT_LIGHTMAP;
    }

    /**
     * Builds a quad from the given vertices (in quad vertex order), in {@link #FORMAT} or {@link #EMISSIVE_FORMAT}.
     *
     * @param positions 4 x (x, y, z) in block coordinates (0..1)
     * @param uvs       4 x (u, v) atlas coordinates
     * @param colors    4 x ARGB colors
     * @param normal    the face normal, or null to derive it from {@code face}
     */
    public static BakedQuad build(float[][] positions, float[][] uvs, int[] colors, @Nullable float[] normal,
            @Nullable EnumFacing face, int tintIndex, TextureAtlasSprite sprite, boolean applyDiffuseLighting,
            boolean emissive) {
        var format = emissive ? EMISSIVE_FORMAT : FORMAT;
        var data = new int[format.getIntegerSize() * 4];
        var tmp = new float[4];
        if (normal == null) {
            normal = face != null
                    ? new float[] { face.getXOffset(), face.getYOffset(), face.getZOffset() }
                    : new float[] { 0, 1, 0 };
        }
        for (int e = 0; e < format.getElementCount(); e++) {
            var element = format.getElement(e);
            for (int v = 0; v < 4; v++) {
                switch (element.getUsage()) {
                    case POSITION -> {
                        tmp[0] = positions[v][0];
                        tmp[1] = positions[v][1];
                        tmp[2] = positions[v][2];
                        tmp[3] = 1;
                    }
                    case COLOR -> {
                        int c = colors[v];
                        tmp[0] = (c >> 16 & 0xFF) / 255f;
                        tmp[1] = (c >> 8 & 0xFF) / 255f;
                        tmp[2] = (c & 0xFF) / 255f;
                        tmp[3] = (c >>> 24 & 0xFF) / 255f;
                    }
                    case UV -> {
                        if (element.getIndex() == 0) {
                            tmp[0] = uvs[v][0];
                            tmp[1] = uvs[v][1];
                        } else {
                            tmp[0] = tmp[1] = 0; // lightmap, set below
                        }
                        tmp[2] = tmp[3] = 0;
                    }
                    case NORMAL -> {
                        tmp[0] = normal[0];
                        tmp[1] = normal[1];
                        tmp[2] = normal[2];
                        tmp[3] = 0;
                    }
                    default -> {
                        continue;
                    }
                }
                LightUtil.pack(tmp, data, format, v, e);
            }
        }
        if (emissive) {
            int lightmapOffset = getLightmapIntOffset(format);
            for (int v = 0; v < 4; v++) {
                data[v * format.getIntegerSize() + lightmapOffset] = FULL_BRIGHT_LIGHTMAP;
            }
        }
        return new BakedQuad(data, tintIndex, face, sprite, applyDiffuseLighting, format);
    }

    /**
     * Same as 1.21 {@code sprite.getU(fraction)}: maps 0..1 to the sprite's U range (1.12.2 uses 0..16).
     */
    public static float getU(TextureAtlasSprite sprite, float fraction) {
        return sprite.getInterpolatedU(fraction * 16.0);
    }

    /**
     * Same as 1.21 {@code sprite.getV(fraction)}: maps 0..1 to the sprite's V range (1.12.2 uses 0..16).
     */
    public static float getV(TextureAtlasSprite sprite, float fraction) {
        return sprite.getInterpolatedV(fraction * 16.0);
    }

    private static int findElement(VertexFormat format, VertexFormatElement element) {
        for (int i = 0; i < format.getElementCount(); i++) {
            var other = format.getElement(i);
            if (other.getUsage() == element.getUsage() && other.getIndex() == element.getIndex()) {
                return i;
            }
        }
        return -1;
    }

    private static int getLightmapIntOffset(VertexFormat format) {
        for (int i = 0; i < format.getElementCount(); i++) {
            var element = format.getElement(i);
            if (element.getUsage() == VertexFormatElement.EnumUsage.UV && element.getIndex() == 1) {
                return format.getOffset(i) / 4;
            }
        }
        throw new IllegalArgumentException("Format has no lightmap: " + format);
    }

    private static int[][] rotX(int degrees) {
        int c = cos(degrees), s = sin(degrees);
        return new int[][] { { 1, 0, 0 }, { 0, c, -s }, { 0, s, c } };
    }

    private static int[][] rotY(int degrees) {
        int c = cos(degrees), s = sin(degrees);
        return new int[][] { { c, 0, s }, { 0, 1, 0 }, { -s, 0, c } };
    }

    private static int[][] rotZ(int degrees) {
        int c = cos(degrees), s = sin(degrees);
        return new int[][] { { c, -s, 0 }, { s, c, 0 }, { 0, 0, 1 } };
    }

    private static int cos(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> 1;
            case 90, 270 -> 0;
            case 180 -> -1;
            default -> throw new IllegalArgumentException("Not a multiple of 90: " + degrees);
        };
    }

    private static int sin(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0, 180 -> 0;
            case 90 -> 1;
            case 270 -> -1;
            default -> throw new IllegalArgumentException("Not a multiple of 90: " + degrees);
        };
    }

    private static int[][] mul(int[][] a, int[][] b) {
        var r = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return r;
    }
}
