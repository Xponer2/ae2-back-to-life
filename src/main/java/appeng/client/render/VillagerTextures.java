/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AppEng;
import appeng.init.InitVillager;

/**
 * The skins of the fluix researcher. A villager skin of this version is the whole villager, while AE2 only has the
 * profession overlay of newer versions: the skin is the villager texture of the game (from the resources of the
 * player, so nothing of the game is shipped) with the overlay drawn on top, made again on every resource reload.
 */
@SideOnly(Side.CLIENT)
public final class VillagerTextures {

    private static boolean registered;

    private VillagerTextures() {
    }

    /**
     * Puts the skins into the texture manager, before the first villager asks for them.
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        var textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.loadTexture(InitVillager.SKIN, new OverlaidTexture(
                new ResourceLocation("minecraft", "textures/entity/villager/villager.png"),
                AppEng.makeId("textures/entity/villager/profession/fluix_researcher.png")));
        textureManager.loadTexture(InitVillager.ZOMBIE_SKIN, new OverlaidTexture(
                new ResourceLocation("minecraft", "textures/entity/zombie_villager/zombie_villager.png"),
                AppEng.makeId("textures/entity/zombie_villager/profession/fluix_researcher.png")));
    }

    /**
     * A texture made of a base texture with an overlay of the same layout on top (the size of the overlay wins).
     */
    private static final class OverlaidTexture extends AbstractTexture {
        private final ResourceLocation base;
        private final ResourceLocation overlay;

        private OverlaidTexture(ResourceLocation base, ResourceLocation overlay) {
            this.base = base;
            this.overlay = overlay;
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) throws IOException {
            deleteGlTexture();
            BufferedImage overlayImage = read(resourceManager, overlay);
            BufferedImage baseImage = read(resourceManager, base);

            int width = overlayImage.getWidth();
            int height = overlayImage.getHeight();
            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = result.createGraphics();
            try {
                graphics.drawImage(baseImage, 0, 0, width, height, null);
                graphics.drawImage(overlayImage, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
            TextureUtil.uploadTextureImageAllocate(getGlTextureId(), result, false, false);
        }

        private static BufferedImage read(IResourceManager resourceManager, ResourceLocation location)
                throws IOException {
            try (IResource resource = resourceManager.getResource(location);
                    InputStream in = resource.getInputStream()) {
                return TextureUtil.readBufferedImage(in);
            }
        }
    }
}
