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

package appeng.client.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.cablebus.FacadeBuilder;
import appeng.core.AppEng;
import appeng.parts.misc.CableAnchorPart;

/**
 * The model class for facades. Since facades wrap existing models, they don't declare any dependencies here other than
 * the cable anchor.
 */
@SideOnly(Side.CLIENT)
public class FacadeItemModel implements BasicUnbakedModel {

    // We use this to get the default item transforms and make our lives easier
    private static final ResourceLocation MODEL_BASE = AppEng.makeId("item/facade_base");

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        IBakedModel bakedBaseModel = BasicUnbakedModel.bakeDependency(MODEL_BASE, format, spriteGetter);
        FacadeBuilder facadeBuilder = new FacadeBuilder(
                location -> BasicUnbakedModel.bakeDependency(location, format, spriteGetter), null);

        return new FacadeDispatcherBakedModel(bakedBaseModel, facadeBuilder);
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        var result = new ArrayList<ResourceLocation>();
        result.add(MODEL_BASE);
        result.addAll(CableAnchorPart.FACADE_MODELS.getModels());
        return result;
    }
}
