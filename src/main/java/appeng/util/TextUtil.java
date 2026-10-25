/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import java.util.List;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * Helpers emulating the fluent text component API of modern Minecraft on 1.12.2 {@link ITextComponent}.
 */
public final class TextUtil {
    private TextUtil() {
    }

    /**
     * Same as modern {@code Component.withStyle(ChatFormatting...)}: modifies and returns the component.
     */
    public static ITextComponent withStyle(ITextComponent component, TextFormatting... formats) {
        var style = component.getStyle();
        for (var f : formats) {
            applyFormatting(style, f);
        }
        return component;
    }

    /**
     * A style with a color (1.21 {@code Style.EMPTY.withColor(...)}). 1.12.2 tooltip lines aren't italic by default, so
     * 1.21's {@code withItalic(false)} needs nothing.
     */
    public static Style style(TextFormatting color) {
        return new Style().setColor(color);
    }

    public static Style style(TextFormatting color, boolean italic) {
        return new Style().setColor(color).setItalic(italic);
    }

    /**
     * 1.12.2 text only has the 16 chat colors: the closest one to an RGB color.
     */
    public static Style style(int rgb) {
        return style(nearestColor(rgb));
    }

    public static ITextComponent withStyle(ITextComponent component, Style style) {
        var target = component.getStyle();
        if (style.getColor() != null) {
            target.setColor(style.getColor());
        }
        if (style.getBold()) {
            target.setBold(true);
        }
        if (style.getItalic()) {
            target.setItalic(true);
        }
        if (style.getUnderlined()) {
            target.setUnderlined(true);
        }
        if (style.getStrikethrough()) {
            target.setStrikethrough(true);
        }
        if (style.getObfuscated()) {
            target.setObfuscated(true);
        }
        if (style.getClickEvent() != null) {
            target.setClickEvent(style.getClickEvent());
        }
        if (style.getHoverEvent() != null) {
            target.setHoverEvent(style.getHoverEvent());
        }
        return component;
    }

    /**
     * 1.12.2 only knows the 16 formatting colors: the closest one is used.
     */
    public static ITextComponent withColor(ITextComponent component, int rgb) {
        component.getStyle().setColor(nearestColor(rgb));
        return component;
    }

    public static void applyFormatting(Style style, TextFormatting f) {
        if (f.isColor()) {
            style.setColor(f);
        } else {
            switch (f) {
                case BOLD -> style.setBold(true);
                case ITALIC -> style.setItalic(true);
                case UNDERLINE -> style.setUnderlined(true);
                case STRIKETHROUGH -> style.setStrikethrough(true);
                case OBFUSCATED -> style.setObfuscated(true);
                case RESET -> {
                    style.setColor(null);
                    style.setBold(false);
                    style.setItalic(false);
                    style.setUnderlined(false);
                    style.setStrikethrough(false);
                    style.setObfuscated(false);
                }
                default -> {
                }
            }
        }
    }

    private static final int[] FORMAT_COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA,
            0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
    private static final TextFormatting[] FORMATS = { TextFormatting.BLACK, TextFormatting.DARK_BLUE,
            TextFormatting.DARK_GREEN, TextFormatting.DARK_AQUA, TextFormatting.DARK_RED,
            TextFormatting.DARK_PURPLE, TextFormatting.GOLD, TextFormatting.GRAY, TextFormatting.DARK_GRAY,
            TextFormatting.BLUE, TextFormatting.GREEN, TextFormatting.AQUA, TextFormatting.RED,
            TextFormatting.LIGHT_PURPLE, TextFormatting.YELLOW, TextFormatting.WHITE };

    public static TextFormatting nearestColor(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < FORMAT_COLORS.length; i++) {
            int c = FORMAT_COLORS[i];
            long dr = r - ((c >> 16) & 0xFF), dg = g - ((c >> 8) & 0xFF), db = b - (c & 0xFF);
            long dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return FORMATS[best];
    }

    public static int getColor(TextFormatting formatting) {
        for (int i = 0; i < FORMATS.length; i++) {
            if (FORMATS[i] == formatting) {
                return FORMAT_COLORS[i];
            }
        }
        return 0xFFFFFF;
    }

    public static ITextComponent literal(String text) {
        return new TextComponentString(text);
    }

    public static ITextComponent translatable(String key, Object... args) {
        return new TextComponentTranslation(key, args);
    }

    public static ITextComponent empty() {
        return new TextComponentString("");
    }

    /**
     * Same as modern {@code Component.append}: adds a sibling and returns the component.
     */
    public static ITextComponent append(ITextComponent component, ITextComponent sibling) {
        return component.appendSibling(sibling);
    }

    public static ITextComponent append(ITextComponent component, String sibling) {
        return component.appendText(sibling);
    }

    /**
     * Joins components with a separator (modern ComponentUtils.formatList).
     */
    public static ITextComponent join(List<? extends ITextComponent> components, ITextComponent separator) {
        var result = new TextComponentString("");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                result.appendSibling(separator.createCopy());
            }
            result.appendSibling(components.get(i));
        }
        return result;
    }

    public static String getString(ITextComponent component) {
        return component.getUnformattedText();
    }
}
