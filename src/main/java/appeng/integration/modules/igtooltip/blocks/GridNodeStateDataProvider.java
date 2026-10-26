package appeng.integration.modules.igtooltip.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

import appeng.api.implementations.IPowerChannelState;
import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.integration.modules.igtooltip.GridNodeState;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.util.TextUtil;

/**
 * Provide info about the grid connection status of a machine.
 */
public final class GridNodeStateDataProvider implements BodyProvider<TileEntity>, ServerDataProvider<TileEntity> {
    private static final String TAG_STATE = "gridNodeState";

    @Override
    public void buildTooltip(TileEntity object, TooltipContext context, TooltipBuilder tooltip) {
        var tag = context.serverData();
        if (tag.hasKey(TAG_STATE, Constants.NBT.TAG_BYTE)) {
            var state = GridNodeState.values()[tag.getByte(TAG_STATE)];
            tooltip.addLine(TextUtil.withStyle(state.textComponent(), TextFormatting.GRAY));
        }
    }

    @Override
    public void provideServerData(EntityPlayer player, TileEntity object, NBTTagCompound serverData) {
        // Some devices can be powered both externally and through the grid.
        // If they are powered externally, they might still be active when the grid itself is down
        if (object instanceof IPowerChannelState powerChannelState && powerChannelState.isActive()) {
            serverData.setByte(TAG_STATE, (byte) GridNodeState.ONLINE.ordinal());
            return;
        }

        if (object instanceof IGridConnectedBlockEntity gridConnectedBlockEntity) {
            var state = GridNodeState.fromNode(gridConnectedBlockEntity.getActionableNode());
            serverData.setByte(TAG_STATE, (byte) state.ordinal());
        }
    }

}
