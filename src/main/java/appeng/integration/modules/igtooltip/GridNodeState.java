package appeng.integration.modules.igtooltip;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;

import appeng.api.networking.IGridNode;
import appeng.core.localization.InGameTooltip;

/**
 * Contains the state of a grid node, as displayed in in-game tooltips.
 */
public enum GridNodeState {
    OFFLINE(InGameTooltip.DeviceOffline),
    NETWORK_BOOTING(InGameTooltip.NetworkBooting),
    MISSING_CHANNEL(InGameTooltip.DeviceMissingChannel),
    ONLINE(InGameTooltip.DeviceOnline);

    private final InGameTooltip text;

    GridNodeState(InGameTooltip text) {
        this.text = text;
    }

    public ITextComponent textComponent() {
        return text.text();
    }

    public static GridNodeState fromNode(@Nullable IGridNode gridNode) {
        var state = GridNodeState.OFFLINE;
        if (gridNode != null && gridNode.isPowered()) {
            if (!gridNode.hasGridBooted()) {
                state = GridNodeState.NETWORK_BOOTING;
            } else if (!gridNode.meetsChannelRequirements()) {
                state = GridNodeState.MISSING_CHANNEL;
            } else {
                state = GridNodeState.ONLINE;
            }
        }
        return state;
    }
}
