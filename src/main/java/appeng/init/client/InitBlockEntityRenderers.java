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

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.blockentity.crafting.CraftingMonitorBlockEntity;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.blockentity.misc.CrankBlockEntity;
import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.blockentity.networking.CableBusTESR;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.blockentity.storage.SkyChestBlockEntity;
import appeng.blockentity.storage.SkyStoneTankBlockEntity;
import appeng.client.render.crafting.CraftingMonitorRenderer;
import appeng.client.render.crafting.MolecularAssemblerRenderer;
import appeng.client.render.tesr.ChargerBlockEntityRenderer;
import appeng.client.render.tesr.ChestBlockEntityRenderer;
import appeng.client.render.tesr.CrankRenderer;
import appeng.client.render.tesr.DriveLedBlockEntityRenderer;
import appeng.client.render.tesr.InscriberTESR;
import appeng.client.render.tesr.SkyChestTESR;
import appeng.client.render.tesr.SkyStoneTankBlockEntityRenderer;

/**
 * Binds the block entity renderers (1.21 {@code EntityRenderersEvent.RegisterRenderers}).
 */
@SideOnly(Side.CLIENT)
public final class InitBlockEntityRenderers {

    private InitBlockEntityRenderers() {
    }

    public static void init() {
        ClientRegistry.bindTileEntitySpecialRenderer(CrankBlockEntity.class, new CrankRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(InscriberBlockEntity.class, new InscriberTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(SkyChestBlockEntity.class, new SkyChestTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(ChargerBlockEntity.class, ChargerBlockEntityRenderer.FACTORY.get());
        ClientRegistry.bindTileEntitySpecialRenderer(DriveBlockEntity.class, new DriveLedBlockEntityRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(MEChestBlockEntity.class, new ChestBlockEntityRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(CraftingMonitorBlockEntity.class, new CraftingMonitorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(MolecularAssemblerBlockEntity.class,
                new MolecularAssemblerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(CableBusBlockEntity.class, new CableBusTESR());
        ClientRegistry.bindTileEntitySpecialRenderer(SkyStoneTankBlockEntity.class,
                new SkyStoneTankBlockEntityRenderer());
    }
}
