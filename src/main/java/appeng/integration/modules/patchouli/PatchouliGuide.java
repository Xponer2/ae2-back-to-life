/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.patchouli;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.AEBaseScreen;
import appeng.core.AppEng;
import appeng.items.tools.GuideItem;

/**
 * Opens AE2's guide, a Patchouli book ({@code ae2:guide}) converted from the 1.21 guidebook. Patchouli is optional, so
 * its API is called by reflection.
 * <p>
 * Guide pages of 1.21 ({@code items-blocks-machines/terminals.md#crafting-terminal-ui}) map to Patchouli entries
 * ({@code ae2:items_blocks_machines/terminals}); the anchor is the {@code anchor} of a page in the entry.
 */
@SideOnly(Side.CLIENT)
public final class PatchouliGuide {
    private static final Logger LOG = LogManager.getLogger(PatchouliGuide.class);

    public static final ResourceLocation BOOK = GuideItem.GUIDE_ID;

    private PatchouliGuide() {
    }

    public static boolean isAvailable() {
        return Loader.isModLoaded("patchouli");
    }

    /**
     * Connects the guide item and the help buttons of the screens to the book. Without Patchouli they stay inactive.
     */
    public static void init() {
        if (!isAvailable()) {
            return;
        }
        GuideItem.setGuideOpener(player -> open(null, null));
        AEBaseScreen.setGuideOpener(PatchouliGuide::openTopic);
    }

    /**
     * @param topic a 1.21 guide page, optionally with an anchor: {@code [namespace:]path.md[#anchor]}
     */
    public static void openTopic(String topic) {
        String page = topic;
        String anchor = null;
        int hash = topic.indexOf('#');
        if (hash >= 0) {
            page = topic.substring(0, hash);
            anchor = topic.substring(hash + 1);
        }
        open(getEntryId(page), anchor);
    }

    /**
     * Patchouli entry of a 1.21 guide page: {@code .md} removed, {@code -} replaced by {@code _}, pages in the root
     * folder are in {@code getting_started}.
     */
    static ResourceLocation getEntryId(String page) {
        String namespace = AppEng.MOD_ID;
        int colon = page.indexOf(':');
        if (colon >= 0) {
            namespace = page.substring(0, colon);
            page = page.substring(colon + 1);
        }
        if (page.endsWith(".md")) {
            page = page.substring(0, page.length() - ".md".length());
        }
        if (page.indexOf('/') < 0) {
            page = "getting_started/" + page;
        }
        return new ResourceLocation(namespace, page.replace('-', '_'));
    }

    /**
     * Opens the book at an entry (and the page with the given anchor), or where it was last closed if entry is null.
     */
    public static void open(@Nullable ResourceLocation entry, @Nullable String anchor) {
        try {
            var api = Class.forName("vazkii.patchouli.api.PatchouliAPI").getField("instance").get(null);
            if (entry != null) {
                try {
                    var openEntry = api.getClass().getMethod("openBookEntry", ResourceLocation.class,
                            ResourceLocation.class, int.class);
                    openEntry.invoke(api, BOOK, entry, findPage(entry, anchor));
                    return;
                } catch (NoSuchMethodException e) {
                    // older Patchouli: open the book only
                }
            }
            api.getClass().getMethod("openBookGUI", ResourceLocation.class).invoke(api, BOOK);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.warn("Failed to open the AE2 guide", e);
        }
    }

    private static int findPage(ResourceLocation entry, @Nullable String anchor) {
        if (anchor == null) {
            return 0;
        }
        var location = new ResourceLocation(entry.getNamespace(),
                "patchouli_books/" + BOOK.getPath() + "/en_us/entries/" + entry.getPath() + ".json");
        try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
                var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            var pages = json.getAsJsonArray("pages");
            for (int i = 0; i < pages.size(); i++) {
                var page = pages.get(i);
                if (page.isJsonObject() && page.getAsJsonObject().has("anchor")
                        && anchor.equals(page.getAsJsonObject().get("anchor").getAsString())) {
                    return i;
                }
            }
        } catch (Exception e) {
            LOG.debug("Guide entry {} not found: {}", location, e.toString());
        }
        return 0;
    }
}
