package appeng.parts.automation;

import appeng.util.ItemProperties;

import appeng.core.CreativeTabOutput;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import appeng.api.util.Registries;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.items.parts.PartItem;

/**
 * Special part item for {@link AnnihilationPlanePart} to handle enchants and extended tooltips.
 */
public class AnnihilationPlanePartItem extends PartItem<AnnihilationPlanePart> {
    public AnnihilationPlanePartItem(ItemProperties properties) {
        super(properties, AnnihilationPlanePart.class, AnnihilationPlanePart::new);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> lines,
            ITooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, lines, isAdvanced);

        var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            lines.add(Tooltips.of(GuiText.CanBeEnchanted));
        } else {
            lines.add(Tooltips.of(GuiText.IncreasedEnergyUseFromEnchants));
        }
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        super.addToMainCreativeTab(parameters, output);

        var enchantmentRegistry = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);

        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH), 1);

        var silkTouch = new ItemStack(this);
        silkTouch.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        output.accept(silkTouch);
    }
}
