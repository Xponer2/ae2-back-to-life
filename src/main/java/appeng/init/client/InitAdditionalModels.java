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

package appeng.init.client;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.crafting.MolecularAssemblerRenderer;
import appeng.client.render.tesr.CrankRenderer;

/**
 * Registers any JSON model files with Minecraft that are not referenced via blockstates or item IDs.
 * <p/>
 * 1.12.2 only loads models reachable from blockstates and items, so these are loaded by hand: their textures are added
 * to the block atlas in {@code TextureStitchEvent.Pre}, and the baked models are put into the model registry as
 * {@code <model>#standalone} in {@code ModelBakeEvent}, from where the renderers get them through the model manager.
 */
@SideOnly(Side.CLIENT)
public final class InitAdditionalModels {

    private InitAdditionalModels() {
    }

    /**
     * Same as 1.21 {@code ModelResourceLocation.standalone}.
     */
    public static ModelResourceLocation standalone(ResourceLocation model) {
        return new ModelResourceLocation(model, "standalone");
    }

    public static List<ModelResourceLocation> getModels() {
        return Arrays.asList(MolecularAssemblerRenderer.LIGHTS_MODEL, CrankRenderer.BASE_MODEL,
                CrankRenderer.HANDLE_MODEL);
    }

    static void registerTextures(TextureMap map) {
        for (var location : getModels()) {
            var model = loadModel(location);
            for (var texture : model.getTextures()) {
                map.registerSprite(texture);
            }
        }
    }

    static void bakeModels(IRegistry<ModelResourceLocation, IBakedModel> modelRegistry) {
        for (var location : getModels()) {
            var model = loadModel(location);
            var bakedModel = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM,
                    ModelLoader.defaultTextureGetter());
            modelRegistry.putObject(location, bakedModel);
        }
    }

    private static IModel loadModel(ModelResourceLocation location) {
        var modelId = new ResourceLocation(location.getNamespace(), location.getPath());
        return ModelLoaderRegistry.getModelOrLogError(modelId, "Unable to load additional model " + modelId);
    }
}
