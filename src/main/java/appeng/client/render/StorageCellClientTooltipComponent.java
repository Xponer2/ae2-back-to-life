package appeng.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.GuiGraphics;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.core.localization.GuiText;
import appeng.items.storage.StorageCellTooltipComponent;

/**
 * Renders the content and upgrades of a storage cell below its tooltip text (see {@link TooltipImages}).
 */
@SideOnly(Side.CLIENT)
public class StorageCellClientTooltipComponent implements ClientTooltipImage {
    private final StorageCellTooltipComponent tooltipComponent;
    private final String upgradesLabel;

    public StorageCellClientTooltipComponent(StorageCellTooltipComponent tooltipComponent) {
        this.tooltipComponent = tooltipComponent;
        this.upgradesLabel = GuiText.StorageCellTooltipUpgrades.text().getFormattedText();
    }

    @Override
    public int getHeight() {
        var height = 0;
        var upgrades = tooltipComponent.upgrades();
        if (!upgrades.isEmpty()) {
            height += 17;
        }
        var content = tooltipComponent.content();
        if (!content.isEmpty()) {
            height += 17;
        }
        return height;
    }

    @Override
    public int getWidth(FontRenderer font) {
        int width = 0;

        var content = tooltipComponent.content();
        if (!content.isEmpty()) {
            var filterWidth = content.size() * 17;
            if (tooltipComponent.hasMoreContent()) {
                filterWidth += 10; // ellipsis
            }
            width = Math.max(width, filterWidth);
        }

        var upgrades = tooltipComponent.upgrades();
        if (!upgrades.isEmpty()) {
            var upgradesWidth = font.getStringWidth(upgradesLabel) + 2 + 17 * upgrades.size();
            width = Math.max(width, upgradesWidth);
        }

        return width;
    }

    @Override
    public void renderText(FontRenderer font, int x, int y) {
        var yoff = (16 - font.FONT_HEIGHT) / 2;

        var content = tooltipComponent.content();
        if (!content.isEmpty()) {
            var xoff = content.size() * 17;
            if (tooltipComponent.hasMoreContent()) {
                font.drawString("…", x + xoff + 2, y + 2, -1);
            }
            y += 17;
        }

        var upgrades = tooltipComponent.upgrades();
        if (!upgrades.isEmpty()) {
            font.drawString(upgradesLabel, x, y + yoff, 0x7E7E7E);
        }
    }

    @Override
    public void renderImage(FontRenderer font, int x, int y, GuiGraphics guiGraphics) {
        var content = tooltipComponent.content();
        if (!content.isEmpty()) {
            var xoff = 0;
            for (var stack : content) {
                AEKeyRendering.drawInGui(Minecraft.getMinecraft(), guiGraphics, x + xoff, y, stack.what());
                xoff += 17;
            }

            // Now render the amounts on top of the items
            if (tooltipComponent.showAmounts()) {
                xoff = 0;
                for (var stack : content) {
                    var amtText = stack.what().formatAmount(stack.amount(), AmountFormat.SLOT);
                    StackSizeRenderer.renderSizeLabel(guiGraphics, font, x + xoff, y, amtText, false);
                    xoff += 17;
                }
            }

            y += 17;
        }

        var upgrades = tooltipComponent.upgrades();
        if (!upgrades.isEmpty()) {
            var xoff = font.getStringWidth(upgradesLabel) + 2;
            var renderItem = Minecraft.getMinecraft().getRenderItem();
            RenderHelper.enableGUIStandardItemLighting();
            for (ItemStack upgrade : upgrades) {
                renderItem.renderItemAndEffectIntoGUI(upgrade, x + xoff, y);
                xoff += 17;
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
        }
    }
}
