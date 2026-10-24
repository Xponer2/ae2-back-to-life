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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.parts.PartModelsInternal;
import appeng.api.util.AEColor;
import appeng.client.render.BasicUnbakedModel;
import appeng.client.render.model.EmissiveModels;
import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.parts.misc.CableAnchorPart;

/**
 * The built-in model for the cable bus block.
 */
@SideOnly(Side.CLIENT)
public class CableBusModel implements BasicUnbakedModel {

    public static final ResourceLocation TRANSLUCENT_FACADE_MODEL = AppEng.makeId("part/translucent_facade");

    @Override
    public Collection<ResourceLocation> getDependencies() {
        PartModelsInternal.freeze();
        var models = new ArrayList<>(PartModelsInternal.getModels());
        models.addAll(CableAnchorPart.FACADE_MODELS.getModels());
        models.add(TRANSLUCENT_FACADE_MODEL);
        return models;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return CableBuilder.getTextures();
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        Map<ResourceLocation, IBakedModel> partModels = this.loadPartModels(state, format, spriteGetter);

        CableBuilder cableBuilder = new CableBuilder(spriteGetter);

        Map<ResourceLocation, IBakedModel> otherModels = new HashMap<>();
        Function<ResourceLocation, IBakedModel> baker = location -> partModels.containsKey(location)
                ? partModels.get(location)
                : otherModels.computeIfAbsent(location,
                        l -> BasicUnbakedModel.bakeDependency(l, format, spriteGetter));

        IBakedModel translucentFacadeModel = baker.apply(TRANSLUCENT_FACADE_MODEL);

        FacadeBuilder facadeBuilder = new FacadeBuilder(baker, translucentFacadeModel);

        // This should normally not be used, but we *have* to provide a particle texture or otherwise damage models will
        // crash
        TextureAtlasSprite particleTexture = cableBuilder.getCoreTexture(CableCoreType.GLASS, AEColor.TRANSPARENT);

        return new CableBusBakedModel(cableBuilder, facadeBuilder, partModels, particleTexture);
    }

    private Map<ResourceLocation, IBakedModel> loadPartModels(IModelState state, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        ImmutableMap.Builder<ResourceLocation, IBakedModel> result = ImmutableMap.builder();

        for (ResourceLocation location : PartModelsInternal.getModels()) {
            IBakedModel bakedModel = BasicUnbakedModel.bakeDependency(location, format, spriteGetter);
            if (bakedModel == null) {
                AELog.warn("Failed to bake part model {}", location);
            } else {
                result.put(location, EmissiveModels.wrapPartModel(location, bakedModel));
            }
        }

        return result.build();
    }
}
