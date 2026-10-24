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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;
import appeng.items.storage.StorageCellTooltipComponent;

/**
 * Tooltip images (1.21 TooltipComponent + ClientTooltipComponent) on top of the 1.12.2 tooltip renderer: the space
 * for the image is reserved with placeholder lines, and the image is drawn in {@link RenderTooltipEvent.PostText} at
 * the position of the first placeholder.
 * <p/>
 * Use {@link #addPlaceholders} before handing the lines to the tooltip renderer (GuiUtils.drawHoveringText), with the
 * image returned from {@link #create}.
 */
@SideOnly(Side.CLIENT)
public final class TooltipImages {

    // A line that renders nothing, marks where the image goes
    private static final String MARKER = "§r§r§r";

    private static final Map<Class<?>, Function<Object, ClientTooltipImage>> FACTORIES = new HashMap<>();

    @Nullable
    private static ClientTooltipImage pending;

    static {
        register(StorageCellTooltipComponent.class, StorageCellClientTooltipComponent::new);
    }

    private TooltipImages() {
    }

    @SuppressWarnings("unchecked")
    public static <T> void register(Class<T> dataClass, Function<T, ClientTooltipImage> factory) {
        FACTORIES.put(dataClass, data -> factory.apply((T) data));
    }

    /**
     * @return The client image for the given tooltip data (1.21 TooltipComponent), or null if there is none.
     */
    @Nullable
    public static ClientTooltipImage create(@Nullable Object data) {
        if (data == null) {
            return null;
        }
        for (var entry : FACTORIES.entrySet()) {
            if (entry.getKey().isInstance(data)) {
                return entry.getValue().apply(data);
            }
        }
        return null;
    }

    /**
     * Reserves the space of the image below the given lines and remembers the image for the next tooltip that is
     * rendered.
     */
    public static List<String> addPlaceholders(List<String> lines, @Nullable ClientTooltipImage image) {
        if (image == null || image.getHeight() <= 0) {
            return lines;
        }
        var font = Minecraft.getMinecraft().fontRenderer;
        var result = new ArrayList<>(lines);
        int lineCount = Math.max(1, (image.getHeight() + 9) / 10);
        // The first placeholder is padded with spaces to reserve the width of the image
        var widthPadding = new StringBuilder(MARKER);
        while (font.getStringWidth(widthPadding.toString()) < image.getWidth(font)) {
            widthPadding.append(' ');
        }
        result.add(widthPadding.toString());
        for (int i = 1; i < lineCount; i++) {
            result.add(MARKER);
        }
        pending = image;
        return result;
    }

    @SubscribeEvent
    public static void onTooltipText(RenderTooltipEvent.PostText event) {
        var image = pending;
        pending = null;
        if (image == null) {
            return;
        }

        var lines = event.getLines();
        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(MARKER)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }

        FontRenderer font = event.getFontRenderer();
        int x = event.getX();
        // Like GuiUtils.drawHoveringText: 10px per line, 2px gap below the title
        int y = event.getY() + index * 10 + (index > 0 ? 2 : 0);
        image.renderText(font, x, y);
        image.renderImage(font, x, y, new GuiGraphics(Minecraft.getMinecraft()));
    }
}
