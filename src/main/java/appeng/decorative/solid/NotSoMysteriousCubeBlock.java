package appeng.decorative.solid;

import appeng.util.ItemTooltipContext;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import appeng.block.misc.MysteriousCubeBlock;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.decorative.AEDecorativeBlock;

public class NotSoMysteriousCubeBlock extends AEDecorativeBlock {
    public NotSoMysteriousCubeBlock() {
        super(MysteriousCubeBlock.PROPERTIES);
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        tooltip.add(Tooltips.of(GuiText.NotSoMysteriousQuote, Tooltips.QUOTE_TEXT));
    }
}
