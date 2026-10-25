package appeng.client.gui.implementations;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import appeng.api.client.AEKeyRendering;
import appeng.api.config.LockCraftingMode;
import appeng.api.stacks.AmountFormat;
import appeng.client.Point;
import appeng.client.gui.GuiGraphics;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.Rect2i;
import appeng.client.gui.Tooltip;
import appeng.core.localization.GuiText;
import appeng.core.localization.InGameTooltip;

public class PatternProviderLockReason implements ICompositeWidget {
    protected boolean visible = false;
    protected int x;
    protected int y;

    private final PatternProviderScreen<?> screen;

    public PatternProviderLockReason(PatternProviderScreen<?> screen) {
        this.screen = screen;
    }

    @Override
    public void setPosition(Point position) {
        x = position.getX();
        y = position.getY();
    }

    @Override
    public void setSize(int width, int height) {
    }

    @Override
    public Rect2i getBounds() {
        return new Rect2i(x, y, 126, 16);
    }

    @Override
    public final boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void drawForegroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        var menu = screen.getMenu();

        Icon icon;
        ITextComponent lockStatusText;
        // Text styles only know the 16 formatting colors, so the color is given to the font instead
        int lockStatusColor;
        if (menu.getCraftingLockedReason() == LockCraftingMode.NONE) {
            icon = Icon.UNLOCKED;
            lockStatusText = GuiText.CraftingLockIsUnlocked.text();
            lockStatusColor = 0xFF7DA9D2;
        } else {
            icon = Icon.LOCKED;
            lockStatusText = GuiText.CraftingLockIsLocked.text();
            lockStatusColor = 0xFFC1424B;
        }

        icon.getBlitter().dest(x, y).blit(guiGraphics);
        guiGraphics.drawString(Minecraft.getMinecraft().fontRenderer, lockStatusText, x + 15, y + 5, lockStatusColor,
                false);
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        var menu = screen.getMenu();
        var tooltip = switch (menu.getCraftingLockedReason()) {
            case NONE -> null;
            case LOCK_UNTIL_PULSE -> InGameTooltip.CraftingLockedUntilPulse.text();
            case LOCK_WHILE_HIGH -> InGameTooltip.CraftingLockedByRedstoneSignal.text();
            case LOCK_WHILE_LOW -> InGameTooltip.CraftingLockedByLackOfRedstoneSignal.text();
            case LOCK_UNTIL_RESULT -> {
                var stack = menu.getUnlockStack();
                ITextComponent stackName;
                ITextComponent stackAmount;
                if (stack != null) {
                    stackName = AEKeyRendering.getDisplayName(stack.what());
                    stackAmount = new TextComponentString(stack.what().formatAmount(stack.amount(), AmountFormat.FULL));
                } else {
                    stackName = new TextComponentString("ERROR");
                    stackAmount = new TextComponentString("ERROR");

                }
                yield InGameTooltip.CraftingLockedUntilResult.text(stackName, stackAmount);
            }
        };

        return tooltip != null ? new Tooltip(tooltip) : null;
    }
}
