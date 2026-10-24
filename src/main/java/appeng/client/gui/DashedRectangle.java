package appeng.client.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.PoseStack;

/**
 * Rendering helper for rendering a rectangle with a dashed outline.
 * <p/>
 * The 1.21 version takes a GuideME LytRect, which doesn't exist in 1.12.2: {@link Rect2i} instead. The pose stack
 * is applied by OpenGL.
 */
@SideOnly(Side.CLIENT)
public final class DashedRectangle {
    private DashedRectangle() {
    }

    public static void render(PoseStack stack, Rect2i bounds, DashPattern pattern, float z) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder builder = tesselator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float right = bounds.getX() + bounds.getWidth();
        float bottom = bounds.getY() + bounds.getHeight();

        var t = 0f;
        if (pattern.animationCycleMs() > 0) {
            t = (System.currentTimeMillis() % (int) pattern.animationCycleMs()) / pattern.animationCycleMs();
        }

        buildHorizontalDashedLine(builder, t, bounds.getX(), right, bounds.getY(), z, pattern, false);
        buildHorizontalDashedLine(builder, t, bounds.getX(), right, bottom - pattern.width(), z, pattern, true);

        buildVerticalDashedLine(builder, t, bounds.getX(), bounds.getY(), bottom, z, pattern, true);
        buildVerticalDashedLine(builder, t, right - pattern.width(), bounds.getY(), bottom, z, pattern, false);

        tesselator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void buildHorizontalDashedLine(BufferBuilder builder,
            float t, float x1, float x2, float y, float z,
            DashPattern pattern, boolean reverse) {
        if (!reverse) {
            t = 1 - t;
        }
        var phase = t * pattern.length();

        var color = pattern.color();
        int a = color >>> 24 & 0xFF, r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;

        for (float x = x1 - phase; x < x2; x += pattern.length()) {
            builder.pos(MathHelper.clamp(x + pattern.onLength(), x1, x2), y, z).color(r, g, b, a).endVertex();
            builder.pos(MathHelper.clamp(x, x1, x2), y, z).color(r, g, b, a).endVertex();
            builder.pos(MathHelper.clamp(x, x1, x2), y + pattern.width(), z).color(r, g, b, a).endVertex();
            builder.pos(MathHelper.clamp(x + pattern.onLength(), x1, x2), y + pattern.width(), z).color(r, g, b, a).endVertex();
        }
    }

    private static void buildVerticalDashedLine(BufferBuilder builder,
            float t, float x, float y1, float y2, float z,
            DashPattern pattern, boolean reverse) {
        if (!reverse) {
            t = 1 - t;
        }
        var phase = t * pattern.length();

        var color = pattern.color();
        int a = color >>> 24 & 0xFF, r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;

        for (float y = y1 - phase; y < y2; y += pattern.length()) {
            builder.pos(x + pattern.width(), MathHelper.clamp(y, y1, y2), z).color(r, g, b, a).endVertex();
            builder.pos(x, MathHelper.clamp(y, y1, y2), z).color(r, g, b, a).endVertex();
            builder.pos(x, MathHelper.clamp(y + pattern.onLength(), y1, y2), z).color(r, g, b, a).endVertex();
            builder.pos(x + pattern.width(), MathHelper.clamp(y + pattern.onLength(), y1, y2), z).color(r, g, b, a).endVertex();
        }
    }

}
