package appeng.integration.modules.igtooltip.parts;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.core.localization.InGameTooltip;
import appeng.integration.modules.igtooltip.ItemNames;
import appeng.parts.automation.AnnihilationPlanePart;

public class AnnihilationPlaneDataProvider
        implements BodyProvider<AnnihilationPlanePart>, ServerDataProvider<AnnihilationPlanePart> {

    private static final String TAG_ENCHANTMENTS = "planeEnchantments";

    @Override
    public void buildTooltip(AnnihilationPlanePart plane, TooltipContext context, TooltipBuilder tooltip) {
        var serverData = context.serverData();
        if (serverData.hasKey(TAG_ENCHANTMENTS, Constants.NBT.TAG_COMPOUND)) {
            tooltip.addLine(InGameTooltip.EnchantedWith.text());
            var enchantments = serverData.getCompoundTag(TAG_ENCHANTMENTS);
            for (var enchantmentId : enchantments.getKeySet()) {
                var enchantment = Enchantment.getEnchantmentByLocation(enchantmentId);
                var level = enchantments.getInteger(enchantmentId);
                if (enchantment != null) {
                    tooltip.addLine(ItemNames.enchantment(enchantment, level));
                }
            }
        }
    }

    @Override
    public void provideServerData(EntityPlayer player, AnnihilationPlanePart plane, NBTTagCompound serverData) {
        var enchantments = plane.getEnchantments();
        if (enchantments != null && !enchantments.isEmpty()) {
            var enchantmentsTag = new NBTTagCompound();
            for (var entry : enchantments.entrySet()) {
                var id = Enchantment.REGISTRY.getNameForObject(entry.getKey());
                if (id != null) {
                    enchantmentsTag.setInteger(id.toString(), entry.getValue());
                }
            }
            serverData.setTag(TAG_ENCHANTMENTS, enchantmentsTag);
        }
    }
}
