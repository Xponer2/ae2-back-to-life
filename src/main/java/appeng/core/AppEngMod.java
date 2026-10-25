/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

/**
 * The FML mod container of AE2. The actual functionality lives in {@link AppEngBase} and its sided implementations
 * {@link AppEngClient} / {@link AppEngServer} (selected by FML's sided proxy).
 */
@Mod(modid = AppEng.MOD_ID, name = AppEng.MOD_NAME, version = AETags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:forge@[14.23.5.2847,);after:jei;after:patchouli;after:theoneprobe;after:waila")
public class AppEngMod {
    @Mod.Instance(AppEng.MOD_ID)
    public static AppEngMod instance;

    @SidedProxy(clientSide = "appeng.core.AppEngClient", serverSide = "appeng.core.AppEngServer")
    public static AppEngBase proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.onServerAboutToStart(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.onServerStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.onServerStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.onServerStopped(event);
    }
}
