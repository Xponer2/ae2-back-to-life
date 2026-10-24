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

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stand-in for the NeoForge IDynamicBakedModel: a baked model whose quads depend on {@link ModelData}.
 * <p/>
 * For blocks, the model data comes from the {@link AEModelData#MODEL_DATA} property of the extended block state. The
 * cable bus model passes the model data of parts directly. The 1.21 {@code renderType} parameter is
 * {@link #getRenderLayer()} (null outside of chunk rendering, e.g. for items or the breaking animation).
 */
@SideOnly(Side.CLIENT)
public interface IDynamicBakedModel extends IBakedModel {

    List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand, ModelData data);

    @Override
    default List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return getQuads(state, side, rand, AEModelData.getModelData(state));
    }

    @Override
    default ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    /**
     * @return The block render layer that is currently being built, or null if not rendering a chunk layer.
     */
    @Nullable
    static BlockRenderLayer getRenderLayer() {
        return MinecraftForgeClient.getRenderLayer();
    }

    /**
     * Gets the quads of any baked model for the given model data.
     */
    static List<BakedQuad> getQuads(IBakedModel model, @Nullable IBlockState state, @Nullable EnumFacing side,
            long rand, ModelData data) {
        if (model instanceof IDynamicBakedModel dynamicModel) {
            return dynamicModel.getQuads(state, side, rand, data);
        }
        return model.getQuads(state, side, rand);
    }
}
