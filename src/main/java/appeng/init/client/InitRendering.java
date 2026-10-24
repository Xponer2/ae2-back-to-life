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

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.CopperTextures;
import appeng.client.render.TooltipImages;
import appeng.client.render.effects.AEParticles;
import appeng.client.render.model.AEModelData;
import appeng.client.render.model.AutoRotatingBakedModel;
import appeng.client.render.model.EmissiveModels;
import appeng.client.render.overlay.OverlayManager;
import appeng.core.AppEng;
import appeng.entity.TinyTNTPrimedEntity;
import appeng.entity.TinyTNTPrimedRenderer;
import appeng.hooks.RenderBlockOutlineHook;

/**
 * Entry point of the client rendering setup: {@link #preInit()} is called once from the client proxy's
 * {@code FMLPreInitializationEvent}. Everything else is driven by the Forge client events registered here.
 */
@SideOnly(Side.CLIENT)
public final class InitRendering {

    private InitRendering() {
    }

    public static void preInit() {
        InitBuiltInModels.init();
        InitBlockEntityRenderers.init();
        RenderingRegistry.registerEntityRenderingHandler(TinyTNTPrimedEntity.class, TinyTNTPrimedRenderer::new);
        RenderBlockOutlineHook.install();
        MinecraftForge.EVENT_BUS.register(InitRendering.class);
        MinecraftForge.EVENT_BUS.register(OverlayManager.getInstance());
        MinecraftForge.EVENT_BUS.register(TooltipImages.class);
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        InitItemModelsProperties.init();
        InitStateMappers.init();
        InitItemModels.init();
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        var map = event.getMap();
        if (map != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        AEParticles.registerSprites(map);
        InitPartTextures.registerTextures(map);
        InitAdditionalModels.registerTextures(map);
        CopperTextures.registerSprites(map);
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        var modelRegistry = event.getModelRegistry();
        InitAdditionalModels.bakeModels(modelRegistry);
        customizeBlockModels(modelRegistry);
    }

    @SubscribeEvent
    public static void onBlockColors(ColorHandlerEvent.Block event) {
        InitBlockColors.init(event.getBlockColors());
    }

    @SubscribeEvent
    public static void onItemColors(ColorHandlerEvent.Item event) {
        InitItemColors.init(event.getItemColors());
    }

    /**
     * Wraps the block models of AE2: emissive faces are made full-bright, and blocks whose orientation is stored in
     * their block entity get the {@link AutoRotatingBakedModel}.
     */
    private static void customizeBlockModels(IRegistry<ModelResourceLocation, IBakedModel> modelRegistry) {
        IBakedModel missingModel = modelRegistry.getObject(new ModelResourceLocation("builtin/missing", "missing"));

        for (ModelResourceLocation location : new ArrayList<>(modelRegistry.getKeys())) {
            if (!AppEng.MOD_ID.equals(location.getNamespace()) || "inventory".equals(location.getVariant())
                    || "standalone".equals(location.getVariant())) {
                continue;
            }

            var blockId = new ResourceLocation(location.getNamespace(), location.getPath());
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null || !blockId.equals(block.getRegistryName())) {
                continue;
            }

            IBakedModel model = modelRegistry.getObject(location);
            // Don't customize the missing model. This causes Forge to swallow exceptions
            if (model == null || model == missingModel) {
                continue;
            }

            IBakedModel newModel = EmissiveModels.wrapBlockModel(blockId, model);
            if (isOrientedByBlockEntity(block) && shouldAutoRotate(blockId, location.getVariant())) {
                newModel = new AutoRotatingBakedModel(newModel);
            }

            if (newModel != model) {
                modelRegistry.putObject(location, newModel);
            }
        }
    }

    private static boolean isOrientedByBlockEntity(Block block) {
        return block.getBlockState() instanceof ExtendedBlockState extendedBlockState
                && extendedBlockState.getUnlistedProperties().contains(AEModelData.ORIENTATION);
    }

    /**
     * The formed crafting monitor model applies the orientation of the monitor itself (1.21 doesn't rotate the formed
     * variants either).
     */
    private static boolean shouldAutoRotate(ResourceLocation blockId, String variant) {
        return !(blockId.getPath().equals("crafting_monitor") && variant.contains("formed=true"));
    }
}
