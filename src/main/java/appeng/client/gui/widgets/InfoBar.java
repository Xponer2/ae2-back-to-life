package appeng.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.util.ItemLike;
import appeng.client.gui.GuiGraphics;
import appeng.client.gui.Icon;

public class InfoBar {
    private final List<Widget> widgets = new ArrayList<>();

    public void render(GuiGraphics guiGraphics, int x, int y) {
        var maxHeight = widgets.stream().mapToInt(Widget::getHeight).max().orElse(0);

        for (var widget : widgets) {
            widget.render(
                    guiGraphics,
                    x,
                    Math.round(y + maxHeight / 2.f - widget.getHeight() / 2.f));
            x += widget.getWidth();
        }

    }

    interface Widget {
        int getWidth();

        int getHeight();

        void render(GuiGraphics guiGraphics, int x, int y);
    }

    // TODO (RID): Added xPos and yPos to give me better control over render, but the code below might need refactoring
    void add(Icon icon, float scale, int xPos, int yPos) {
        widgets.add(new IconWidget(icon, scale, xPos, yPos));
    }

    void add(String text, int color, float scale, int xPos, int yPos) {
        widgets.add(new TextWidget(new TextComponentString(text), color, scale, xPos, yPos));
    }

    void add(ITextComponent text, int color, float scale, int xPos, int yPos) {
        widgets.add(new TextWidget(text, color, scale, xPos, yPos));
    }

    void add(AEKey what, float scale, int xPos, int yPos) {
        widgets.add(new StackWidget(what, scale, xPos, yPos));
    }

    void add(ItemLike what, float scale, int xPos, int yPos) {
        widgets.add(new StackWidget(AEItemKey.of(what), scale, xPos, yPos));
    }

    void addSpace(int width) {
        widgets.add(new SpaceWidget(width));
    }

    private final static class StackWidget implements Widget {
        private final AEKey what;
        private final float scale;
        private final int xPos;
        private final int yPos;

        public StackWidget(AEKey what, float scale, int xPos, int yPos) {
            this.what = what;
            this.scale = scale;
            this.xPos = xPos;
            this.yPos = yPos;
        }

        public AEKey what() {
            return what;
        }

        public float scale() {
            return scale;
        }

        public int xPos() {
            return xPos;
        }

        public int yPos() {
            return yPos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StackWidget)) return false;
            StackWidget that = (StackWidget) o;
            return java.util.Objects.equals(what, that.what)
                    && Float.compare(scale, that.scale) == 0
                    && xPos == that.xPos
                    && yPos == that.yPos;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(what, scale, xPos, yPos);
        }

        @Override
        public String toString() {
            return "StackWidget[" + "what=" + what + ", " + "scale=" + scale + ", " + "xPos=" + xPos + ", " + "yPos=" + yPos + "]";
        }

        @Override
        public int getWidth() {
            return Math.round(16 * scale);
        }

        @Override
        public int getHeight() {
            return Math.round(16 * scale);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y) {
            var poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(xPos, yPos, 0);
            poseStack.scale(scale, scale, 1);
            AEKeyRendering.drawInGui(Minecraft.getMinecraft(), guiGraphics, 0, 0, what);
            poseStack.popPose();
        }
    }

    private final static class IconWidget implements Widget {
        private final Icon icon;
        private final float scale;
        private final int xPos;
        private final int yPos;

        public IconWidget(Icon icon, float scale, int xPos, int yPos) {
            this.icon = icon;
            this.scale = scale;
            this.xPos = xPos;
            this.yPos = yPos;
        }

        public Icon icon() {
            return icon;
        }

        public float scale() {
            return scale;
        }

        public int xPos() {
            return xPos;
        }

        public int yPos() {
            return yPos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IconWidget)) return false;
            IconWidget that = (IconWidget) o;
            return java.util.Objects.equals(icon, that.icon)
                    && Float.compare(scale, that.scale) == 0
                    && xPos == that.xPos
                    && yPos == that.yPos;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(icon, scale, xPos, yPos);
        }

        @Override
        public String toString() {
            return "IconWidget[" + "icon=" + icon + ", " + "scale=" + scale + ", " + "xPos=" + xPos + ", " + "yPos=" + yPos + "]";
        }

        @Override
        public int getWidth() {
            return Math.round(16 * scale);
        }

        @Override
        public int getHeight() {
            return Math.round(16 * scale);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y) {
            var poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(xPos, yPos, 0);
            poseStack.scale(scale, scale, 1);
            icon.getBlitter()
                    .dest(0, 0)
                    .blit(guiGraphics);
            poseStack.popPose();
        }
    }

    private static final class TextWidget implements Widget {
        private final ITextComponent text;
        private final int color;
        private final float scale;
        private final int xPos;
        private final int yPos;
        private final int width;
        private final int height;

        public TextWidget(ITextComponent text, int color, float scale, int xPos, int yPos) {
            this.text = text;
            this.color = color;
            this.scale = scale;
            this.xPos = xPos;
            this.yPos = yPos;
            var font = Minecraft.getMinecraft().fontRenderer;
            this.width = Math.round(font.getStringWidth(text.getFormattedText()) * scale);
            this.height = Math.round(font.FONT_HEIGHT * scale);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y) {
            var poseStack = guiGraphics.pose();
            var font = Minecraft.getMinecraft().fontRenderer;
            poseStack.pushPose();
            poseStack.translate(xPos, yPos, 0);
            poseStack.scale(scale, scale, 1);
            guiGraphics.drawString(font, text, 0, 0, color, false);
            poseStack.popPose();
        }
    }

    private static final class SpaceWidget implements Widget {
        private final int width;

        public SpaceWidget(int width) {
            this.width = width;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y) {
        }
    }
}
