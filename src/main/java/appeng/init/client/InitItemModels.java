/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.init.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AppEng;

/**
 * Assigns the item models. 1.21 resolves {@code models/item/<id>.json} by the item id, 1.12.2 needs the model location
 * of every item: {@code ae2:<id>#inventory}, which loads {@code models/item/<id>.json} (or the {@code inventory}
 * variant of the blockstate, which the AE2 blockstates don't have).
 * <p/>
 * Called from {@code ModelRegistryEvent}.
 */
@SideOnly(Side.CLIENT)
public final class InitItemModels {

    private InitItemModels() {
    }

    public static void init() {
        for (var item : ForgeRegistries.ITEMS) {
            var id = item.getRegistryName();
            if (id == null || !AppEng.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(id, "inventory"));
        }
    }
}
