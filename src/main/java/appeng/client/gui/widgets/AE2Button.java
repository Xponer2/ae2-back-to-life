package appeng.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import appeng.client.gui.GuiGraphics;
import appeng.core.AppEng;
import appeng.util.Util;

public class AE2Button extends GuiButton {
    protected static final WidgetSprites SPRITES = new WidgetSprites(
            AppEng.makeId("button"), AppEng.makeId("button_disabled"), AppEng.makeId("button_highlighted"));

    public AE2Button(int pX, int pY, int pWidth, int pHeight, ITextComponent component, OnPress onPress) {
        super(pX, pY, pWidth, pHeight, component, onPress, supplier -> new TextComponentString(""));
    }

    public AE2Button(ITextComponent component, OnPress onPress) {
        super(0, 0, 0, 0, component, onPress, supplier -> new TextComponentString(""));
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getMinecraft();
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        pGuiGraphics.blitSprite(SPRITES.get(this.active, this.isHovered()), this.getX(), this.getY(), this.getWidth(),
                this.getHeight());
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (!this.active) {
            this.renderButtonText(pGuiGraphics, minecraft.fontRenderer, 2, 0x413f54 | MathHelper.ceil(this.alpha * 255.0F) << 24, -1);
        } else if (this.isHovered()) {
            this.renderButtonText(pGuiGraphics, minecraft.fontRenderer, 2, 0x517497 | MathHelper.ceil(this.alpha * 255.0F) << 24, 0);
        } else {
            this.renderButtonText(pGuiGraphics, minecraft.fontRenderer, 2, 0xf2f2f2 | MathHelper.ceil(this.alpha * 255.0F) << 24, 1);
        }
    }

    public static void renderButtonText(GuiGraphics pGuiGraphics, FontRenderer pFont, ITextComponent pText, int pMinX, int pMinY,
            int pMaxX, int pMaxY, int yOffset, int pColor) {
        renderButtonText(pGuiGraphics, pFont, pText, (pMinX + pMaxX) / 2, pMinX, pMinY, pMaxX, pMaxY, yOffset, pColor);
    }

    public static void renderButtonText(GuiGraphics pGuiGraphics, FontRenderer pFont, ITextComponent pText, int pCenterX, int pMinX,
            int pMinY, int pMaxX, int pMaxY, int yOffset, int pColor) {
        int i = pFont.getStringWidth(pText);
        int j = (pMinY + pMaxY - 9) / 2 + 1;
        int k = pMaxX - pMinX;
        if (i > k) {
            int l = i - k;
            double d0 = (double) Util.getMillis() / 1000.0;
            double d1 = Math.max((double) l * 0.5, 3.0);
            double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
            double d3 = MathHelper.lerp(d2, 0.0, (double) l);
            pGuiGraphics.enableScissor(pMinX, pMinY, pMaxX, pMaxY);
            pGuiGraphics.drawString(pFont, pText, pMinX - (int) d3, j, pColor, false);
            pGuiGraphics.disableScissor();
        } else {
            int i1 = MathHelper.clamp(pCenterX, pMinX + i / 2, pMaxX - i / 2);
            FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
            pGuiGraphics.drawString(pFont, formattedcharsequence, i1 - pFont.width(formattedcharsequence) / 2,
                    j - yOffset, pColor, false);
        }
    }

    protected void renderButtonText(GuiGraphics pGuiGraphics, FontRenderer pFont, int pWidth, int pColor, int yOffset) {
        int i = this.getX() + pWidth;
        int j = this.getX() + this.getWidth() - pWidth;
        renderButtonText(pGuiGraphics, pFont, this.getMessage(), i, this.getY(), j, this.getY() + this.getHeight(),
                yOffset, pColor);
    }
}
