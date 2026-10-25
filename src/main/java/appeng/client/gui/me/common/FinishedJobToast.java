package appeng.client.gui.me.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.GuiGraphics;
import appeng.core.localization.GuiText;

/**
 * A Minecraft toast for a finished crafting job.
 */
public class FinishedJobToast implements IToast {
    // Toasts of the game have a fixed size, the text gets one line
    private static final int WIDTH = 160;
    private static final int TEXT_WIDTH = WIDTH - 30 - 5;

    private static final long TIME_VISIBLE = 2500;
    private static final int TITLE_COLOR = 0xFF500050;
    private static final int TEXT_COLOR = 0xFF000000;

    private final AEKey what;
    private final String line;

    public FinishedJobToast(AEKey what, long amount) {
        this.what = what;

        var minecraft = Minecraft.getMinecraft();
        var font = minecraft.fontRenderer;

        var formattedAmount = what.formatAmount(amount, AmountFormat.SLOT);

        var text = GuiText.ToastCraftingJobFinishedText.text(formattedAmount, AEKeyRendering.getDisplayName(what));
        line = fitToWidth(font, text.getFormattedText(), TEXT_WIDTH);
    }

    private static String fitToWidth(FontRenderer font, String text, int width) {
        if (font.getStringWidth(text) <= width) {
            return text;
        }
        return font.trimStringToWidth(text, width - font.getStringWidth("...")) + "...";
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        var minecraft = toastGui.getMinecraft();
        var font = minecraft.fontRenderer;
        var guiGraphics = new GuiGraphics(minecraft);

        minecraft.getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        toastGui.drawTexturedModalRect(0, 0, 0, 32, WIDTH, 32);
        guiGraphics.drawString(font, GuiText.ToastCraftingJobFinishedTitle.text(), 30, 7, TITLE_COLOR, false);
        guiGraphics.drawString(font, line, 30, 18, TEXT_COLOR, false);
        AEKeyRendering.drawInGui(minecraft, guiGraphics, 8, 8, what);

        return delta >= TIME_VISIBLE ? Visibility.HIDE : Visibility.SHOW;
    }
}
