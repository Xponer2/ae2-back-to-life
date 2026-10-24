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

package appeng.client.render.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.BakedQuads;
import appeng.decorative.solid.GlassState;

@SideOnly(Side.CLIENT)
public class GlassBakedModel implements IDynamicBakedModel {

    // Alternating textures based on position
    static final ResourceLocation TEXTURE_A = new ResourceLocation("ae2:block/glass/quartz_glass_a");
    static final ResourceLocation TEXTURE_B = new ResourceLocation("ae2:block/glass/quartz_glass_b");
    static final ResourceLocation TEXTURE_C = new ResourceLocation("ae2:block/glass/quartz_glass_c");
    static final ResourceLocation TEXTURE_D = new ResourceLocation("ae2:block/glass/quartz_glass_d");

    // Frame texture
    static final ResourceLocation[] TEXTURES_FRAME = generateTexturesFrame();

    // Generates the required textures for the frame
    private static ResourceLocation[] generateTexturesFrame() {
        var result = new ResourceLocation[15];
        for (int i = 1; i < 16; i++) {
            var bits = Strings.padStart(Integer.toBinaryString(i), 4, '0');
            result[i - 1] = new ResourceLocation("ae2:block/glass/quartz_glass_frame" + bits);
        }
        return result;
    }

    private final TextureAtlasSprite[] glassTextures;

    private final TextureAtlasSprite[] frameTextures;

    public GlassBakedModel(Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        this.glassTextures = new TextureAtlasSprite[] { bakedTextureGetter.apply(TEXTURE_A),
                bakedTextureGetter.apply(TEXTURE_B), bakedTextureGetter.apply(TEXTURE_C),
                bakedTextureGetter.apply(TEXTURE_D) };

        // The first frame texture would be empty, so we simply leave it set to null
        // here
        this.frameTextures = new TextureAtlasSprite[16];
        for (int i = 0; i < TEXTURES_FRAME.length; i++) {
            this.frameTextures[1 + i] = bakedTextureGetter.apply(TEXTURES_FRAME[i]);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long seed,
            ModelData extraData) {

        if (side == null) {
            return Collections.emptyList();
        }

        GlassState glassState = extraData.get(GlassModelData.GLASS_STATE);
        if (glassState == null) {
            glassState = GlassState.DEFAULT;
        }

        // 1.21 reseeds the random source with the position seed for every side
        var rand = new Random(seed);
        int randomOffset = rand.nextInt(4);
        var u = randomOffset / 16f;
        var v = rand.nextInt(4) / 16f;
        int texIdx = (randomOffset + rand.nextInt(4)) % 4;

        if (texIdx < 2) {
            u /= 2;
            v /= 2;
        }

        final TextureAtlasSprite glassTexture = this.glassTextures[texIdx];

        // Render the glass side
        if (glassState.hasAdjacentGlassBlock(side)) { // Skip sides that are connected to another glass block
            return Collections.emptyList();
        }

        final List<BakedQuad> quads = new ArrayList<>(5); // At most 5

        final List<float[]> corners = RenderHelper.getFaceCorners(side);
        quads.add(this.createQuad(side, corners, glassTexture, u, v));

        final int edgeBitmask = glassState.getMask(side);
        final TextureAtlasSprite sideSprite = this.frameTextures[edgeBitmask];

        if (sideSprite != null) {
            quads.add(this.createQuad(side, corners, sideSprite, 0, 0));
        }

        return quads;
    }

    private BakedQuad createQuad(EnumFacing side, List<float[]> corners, TextureAtlasSprite sprite, float uOffset,
            float vOffset) {
        // Apply the u,v shift.
        // This mirrors the logic from OffsetIcon from 1.7
        float u1 = MathHelper.clamp(0 - uOffset, 0, 1);
        float u2 = MathHelper.clamp(1 - uOffset, 0, 1);
        float v1 = MathHelper.clamp(0 - vOffset, 0, 1);
        float v2 = MathHelper.clamp(1 - vOffset, 0, 1);

        var positions = new float[][] { corners.get(0), corners.get(1), corners.get(2), corners.get(3) };
        var uvs = new float[][] {
                { BakedQuads.getU(sprite, u1), BakedQuads.getV(sprite, v1) },
                { BakedQuads.getU(sprite, u1), BakedQuads.getV(sprite, v2) },
                { BakedQuads.getU(sprite, u2), BakedQuads.getV(sprite, v2) },
                { BakedQuads.getU(sprite, u2), BakedQuads.getV(sprite, v1) } };
        var colors = new int[] { -1, -1, -1, -1 };
        return BakedQuads.build(positions, uvs, colors, null, side, -1, sprite, true, false);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.frameTextures[this.frameTextures.length - 1];
    }
}
