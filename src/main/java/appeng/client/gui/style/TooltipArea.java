package appeng.client.gui.style;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.text.ITextComponent;

public final class TooltipArea extends Position {
    private int width;
    private int height;

    private List<ITextComponent> tooltip = new ArrayList<>();

    public List<ITextComponent> getTooltip() {
        return tooltip;
    }

    public void setTooltip(List<ITextComponent> tooltip) {
        this.tooltip = tooltip;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
