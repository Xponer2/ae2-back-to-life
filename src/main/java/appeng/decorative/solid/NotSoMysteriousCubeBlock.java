package appeng.decorative.solid;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.misc.MysteriousCubeBlock;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.decorative.AEDecorativeBlock;
import appeng.util.ItemTooltipContext;

public class NotSoMysteriousCubeBlock extends AEDecorativeBlock {
    public NotSoMysteriousCubeBlock() {
        super(MysteriousCubeBlock.PROPERTIES);
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        tooltip.add(Tooltips.of(GuiText.NotSoMysteriousQuote, Tooltips.QUOTE_TEXT));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
