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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;

/**
 * 1.12.2 only: the textures of {@code ae2:copper_ingot} and {@code ae2:copper_ore}. The
 * repository ships no copper textures: they are made while the block atlas is stitched by recoloring the iron ingot
 * and the specks of the iron ore of the player's own resources (vanilla or resource pack), so no Mojang asset is part
 * of the mod.
 */
@SideOnly(Side.CLIENT)
public final class CopperTextures {

    private static final ResourceLocation IRON_INGOT = new ResourceLocation("minecraft",
            "textures/items/iron_ingot.png");
    private static final ResourceLocation IRON_ORE = new ResourceLocation("minecraft", "textures/blocks/iron_ore.png");
    private static final ResourceLocation STONE = new ResourceLocation("minecraft", "textures/blocks/stone.png");

    /**
     * Copper gradient (dark to light): brightness 0..255 followed by the RGB it maps to.
     */
    private static final int[][] GRADIENT = {
            { 0, 60, 24, 12 },
            { 60, 94, 42, 23 },
            { 110, 150, 70, 40 },
            { 150, 196, 106, 62 },
            { 200, 230, 140, 92 },
            { 255, 255, 206, 176 },
    };

    /**
     * Pixels whose RGB channels differ by at most this much are grey (stone or the shadows around the ore specks).
     */
    private static final int GREY_TOLERANCE = 12;

    /**
     * Vanilla allows at most 4 mipmap levels; the frame array may be longer than the atlas needs.
     */
    private static final int MAX_MIPMAP_LEVELS = 4;

    private CopperTextures() {
    }

    /**
     * Puts the recolored sprites into the block atlas. Called from {@code TextureStitchEvent.Pre}, which runs before
     * the models register their textures, so the models get these sprites instead of loading a png.
     */
    public static void registerSprites(TextureMap map) {
        map.setTextureEntry(new RecoloredSprite("ae2:item/copper_ingot", IRON_INGOT, false));
        map.setTextureEntry(new RecoloredSprite("ae2:block/copper_ore", IRON_ORE, true));
    }

    private static final class RecoloredSprite extends TextureAtlasSprite {
        private final ResourceLocation source;
        private final boolean onlySpecks;

        private RecoloredSprite(String name, ResourceLocation source, boolean onlySpecks) {
            super(name);
            this.source = source;
            this.onlySpecks = onlySpecks;
        }

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
            return true;
        }

        /**
         * Returns false when the sprite was loaded: TextureMap skips stitching the sprite if this returns true.
         */
        @Override
        public boolean load(IResourceManager manager, ResourceLocation location,
                Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
            int[] pixels;
            int size;
            try {
                var image = readImage(manager, source);
                // the first frame if the resource pack animates the texture
                size = Math.min(image.getWidth(), image.getHeight());
                pixels = readPixels(image, size);
            } catch (IOException | RuntimeException e) {
                AELog.warn(e, "Couldn't create the copper texture " + getIconName() + " from " + source);
                return true;
            }

            if (onlySpecks) {
                recolorSpecks(pixels, readStone(manager, size));
            } else {
                recolorAll(pixels);
            }

            setIconWidth(size);
            setIconHeight(size);
            var mipmapLevels = Math.max(MAX_MIPMAP_LEVELS, Minecraft.getMinecraft().gameSettings.mipmapLevels);
            var frame = new int[mipmapLevels + 1][];
            frame[0] = pixels;
            clearFramesTextureData();
            framesTextureData.add(frame);
            return false;
        }
    }

    private static BufferedImage readImage(IResourceManager manager, ResourceLocation location) throws IOException {
        try (IResource resource = manager.getResource(location)) {
            return TextureUtil.readBufferedImage(resource.getInputStream());
        }
    }

    private static int[] readPixels(BufferedImage image, int size) {
        var pixels = new int[size * size];
        image.getRGB(0, 0, size, size, pixels, 0, size);
        return pixels;
    }

    /**
     * The stone the ore texture is drawn on, to tell the specks apart from it. Null if it's missing or has another
     * size than the ore.
     */
    @Nullable
    private static int[] readStone(IResourceManager manager, int size) {
        try {
            var image = readImage(manager, STONE);
            if (image.getWidth() != size || image.getHeight() < size) {
                return null;
            }
            return readPixels(image, size);
        } catch (IOException e) {
            return null;
        }
    }

    private static void recolorAll(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            int alpha = pixels[i] >>> 24;
            if (alpha != 0) {
                pixels[i] = copper(brightness(pixels[i]), alpha);
            }
        }
    }

    /**
     * Recolors the colored pixels that aren't part of the stone and spreads their shades over the middle and bright
     * part of the gradient. The grey stone and the grey shadows of the specks stay.
     */
    private static void recolorSpecks(int[] pixels, @Nullable int[] stone) {
        var specks = new boolean[pixels.length];
        int darkest = 255;
        int brightest = 0;
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            if (pixel >>> 24 == 0 || stone != null && pixel == stone[i] || isGrey(pixel)) {
                continue;
            }
            specks[i] = true;
            darkest = Math.min(darkest, brightness(pixel));
            brightest = Math.max(brightest, brightness(pixel));
        }

        int range = Math.max(1, brightest - darkest);
        for (int i = 0; i < pixels.length; i++) {
            if (specks[i]) {
                int value = 110 + (brightness(pixels[i]) - darkest) * 130 / range;
                pixels[i] = copper(value, pixels[i] >>> 24);
            }
        }
    }

    private static boolean isGrey(int pixel) {
        int r = pixel >> 16 & 0xFF;
        int g = pixel >> 8 & 0xFF;
        int b = pixel & 0xFF;
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= GREY_TOLERANCE;
    }

    private static int brightness(int pixel) {
        return ((pixel >> 16 & 0xFF) + (pixel >> 8 & 0xFF) + (pixel & 0xFF)) / 3;
    }

    private static int copper(int value, int alpha) {
        for (int i = 1; i < GRADIENT.length; i++) {
            int[] from = GRADIENT[i - 1];
            int[] to = GRADIENT[i];
            if (value <= to[0]) {
                float f = (value - from[0]) / (float) (to[0] - from[0]);
                int r = Math.round(from[1] + (to[1] - from[1]) * f);
                int g = Math.round(from[2] + (to[2] - from[2]) * f);
                int b = Math.round(from[3] + (to[3] - from[3]) * f);
                return alpha << 24 | r << 16 | g << 8 | b;
            }
        }
        int[] last = GRADIENT[GRADIENT.length - 1];
        return alpha << 24 | last[1] << 16 | last[2] << 8 | last[3];
    }
}
