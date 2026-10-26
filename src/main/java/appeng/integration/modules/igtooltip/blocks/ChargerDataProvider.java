package appeng.integration.modules.igtooltip.blocks;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.core.localization.InGameTooltip;
import appeng.integration.modules.igtooltip.ItemNames;
import appeng.util.Platform;
import appeng.util.TextUtil;

/**
 * Shows the tooltip item and the charge progress.
 */
public final class ChargerDataProvider implements BodyProvider<ChargerBlockEntity> {
    @Override
    public void buildTooltip(ChargerBlockEntity charger, TooltipContext context, TooltipBuilder tooltip) {
        var chargerInventory = charger.getInternalInventory();
        var chargingItem = chargerInventory.getStackInSlot(0);

        if (!chargingItem.isEmpty()) {
            tooltip.addLine(InGameTooltip.Contains.text(
                    TextUtil.withStyle(ItemNames.of(chargingItem), TextFormatting.WHITE)));

            if (chargingItem.getItem() instanceof IAEItemPowerStorage powerStorage
                    && Platform.isChargeable(chargingItem)) {
                var fillRate = MathHelper.floor(powerStorage.getAECurrentPower(chargingItem) * 100 /
                        powerStorage.getAEMaxPower(chargingItem));
                tooltip.addLine(InGameTooltip.Charged.text(fillRate));
            }
        }
    }
}
