package appeng.integration.modules.igtooltip;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.orientation.IOrientationStrategy;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.me.InWorldGridNode;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.me.service.TickManagerService;
import appeng.parts.AEBasePart;
import appeng.util.Platform;
import appeng.util.TextUtil;

/**
 * Add debug info to the waila tooltip if the user is holding a debug card and the block entity is a grid-connected
 * block entity.
 */
public final class DebugProvider {

    private static final String TAG_NODES = "debugNodes";
    private static final String TAG_NODE_NAME = "nodeName";
    // 1.12.2 has no getter for long arrays in NBT: the three tick times are separate longs
    private static final String TAG_TICK_TIME_AVG = "tickTimeAvg";
    private static final String TAG_TICK_TIME_MAX = "tickTimeMax";
    private static final String TAG_TICK_TIME_SUM = "tickTimeSum";
    private static final String TAG_TICK_SLEEPING = "tickSleeping";
    private static final String TAG_TICK_ALERTABLE = "tickAlertable";
    private static final String TAG_TICK_AWAKE = "tickAwake";
    private static final String TAG_TICK_QUEUED = "tickQueued";
    private static final String TAG_TICK_CURRENT_RATE = "tickCurrentRate";
    private static final String TAG_TICK_LAST_TICK = "tickLastTick";
    private static final String TAG_NODE_EXPOSED = "exposedSides";

    private DebugProvider() {
    }

    public static void provideBlockEntityBody(TileEntity object, TooltipContext context, TooltipBuilder tooltip) {
        var player = context.player();
        if (!DebugProvider.isVisible(player)) {
            return;
        }

        DebugProvider.addBlockEntityRotation(object, tooltip);
        DebugProvider.addToTooltip(context.serverData(), tooltip);
    }

    public static void provideBlockEntityData(EntityPlayer player, TileEntity object, NBTTagCompound serverData) {
        if (object instanceof IGridConnectedBlockEntity gridConnected && DebugProvider.isVisible(player)) {
            DebugProvider.addServerDataMainNode(serverData, gridConnected.getMainNode());
        }
    }

    public static void providePartBody(AEBasePart object, TooltipContext context, TooltipBuilder tooltip) {
        DebugProvider.addToTooltip(context.serverData(), tooltip);
    }

    public static void providePartData(EntityPlayer player, AEBasePart part, NBTTagCompound serverData) {
        if (DebugProvider.isVisible(player)) {
            DebugProvider.addServerDataMainNode(serverData, part.getMainNode());
            DebugProvider.addServerDataNode(serverData, "External Node", part.getExternalFacingNode());
        }
    }

    private static void addBlockEntityRotation(TileEntity blockEntity, TooltipBuilder tooltip) {
        if (!(blockEntity instanceof AEBaseBlockEntity aeBlockEntity)) {
            return;
        }
        var blockState = aeBlockEntity.getBlockState();
        var strategy = IOrientationStrategy.get(blockState);
        if (!strategy.getProperties().isEmpty()) {
            var forward = strategy.getFacing(blockState);
            var spin = strategy.getSpin(blockState);
            tooltip.addLine(
                    new TextComponentString("")
                            .appendSibling(white(" Forward: "))
                            .appendSibling(new TextComponentString(forward.name()))
                            .appendSibling(white(" Spin: "))
                            .appendSibling(new TextComponentString(String.valueOf(spin))));
        }
    }

    private static void addToTooltip(NBTTagCompound serverData, TooltipBuilder tooltip) {
        var nodes = serverData.getTagList(TAG_NODES, Constants.NBT.TAG_COMPOUND);
        for (var i = 0; i < nodes.tagCount(); i++) {
            var nodeCompound = nodes.getCompoundTagAt(i);
            if (nodes.tagCount() > 1) {
                var nodeName = nodeCompound.getString(TAG_NODE_NAME);
                tooltip.addLine(TextUtil.withStyle(new TextComponentString(nodeName), TextFormatting.ITALIC));
            }

            addNodeToTooltip(nodeCompound, tooltip);
        }
    }

    private static void addNodeToTooltip(NBTTagCompound tag, TooltipBuilder tooltip) {
        if (tag.hasKey(TAG_TICK_TIME_AVG, Constants.NBT.TAG_LONG)) {
            var avg = tag.getLong(TAG_TICK_TIME_AVG);
            var max = tag.getLong(TAG_TICK_TIME_MAX);
            var sum = tag.getLong(TAG_TICK_TIME_SUM);

            tooltip.addLine(
                    new TextComponentString("")
                            .appendSibling(white("Tick Time: "))
                            .appendSibling(italic("Avg: "))
                            .appendSibling(white(Platform.formatTimeMeasurement(avg)))
                            .appendSibling(italic(" Max: "))
                            .appendSibling(white(Platform.formatTimeMeasurement(max)))
                            .appendSibling(italic(" Sum: "))
                            .appendSibling(white(Platform.formatTimeMeasurement(sum))));
        }

        if (tag.hasKey(TAG_TICK_QUEUED)) {
            var status = new ArrayList<String>();
            if (tag.getBoolean(TAG_TICK_SLEEPING)) {
                status.add("Sleeping");
            }
            if (tag.getBoolean(TAG_TICK_ALERTABLE)) {
                status.add("Alertable");
            }
            if (tag.getBoolean(TAG_TICK_AWAKE)) {
                status.add("Awake");
            }
            if (tag.getBoolean(TAG_TICK_QUEUED)) {
                status.add("Queued");
            }

            tooltip.addLine(
                    new TextComponentString("")
                            .appendSibling(white("Tick Status: "))
                            .appendText(String.join(", ", status)));
            tooltip.addLine(
                    new TextComponentString("")
                            .appendSibling(white("Tick Rate: "))
                            .appendText(String.valueOf(tag.getInteger(TAG_TICK_CURRENT_RATE)))
                            .appendSibling(white(" Last: "))
                            .appendText(tag.getLong(TAG_TICK_LAST_TICK) + " ticks ago"));
        }

        if (tag.hasKey(TAG_NODE_EXPOSED, Constants.NBT.TAG_INT)) {
            var exposedSides = tag.getInteger(TAG_NODE_EXPOSED);
            var line = white("Node Exposed: ");
            for (EnumFacing value : EnumFacing.values()) {
                var sideText = new TextComponentString(value.name().substring(0, 1));
                if ((exposedSides & (1 << value.ordinal())) == 0) {
                    TextUtil.withStyle(sideText, TextFormatting.GRAY);
                } else {
                    TextUtil.withStyle(sideText, TextFormatting.GREEN);
                }
                line.appendSibling(sideText);
            }
            tooltip.addLine(line);
        }
    }

    private static void addServerDataMainNode(NBTTagCompound tag, IManagedGridNode managedGridNode) {
        addServerDataNode(tag, "Main Node", managedGridNode.getNode());
    }

    private static void addServerDataNode(NBTTagCompound tag, String name, @Nullable IGridNode node) {
        var nodeTag = toServerData(node, name);
        if (nodeTag != null) {
            if (!tag.hasKey(TAG_NODES, Constants.NBT.TAG_LIST)) {
                tag.setTag(TAG_NODES, new NBTTagList());
            }
            tag.getTagList(TAG_NODES, Constants.NBT.TAG_COMPOUND).appendTag(nodeTag);
        }
    }

    @Nullable
    private static NBTTagCompound toServerData(@Nullable IGridNode node, String name) {
        if (node == null) {
            return null;
        }

        var tag = new NBTTagCompound();
        tag.setString(TAG_NODE_NAME, name);

        if (node.getService(IGridTickable.class) != null) {
            var tickManager = (TickManagerService) node.getGrid().getTickManager();
            tag.setLong(TAG_TICK_TIME_AVG, tickManager.getAverageTime(node));
            tag.setLong(TAG_TICK_TIME_MAX, tickManager.getMaximumTime(node));
            tag.setLong(TAG_TICK_TIME_SUM, tickManager.getOverallTime(node));

            var status = tickManager.getStatus(node);
            tag.setBoolean(TAG_TICK_SLEEPING, status.sleeping());
            tag.setBoolean(TAG_TICK_ALERTABLE, status.alertable());
            tag.setBoolean(TAG_TICK_AWAKE, status.awake());
            tag.setBoolean(TAG_TICK_QUEUED, status.queued());
            tag.setInteger(TAG_TICK_CURRENT_RATE, status.currentRate());
            tag.setLong(TAG_TICK_LAST_TICK, status.lastTick());
        }

        if (node instanceof InWorldGridNode inWorldNode) {
            // Record on which sides the node is exposed
            int exposedSides = 0;
            for (var value : EnumFacing.values()) {
                if (inWorldNode.isExposedOnSide(value)) {
                    exposedSides |= 1 << value.ordinal();
                }
            }
            tag.setInteger(TAG_NODE_EXPOSED, exposedSides);
        }

        return tag;
    }

    private static ITextComponent white(String text) {
        return TextUtil.withStyle(new TextComponentString(text), TextFormatting.WHITE);
    }

    private static ITextComponent italic(String text) {
        return TextUtil.withStyle(new TextComponentString(text), TextFormatting.ITALIC);
    }

    private static boolean isVisible(EntityPlayer player) {
        return AEItems.DEBUG_CARD.is(player.getHeldItem(EnumHand.OFF_HAND))
                || AEItems.DEBUG_CARD.is(player.getHeldItem(EnumHand.MAIN_HAND));
    }
}
