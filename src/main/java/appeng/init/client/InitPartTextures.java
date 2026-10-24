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

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;
import appeng.core.AppEng;

/**
 * 1.21 adds the whole {@code textures/part/} folder to the block atlas ({@code atlases/blocks.json}). 1.12.2 only
 * stitches textures referenced by models, so the folder is listed from the mod's resources and every texture is
 * registered in {@code TextureStitchEvent.Pre}.
 */
@SideOnly(Side.CLIENT)
public final class InitPartTextures {

    private static final String FOLDER = "assets/" + AppEng.MOD_ID + "/textures/part/";

    private InitPartTextures() {
    }

    public static void registerTextures(TextureMap map) {
        for (var texture : findTextures()) {
            map.registerSprite(texture);
        }
    }

    static List<ResourceLocation> findTextures() {
        List<String> files;
        try {
            files = listFiles();
        } catch (Exception e) {
            AELog.warn(e, "Failed to list the part textures");
            return Collections.emptyList();
        }

        List<ResourceLocation> result = new ArrayList<>();
        for (var file : files) {
            if (file.endsWith(".png")) {
                var path = file.substring(0, file.length() - ".png".length());
                result.add(AppEng.makeId("part/" + path));
            }
        }
        return result;
    }

    /**
     * @return The paths of all files in the part texture folder, relative to it, with forward slashes.
     */
    private static List<String> listFiles() throws Exception {
        URL url = InitPartTextures.class.getClassLoader().getResource(FOLDER);
        if (url == null) {
            // Jars without directory entries: list the mod jar itself
            var mod = Loader.instance().getIndexedModList().get(AppEng.MOD_ID);
            if (mod != null && mod.getSource() != null && mod.getSource().isFile()) {
                try (var jar = new JarFile(mod.getSource())) {
                    return listJar(jar);
                }
            }
            return Collections.emptyList();
        }

        if ("jar".equals(url.getProtocol())) {
            var connection = (JarURLConnection) url.openConnection();
            connection.setUseCaches(false);
            try (var jar = connection.getJarFile()) {
                return listJar(jar);
            }
        }

        Path folder = Paths.get(url.toURI());
        try (Stream<Path> stream = Files.walk(folder)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> folder.relativize(p).toString().replace(File.separatorChar, '/'))
                    .collect(Collectors.toList());
        }
    }

    private static List<String> listJar(JarFile jar) {
        List<String> result = new ArrayList<>();
        var entries = jar.entries();
        while (entries.hasMoreElements()) {
            var name = entries.nextElement().getName();
            if (name.startsWith(FOLDER) && !name.endsWith("/")) {
                result.add(name.substring(FOLDER.length()));
            }
        }
        return result;
    }
}
