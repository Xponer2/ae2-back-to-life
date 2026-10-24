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

package appeng.client.render;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;

/**
 * An unbaked model that has standard models as a dependency and produces a custom baked model as a result.
 * <p/>
 * In 1.12.2 this is an {@link IModel}: {@link #getDependencies()} are loaded (and their textures stitched) by the model
 * loader, {@link #getTextures()} lists the additional textures that the baked model uses directly.
 */
@SideOnly(Side.CLIENT)
public interface BasicUnbakedModel extends IModel {
    @Override
    default Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    default Collection<ResourceLocation> getTextures() {
        return Collections.emptyList();
    }

    @Override
    default IModelState getDefaultState() {
        return TRSRTransformation.identity();
    }

    /**
     * Bakes one of the {@link #getDependencies() dependencies} (1.21 {@code baker.bake(location, state)}). Logs and
     * returns the missing model if the model can't be loaded.
     */
    static IBakedModel bakeDependency(ResourceLocation location, IModelState state, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        IModel model;
        try {
            model = ModelLoaderRegistry.getModel(location);
        } catch (Exception e) {
            AELog.error(e, "Unable to load model " + location);
            model = ModelLoaderRegistry.getMissingModel();
        }
        return model.bake(state, format, spriteGetter);
    }

    /**
     * Same as {@link #bakeDependency(ResourceLocation, IModelState, VertexFormat, Function)} with the model's own
     * default state.
     */
    static IBakedModel bakeDependency(ResourceLocation location, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        IModel model;
        try {
            model = ModelLoaderRegistry.getModel(location);
        } catch (Exception e) {
            AELog.error(e, "Unable to load model " + location);
            model = ModelLoaderRegistry.getMissingModel();
        }
        return model.bake(model.getDefaultState(), format, spriteGetter);
    }
}
