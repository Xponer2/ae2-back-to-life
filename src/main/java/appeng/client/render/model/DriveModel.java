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

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.client.StorageCellModels;
import appeng.client.render.BasicUnbakedModel;
import appeng.init.internal.InitStorageCells;

/**
 * The drive model. The orientation of the drive is applied afterwards by the {@link AutoRotatingBakedModel} around
 * it, so this model is always baked facing north.
 */
@SideOnly(Side.CLIENT)
public class DriveModel implements BasicUnbakedModel {

    private static final ResourceLocation MODEL_BASE = new ResourceLocation(
            "ae2:block/drive/drive_base");
    private static final ResourceLocation MODEL_CELL_EMPTY = new ResourceLocation(
            "ae2:block/drive/drive_cell_empty");

    @Override
    public IBakedModel bake(IModelState modelTransform, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        final Map<Item, IBakedModel> cellModels = new IdentityHashMap<>();

        // Load the base model and the model for each cell model.
        for (var entry : StorageCellModels.models().entrySet()) {
            var cellModel = BasicUnbakedModel.bakeDependency(entry.getValue(), format, spriteGetter);
            cellModels.put(entry.getKey(), cellModel);
        }

        final IBakedModel baseModel = BasicUnbakedModel.bakeDependency(MODEL_BASE, format, spriteGetter);
        final IBakedModel defaultCell = BasicUnbakedModel.bakeDependency(StorageCellModels.getDefaultModel(), format,
                spriteGetter);
        cellModels.put(Items.AIR, BasicUnbakedModel.bakeDependency(MODEL_CELL_EMPTY, format, spriteGetter));

        return new DriveBakedModel(baseModel, cellModels, defaultCell);
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return ImmutableSet.<ResourceLocation>builder().add(StorageCellModels.getDefaultModel())
                .add(MODEL_BASE, MODEL_CELL_EMPTY)
                .addAll(InitStorageCells.getModels())
                .addAll(StorageCellModels.models().values()).build();
    }

}
