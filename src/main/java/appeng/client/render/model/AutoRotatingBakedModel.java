/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.model;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.client.render.BakedQuads;
import appeng.client.render.DelegateBakedModel;

/**
 * Rotates the quads of the wrapped model according to the {@link AEModelData#ORIENTATION} of the block.
 * <p/>
 * In 1.21 the orientation (facing + spin) is part of the block state and the blockstate JSON rotates the model. In
 * 1.12.2 the orientation is stored in the block entity, the blockstate JSON only has the unrotated (north, spin 0)
 * model and this model applies the same rotation as the 1.21 variants ({@code x}, {@code y} and {@code ae2:z}).
 */
@SideOnly(Side.CLIENT)
public class AutoRotatingBakedModel extends DelegateBakedModel {

    private final LoadingCache<AutoRotatingCacheKey, List<BakedQuad>> quadCache;

    public AutoRotatingBakedModel(IBakedModel parent) {
        super(parent);
        // 24 orientations * 7 sides (DUNSWE + null) for a few states
        this.quadCache = CacheBuilder.newBuilder().maximumSize(24 * 7 * 8)
                .build(new CacheLoader<AutoRotatingCacheKey, List<BakedQuad>>() {
                    @Override
                    public List<BakedQuad> load(AutoRotatingCacheKey key) {
                        return getRotatedQuads(key.getBlockState(), key.getSide(), key.getOrientation(), 0);
                    }
                });
    }

    private List<BakedQuad> getRotatedQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
            BlockOrientation orientation, long rand) {
        // The quads that end up on the requested side come from the side of the unrotated model
        var originalSide = side != null ? BakedQuads.resultingRotate(orientation, side) : null;
        var original = getBaseModel().getQuads(state, originalSide, rand);
        return BakedQuads.rotate(original, orientation);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        var orientation = AEModelData.getOrientation(state);
        if (orientation == null || orientation.isRedundant()) {
            return getBaseModel().getQuads(state, side, rand);
        }

        // Models without model data only depend on the (listed) block state, so their rotated quads can be cached.
        // Models with model data (drive cells etc.) are rotated every time.
        if (state instanceof IExtendedBlockState extendedState
                && AEModelData.getModelData(state) == ModelData.EMPTY
                && !(getBaseModel() instanceof IDynamicBakedModel)) {
            return quadCache.getUnchecked(new AutoRotatingCacheKey(extendedState.getClean(), orientation, side));
        }

        return getRotatedQuads(state, side, orientation, rand);
    }
}
