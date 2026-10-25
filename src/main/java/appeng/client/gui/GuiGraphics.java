/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.gui;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.PoseStack;
import appeng.client.render.TooltipImages;
import appeng.hooks.GuiGraphicsHooks;

/**
 * Stand-in for the 1.21 GuiGraphics on top of the 1.12.2 immediate mode renderer. Everything is drawn right away with
 * the current GL matrix; {@link #pose()} manipulates that matrix.
 * <p/>
 * Sprites ({@link #blitSprite}) are the 1.21 GUI sprites: {@code ns:path} is the texture
 * {@code ns:textures/gui/sprites/path.png}, drawn according to the {@code gui.scaling} section of its {@code .mcmeta}
 * (stretch, tile or nine_slice), which 1.12.2 doesn't read by itself.
 */
@SideOnly(Side.CLIENT)
public class GuiGraphics {

    private final Minecraft minecraft;
    private final PoseStack pose = new PoseStack();
    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    public GuiGraphics(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public PoseStack pose() {
        return pose;
    }

    public int guiWidth() {
        return new ScaledResolution(minecraft).getScaledWidth();
    }

    public int guiHeight() {
        return new ScaledResolution(minecraft).getScaledHeight();
    }

    /**
     * Nothing is batched in 1.12.2.
     */
    public void flush() {
    }

    public void setColor(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }

    // ------------------------------------------------------------------------------------------ fills

    public void hLine(int minX, int maxX, int y, int color) {
        if (maxX < minX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }
        fill(minX, y, maxX + 1, y + 1, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        if (maxY < minY) {
            int i = minY;
            minY = maxY;
            maxY = i;
        }
        fill(x, minY + 1, x + 1, maxY, color);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        Gui.drawRect(minX, minY, maxX, maxY, color);
        GlStateManager.color(1, 1, 1, 1);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
        pose.pushPose();
        pose.translate(0, 0, z);
        fill(minX, minY, maxX, maxY, color);
        pose.popPose();
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        fillGradient(x1, y1, x2, y2, 0, colorFrom, colorTo);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo) {
        GuiUtils.drawGradientRect(z, x1, y1, x2, y2, colorFrom, colorTo);
    }

    // ------------------------------------------------------------------------------------------ text

    public int drawString(FontRenderer font, @Nullable String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, @Nullable String text, int x, int y, int color, boolean dropShadow) {
        if (text == null) {
            return 0;
        }
        int result = font.drawString(text, (float) x, (float) y, color, dropShadow);
        GlStateManager.color(1, 1, 1, 1);
        return result;
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return drawString(font, text.getFormattedText(), x, y, color, true);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean dropShadow) {
        return drawString(font, text.getFormattedText(), x, y, color, dropShadow);
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        drawString(font, text, x - font.getStringWidth(text) / 2, y, color, true);
    }

    public void drawCenteredString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        drawCenteredString(font, text.getFormattedText(), x, y, color);
    }

    // ------------------------------------------------------------------------------------------ items

    public void renderItem(ItemStack stack, int x, int y) {
        renderItem(null, null, stack, x, y, 0, 0);
    }

    public void renderItem(ItemStack stack, int x, int y, int seed) {
        renderItem(null, null, stack, x, y, seed, 0);
    }

    public void renderItem(ItemStack stack, int x, int y, int seed, int guiOffset) {
        renderItem(null, null, stack, x, y, seed, guiOffset);
    }

    public void renderFakeItem(ItemStack stack, int x, int y) {
        renderItem(null, null, stack, x, y, 0, 0);
    }

    public void renderItem(@Nullable EntityLivingBase entity, @Nullable World level, ItemStack stack, int x, int y,
            int seed) {
        renderItem(entity, level, stack, x, y, seed, 0);
    }

    public void renderItem(@Nullable EntityLivingBase entity, @Nullable World level, ItemStack stack, int x, int y,
            int seed, int guiOffset) {
        if (stack.isEmpty()) {
            return;
        }
        if (level == null) {
            level = minecraft.world;
        }
        // Wrapped generic stacks and encoded patterns (while shift is held) are drawn differently
        if (GuiGraphicsHooks.onRenderGuiItem(this, entity, level, stack, x, y, seed, guiOffset)) {
            return;
        }
        var renderItem = minecraft.getRenderItem();
        pose.pushPose();
        pose.translate(0, 0, guiOffset);
        RenderHelper.enableGUIStandardItemLighting();
        if (entity != null) {
            renderItem.renderItemAndEffectIntoGUI(entity, stack, x, y);
        } else {
            renderItem.renderItemAndEffectIntoGUI(stack, x, y);
        }
        RenderHelper.disableStandardItemLighting();
        pose.popPose();
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y) {
        renderItemDecorations(font, stack, x, y, null);
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y, @Nullable String text) {
        if (stack.isEmpty()) {
            return;
        }
        minecraft.getRenderItem().renderItemOverlayIntoGUI(font, stack, x, y, text);
        GlStateManager.color(1, 1, 1, 1);
    }

    // ------------------------------------------------------------------------------------------ textures

    /**
     * Blits a region of a 256x256 texture.
     */
    public void blit(ResourceLocation texture, int x, int y, int uOffset, int vOffset, int width, int height) {
        blit(texture, x, y, width, height, uOffset, vOffset, width, height, 256, 256);
    }

    public void blit(ResourceLocation texture, int x, int y, float uOffset, float vOffset, int width, int height,
            int textureWidth, int textureHeight) {
        blit(texture, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int width, int height, float uOffset, float vOffset,
            int uWidth, int vHeight, int textureWidth, int textureHeight) {
        innerBlit(texture, x, x + width, y, y + height, 0,
                uOffset / textureWidth, (uOffset + uWidth) / textureWidth,
                vOffset / textureHeight, (vOffset + vHeight) / textureHeight);
    }

    private void innerBlit(ResourceLocation texture, float x1, float x2, float y1, float y2, float z,
            float minU, float maxU, float minV, float maxV) {
        minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        var tessellator = Tessellator.getInstance();
        var buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x1, y2, z).tex(minU, maxV).endVertex();
        buffer.pos(x2, y2, z).tex(maxU, maxV).endVertex();
        buffer.pos(x2, y1, z).tex(maxU, minV).endVertex();
        buffer.pos(x1, y1, z).tex(minU, minV).endVertex();
        tessellator.draw();
    }

    /**
     * Draws a GUI sprite scaled to the given size, according to its scaling (stretch, tile or nine_slice).
     */
    public void blitSprite(ResourceLocation sprite, int x, int y, int width, int height) {
        blitSprite(sprite, x, y, 0, width, height);
    }

    public void blitSprite(ResourceLocation sprite, int x, int y, int z, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        var info = SpriteInfo.get(minecraft, sprite);
        switch (info.type) {
            case "nine_slice" -> blitNineSliced(info, x, y, z, width, height);
            case "tile" -> blitTiled(info, x, y, z, width, height, 0, 0, info.width, info.height, info.width,
                    info.height);
            default -> innerBlit(info.texture, x, x + width, y, y + height, z, 0, 1, 0, 1);
        }
    }

    /**
     * Draws the region (u, v, width, height) of a sprite that is {@code textureWidth} x {@code textureHeight} big.
     */
    public void blitSprite(ResourceLocation sprite, int textureWidth, int textureHeight, int uPosition,
            int vPosition, int x, int y, int width, int height) {
        blitSprite(sprite, textureWidth, textureHeight, uPosition, vPosition, x, y, 0, width, height);
    }

    public void blitSprite(ResourceLocation sprite, int textureWidth, int textureHeight, int uPosition,
            int vPosition, int x, int y, int z, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        var info = SpriteInfo.get(minecraft, sprite);
        innerBlit(info.texture, x, x + width, y, y + height, z,
                (float) uPosition / textureWidth, (float) (uPosition + width) / textureWidth,
                (float) vPosition / textureHeight, (float) (vPosition + height) / textureHeight);
    }

    private void blitNineSliced(SpriteInfo info, int x, int y, int z, int width, int height) {
        int left = Math.min(info.borderLeft, width / 2);
        int right = Math.min(info.borderRight, width / 2);
        int top = Math.min(info.borderTop, height / 2);
        int bottom = Math.min(info.borderBottom, height / 2);
        int w = info.width;
        int h = info.height;

        if (width == w && height == h) {
            innerBlit(info.texture, x, x + width, y, y + height, z, 0, 1, 0, 1);
            return;
        }

        // Corners
        blitRegion(info, x, y, z, left, top, 0, 0, left, top);
        blitRegion(info, x + width - right, y, z, right, top, w - right, 0, right, top);
        blitRegion(info, x, y + height - bottom, z, left, bottom, 0, h - bottom, left, bottom);
        blitRegion(info, x + width - right, y + height - bottom, z, right, bottom, w - right, h - bottom, right,
                bottom);
        // Edges (tiled like 1.21)
        blitTiled(info, x + left, y, z, width - right - left, top, left, 0, w - right - left, top, w, h);
        blitTiled(info, x + left, y + height - bottom, z, width - right - left, bottom, left, h - bottom,
                w - right - left, bottom, w, h);
        blitTiled(info, x, y + top, z, left, height - bottom - top, 0, top, left, h - bottom - top, w, h);
        blitTiled(info, x + width - right, y + top, z, right, height - bottom - top, w - right, top, right,
                h - bottom - top, w, h);
        // Center
        blitTiled(info, x + left, y + top, z, width - right - left, height - bottom - top, left, top,
                w - right - left, h - bottom - top, w, h);
    }

    private void blitRegion(SpriteInfo info, int x, int y, int z, int width, int height, int u, int v, int uWidth,
            int vHeight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        innerBlit(info.texture, x, x + width, y, y + height, z,
                (float) u / info.width, (float) (u + uWidth) / info.width,
                (float) v / info.height, (float) (v + vHeight) / info.height);
    }

    /**
     * Fills the area with copies of the region (u, v, tileWidth, tileHeight) of the sprite.
     */
    private void blitTiled(SpriteInfo info, int x, int y, int z, int width, int height, int u, int v, int tileWidth,
            int tileHeight, int spriteWidth, int spriteHeight) {
        if (width <= 0 || height <= 0 || tileWidth <= 0 || tileHeight <= 0) {
            return;
        }
        for (int dx = 0; dx < width; dx += tileWidth) {
            int w = Math.min(tileWidth, width - dx);
            for (int dy = 0; dy < height; dy += tileHeight) {
                int h = Math.min(tileHeight, height - dy);
                innerBlit(info.texture, x + dx, x + dx + w, y + dy, y + dy + h, z,
                        (float) u / spriteWidth, (float) (u + w) / spriteWidth,
                        (float) v / spriteHeight, (float) (v + h) / spriteHeight);
            }
        }
    }

    /**
     * The 1.21 GUI sprite scaling, read from the .mcmeta next to the sprite texture.
     */
    private static final class SpriteInfo {
        private static final Map<ResourceLocation, SpriteInfo> CACHE = new HashMap<>();

        final ResourceLocation texture;
        String type = "stretch";
        int width = 16;
        int height = 16;
        int borderLeft, borderTop, borderRight, borderBottom;

        private SpriteInfo(ResourceLocation texture) {
            this.texture = texture;
        }

        static SpriteInfo get(Minecraft minecraft, ResourceLocation sprite) {
            return CACHE.computeIfAbsent(sprite, s -> load(minecraft, s));
        }

        private static SpriteInfo load(Minecraft minecraft, ResourceLocation sprite) {
            var texture = new ResourceLocation(sprite.getNamespace(),
                    "textures/gui/sprites/" + sprite.getPath() + ".png");
            var info = new SpriteInfo(texture);
            var meta = new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta");
            try (var resource = minecraft.getResourceManager().getResource(meta);
                    var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                if (json.has("gui") && json.getAsJsonObject("gui").has("scaling")) {
                    var scaling = json.getAsJsonObject("gui").getAsJsonObject("scaling");
                    info.type = scaling.has("type") ? scaling.get("type").getAsString() : "stretch";
                    info.width = scaling.has("width") ? scaling.get("width").getAsInt() : 16;
                    info.height = scaling.has("height") ? scaling.get("height").getAsInt() : 16;
                    if (scaling.has("border")) {
                        var border = scaling.get("border");
                        if (border.isJsonObject()) {
                            var b = border.getAsJsonObject();
                            info.borderLeft = b.get("left").getAsInt();
                            info.borderTop = b.get("top").getAsInt();
                            info.borderRight = b.get("right").getAsInt();
                            info.borderBottom = b.get("bottom").getAsInt();
                        } else {
                            info.borderLeft = info.borderTop = info.borderRight = info.borderBottom = border
                                    .getAsInt();
                        }
                    }
                }
            } catch (Exception ignored) {
                // No .mcmeta: stretched
            }
            return info;
        }
    }

    // ------------------------------------------------------------------------------------------ scissor

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        int[] rect = new int[] { minX, minY, maxX, maxY };
        var parent = scissorStack.peek();
        if (parent != null) {
            rect = new int[] { Math.max(minX, parent[0]), Math.max(minY, parent[1]), Math.min(maxX, parent[2]),
                    Math.min(maxY, parent[3]) };
        }
        scissorStack.push(rect);
        applyScissor(rect);
    }

    public void disableScissor() {
        scissorStack.poll();
        var rect = scissorStack.peek();
        if (rect != null) {
            applyScissor(rect);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void applyScissor(int[] rect) {
        var resolution = new ScaledResolution(minecraft);
        int scale = resolution.getScaleFactor();
        int width = Math.max(0, rect[2] - rect[0]);
        int height = Math.max(0, rect[3] - rect[1]);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(rect[0] * scale, minecraft.displayHeight - rect[3] * scale, width * scale, height * scale);
    }

    // ------------------------------------------------------------------------------------------ tooltips

    public void renderTooltip(FontRenderer font, ITextComponent text, int mouseX, int mouseY) {
        renderComponentTooltip(font, Collections.singletonList(text), mouseX, mouseY);
    }

    public void renderComponentTooltip(FontRenderer font, List<ITextComponent> lines, int mouseX, int mouseY) {
        renderTooltip(font, lines, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
    }

    public void renderComponentTooltip(FontRenderer font, List<ITextComponent> lines, int mouseX, int mouseY,
            ItemStack stack) {
        renderTooltip(font, lines, Optional.empty(), stack, mouseX, mouseY);
    }

    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, int mouseX, int mouseY) {
        renderTooltip(font, lines, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
    }

    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, Optional<?> image, int mouseX,
            int mouseY) {
        renderTooltip(font, lines, image, ItemStack.EMPTY, mouseX, mouseY);
    }

    /**
     * Renders a tooltip with an optional image (1.21 TooltipComponent, see {@link TooltipImages}).
     */
    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, Optional<?> image, ItemStack stack,
            int mouseX, int mouseY) {
        List<String> text = new ArrayList<>(lines.size());
        for (var line : lines) {
            text.add(line.getFormattedText());
        }
        renderStringTooltip(font, text, image.orElse(null), stack, mouseX, mouseY);
    }

    public void renderStringTooltip(FontRenderer font, List<String> lines, @Nullable Object image, ItemStack stack,
            int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }
        lines = TooltipImages.addPlaceholders(lines, TooltipImages.create(image));
        GuiUtils.drawHoveringText(stack, lines, mouseX, mouseY, guiWidth(), guiHeight(), -1, font);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1, 1, 1, 1);
    }
}
