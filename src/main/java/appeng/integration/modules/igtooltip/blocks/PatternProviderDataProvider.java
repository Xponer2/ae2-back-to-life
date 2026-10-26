package appeng.integration.modules.igtooltip.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.core.localization.InGameTooltip;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.TextUtil;

public final class PatternProviderDataProvider
        implements BodyProvider<PatternProviderLogicHost>, ServerDataProvider<PatternProviderLogicHost> {
    private static final String NBT_LOCK_REASON = "craftingLockReason";
    private static final String NBT_LOCK_UNTIL_RESULT_STACK = "craftingLockUntilResultStack";

    @Override
    public void buildTooltip(PatternProviderLogicHost host, TooltipContext context, TooltipBuilder tooltip) {
        var lockReason = context.serverData().getString(NBT_LOCK_REASON);
        if (!lockReason.isEmpty()) {
            var reason = ITextComponent.Serializer.jsonToComponent(lockReason);
            if (reason != null) {
                tooltip.addLine(reason);
            }
        }
        var stack = context.serverData().getCompoundTag(NBT_LOCK_UNTIL_RESULT_STACK);
        if (!stack.isEmpty()) {
            var genericStack = GenericStack.readTag(stack);
            ITextComponent stackName;
            ITextComponent stackAmount;
            if (genericStack == null) {
                stackName = new TextComponentString("ERROR");
                stackAmount = new TextComponentString("ERROR");
            } else {
                // 1.21 asks the client-only AEKeyRendering; The One Probe builds this on the server
                stackName = genericStack.what().getDisplayName();
                stackAmount = new TextComponentString(
                        genericStack.what().formatAmount(genericStack.amount(), AmountFormat.FULL));
            }
            tooltip.addLine(TextUtil.withStyle(InGameTooltip.CraftingLockedUntilResult.text(stackName, stackAmount),
                    TextFormatting.RED));
        }
    }

    @Override
    public void provideServerData(EntityPlayer player, PatternProviderLogicHost host, NBTTagCompound serverData) {
        var logic = host.getLogic();
        ITextComponent reason = null;

        switch (logic.getCraftingLockedReason()) {
            case LOCK_UNTIL_PULSE -> {
                reason = InGameTooltip.CraftingLockedUntilPulse.text();
            }
            case LOCK_WHILE_HIGH -> {
                reason = InGameTooltip.CraftingLockedByRedstoneSignal.text();
            }
            case LOCK_WHILE_LOW -> {
                reason = InGameTooltip.CraftingLockedByLackOfRedstoneSignal.text();
            }
            case LOCK_UNTIL_RESULT -> {
                var stack = logic.getUnlockStack();
                if (stack != null) {
                    serverData.setTag(NBT_LOCK_UNTIL_RESULT_STACK, GenericStack.writeTag(stack));
                } else {
                    // Put a non-empty compound tag, so we get "ERROR" when handling this on the client
                    final NBTTagCompound errorDummy = new NBTTagCompound();
                    errorDummy.setString("error", "error");
                    serverData.setTag(NBT_LOCK_UNTIL_RESULT_STACK, errorDummy);
                }
                return;
            }
            default -> {
            }
        }

        if (reason != null) {
            serverData.setString(NBT_LOCK_REASON,
                    ITextComponent.Serializer.componentToJson(TextUtil.withStyle(reason, TextFormatting.RED)));
        }
    }
}
