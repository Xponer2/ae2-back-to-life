package appeng.parts.automation;

import appeng.util.ItemProperties;

import appeng.core.CreativeTabOutput;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.items.parts.PartItem;
import appeng.util.ItemTooltipContext;

/**
 * Special part item for {@link AnnihilationPlanePart} to handle enchants and extended tooltips.
 */
public class AnnihilationPlanePartItem extends PartItem<AnnihilationPlanePart> {
    public AnnihilationPlanePartItem(ItemProperties properties) {
        super(properties, AnnihilationPlanePart.class, AnnihilationPlanePart::new);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getItemEnchantability() {
        return 10;
    }

    /**
     * The plane takes the enchantments of mining tools (silk touch, fortune, efficiency, unbreaking).
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.enchantment.Enchantment enchantment) {
        return enchantment.type == EnumEnchantmentType.DIGGER || enchantment.type == EnumEnchantmentType.BREAKABLE;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> lines,
            ITooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, lines, isAdvanced);

        var enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) {
            lines.add(Tooltips.of(GuiText.CanBeEnchanted));
        } else {
            lines.add(Tooltips.of(GuiText.IncreasedEnergyUseFromEnchants));
        }
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        super.addToMainCreativeTab(parameters, output);

        var silkTouch = new ItemStack(this);
        silkTouch.addEnchantment(Enchantments.SILK_TOUCH, 1);
        output.accept(silkTouch);
    }
}
