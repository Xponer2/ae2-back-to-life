package appeng.items.storage;

import java.util.List;

import net.minecraft.item.ItemStack;
import appeng.api.util.TooltipComponent;

import appeng.api.stacks.GenericStack;

public final class StorageCellTooltipComponent implements TooltipComponent {
    private final List<ItemStack> upgrades;
    private final List<GenericStack> content;
    private final boolean hasMoreContent;
    private final boolean showAmounts;

    public StorageCellTooltipComponent(List<ItemStack> upgrades, List<GenericStack> content, boolean hasMoreContent, boolean showAmounts) {
        this.upgrades = upgrades;
        this.content = content;
        this.hasMoreContent = hasMoreContent;
        this.showAmounts = showAmounts;
    }

    public List<ItemStack> upgrades() {
        return upgrades;
    }

    public List<GenericStack> content() {
        return content;
    }

    public boolean hasMoreContent() {
        return hasMoreContent;
    }

    public boolean showAmounts() {
        return showAmounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StorageCellTooltipComponent)) return false;
        StorageCellTooltipComponent that = (StorageCellTooltipComponent) o;
        return java.util.Objects.equals(upgrades, that.upgrades)
                && java.util.Objects.equals(content, that.content)
                && hasMoreContent == that.hasMoreContent
                && showAmounts == that.showAmounts;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(upgrades, content, hasMoreContent, showAmounts);
    }

    @Override
    public String toString() {
        return "StorageCellTooltipComponent[" + "upgrades=" + upgrades + ", " + "content=" + content + ", " + "hasMoreContent=" + hasMoreContent + ", " + "showAmounts=" + showAmounts + "]";
    }

}
