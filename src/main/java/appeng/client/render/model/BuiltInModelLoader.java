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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Provides the models that are created in code (1.21 BuiltInModelHooks). The JSON files of these models are empty
 * ({@code {}}), the model loader asks this loader first.
 * <p/>
 * Model locations are as the model loader requests them: {@code ae2:block/cable_bus}, {@code ae2:item/facade}, ...
 */
@SideOnly(Side.CLIENT)
public final class BuiltInModelLoader implements ICustomModelLoader {

    private static final BuiltInModelLoader INSTANCE = new BuiltInModelLoader();

    private final Map<ResourceLocation, Supplier<? extends IModel>> builtInModels = new HashMap<>();

    private BuiltInModelLoader() {
    }

    public static BuiltInModelLoader getInstance() {
        return INSTANCE;
    }

    public void addBuiltInModel(ResourceLocation id, Supplier<? extends IModel> modelFactory) {
        if (builtInModels.put(id, modelFactory) != null) {
            throw new IllegalStateException("Duplicate built-in model ID: " + id);
        }
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return builtInModels.containsKey(normalize(modelLocation));
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) {
        // A new instance for each reload, so that the models pick up the current part model registrations etc.
        return builtInModels.get(normalize(modelLocation)).get();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }

    /**
     * Strips a "models/" prefix if the location came in as a file path.
     */
    private static ResourceLocation normalize(ResourceLocation location) {
        var path = location.getPath();
        if (path.startsWith("models/")) {
            return new ResourceLocation(location.getNamespace(), path.substring("models/".length()));
        }
        return location;
    }
}
