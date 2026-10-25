package appeng.integration.modules.igtooltip.parts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.api.networking.pathing.ControllerState;
import appeng.core.localization.InGameTooltip;
import appeng.me.service.PathingService;
import appeng.parts.networking.IUsedChannelProvider;
import appeng.util.TextUtil;

/**
 * Shows the used and maximum channel counts for a part that implements {@link IUsedChannelProvider}.
 */
public final class ChannelDataProvider
        implements BodyProvider<IUsedChannelProvider>, ServerDataProvider<IUsedChannelProvider> {
    private static final String TAG_MAX_CHANNELS = "maxChannels";
    private static final String TAG_USED_CHANNELS = "usedChannels";
    private static final String TAG_ERROR = "channelError";

    @Override
    public void buildTooltip(IUsedChannelProvider object, TooltipContext context, TooltipBuilder tooltip) {
        var serverData = context.serverData();
        if (serverData.hasKey(TAG_ERROR, Constants.NBT.TAG_STRING)) {
            var error = ChannelError.valueOf(serverData.getString(TAG_ERROR));
            tooltip.addLine(TextUtil.withStyle(error.text.text(), TextFormatting.RED));
            return;
        }

        if (serverData.hasKey(TAG_MAX_CHANNELS, Constants.NBT.TAG_INT)) {
            var usedChannels = serverData.getInteger(TAG_USED_CHANNELS);
            var maxChannels = serverData.getInteger(TAG_MAX_CHANNELS);
            // Even in the maxChannels=0 case, we'll show as infinite
            if (maxChannels <= 0) {
                tooltip.addLine(InGameTooltip.Channels.text(usedChannels));
            } else {
                tooltip.addLine(InGameTooltip.ChannelsOf.text(usedChannels, maxChannels));
            }
        }
    }

    @Override
    public void provideServerData(EntityPlayer player, IUsedChannelProvider object, NBTTagCompound serverData) {
        var gridNode = object.getGridNode();
        if (gridNode != null) {
            var pathingService = (PathingService) gridNode.getGrid().getPathingService();
            if (pathingService.getControllerState() == ControllerState.NO_CONTROLLER) {
                var adHocError = pathingService.getAdHocNetworkError();
                if (adHocError != null) {
                    serverData.setString(TAG_ERROR, switch (adHocError) {
                        case NESTED_P2P_TUNNEL -> ChannelError.AD_HOC_NESTED_P2P_TUNNEL.name();
                        case TOO_MANY_CHANNELS -> ChannelError.AD_HOC_TOO_MANY_CHANNELS.name();
                    });
                    return;
                }
            } else if (pathingService.getControllerState() == ControllerState.CONTROLLER_CONFLICT) {
                serverData.setString(TAG_ERROR, ChannelError.CONTROLLER_CONFLICT.name());
            }
        }

        serverData.setInteger(TAG_USED_CHANNELS, object.getUsedChannelsInfo());
        serverData.setInteger(TAG_MAX_CHANNELS, object.getMaxChannelsInfo());
    }

    enum ChannelError {
        AD_HOC_NESTED_P2P_TUNNEL(InGameTooltip.ErrorNestedP2PTunnel),
        AD_HOC_TOO_MANY_CHANNELS(InGameTooltip.ErrorTooManyChannels),
        CONTROLLER_CONFLICT(InGameTooltip.ErrorControllerConflict);

        final InGameTooltip text;

        ChannelError(InGameTooltip text) {
            this.text = text;
        }
    }
}
