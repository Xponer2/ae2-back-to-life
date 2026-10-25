/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.server.subcommands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.api.features.IPlayerRegistry;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.items.storage.SpatialStorageCellItem;
import appeng.server.AECommand;
import appeng.server.ISubCommand;
import appeng.spatial.SpatialStorageDimensionIds;
import appeng.spatial.SpatialStoragePlot;
import appeng.spatial.SpatialStoragePlotManager;
import appeng.spatial.TransitionInfo;

public class SpatialStorageCommand implements ISubCommand {

    @Override
    public String getArgumentUsage() {
        return "[info [plotId] | tp <plotId> | tpback [plotId] | givecell <plotId>]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        return args.length == 1
                ? CommandBase.getListOfStringsMatchingLastWord(args, Arrays.asList("info", "tp", "tpback", "givecell"))
                : Collections.emptyList();
    }

    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            listPlots(sender);
            return;
        }
        switch (args[0]) {
            // Shows info about a given plot or about the plot the player is currently in
            case "info" -> showPlotInfo(srv, sender, args.length > 1 ? getPlot(plotId(args)) : getCurrentPlot(sender));
            // Teleport into the plot
            case "tp" -> teleportToPlot(sender, plotId(args));
            // Teleport from the current plot back to the source of its content, or do the
            // same for a given plot id
            case "tpback" -> {
                if (args.length > 1) {
                    teleportBack(sender, getPlot(plotId(args)));
                } else {
                    teleportBack(sender, getCurrentPlot(sender));
                }
            }
            // Creates a storage cell for the given plot id and gives it to the player
            case "givecell" -> giveCell(sender, plotId(args));
            default -> throw new WrongUsageException("/ae2 spatial " + getArgumentUsage());
        }
    }

    private int plotId(String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("/ae2 spatial " + getArgumentUsage());
        }
        return CommandBase.parseInt(args[1], 1);
    }

    /**
     * Teleports back from the given plot.
     */
    private void teleportBack(ICommandSender source, SpatialStoragePlot plot) throws CommandException {
        TransitionInfo lastTransition = plot.getLastTransition();
        if (lastTransition == null) {
            throw new CommandException(PlayerMessages.NoLastTransition.getTranslationKey());
        }

        AECommand.teleport(AECommand.getPlayer(source), lastTransition.getDimension(),
                lastTransition.getMin().up());
    }

    /**
     * Shows detailed information about a spatial storage plot.
     */
    private static void showPlotInfo(MinecraftServer server, ICommandSender source, SpatialStoragePlot plot) {

        sendKeyValuePair(source, PlayerMessages.PlotID.text(), String.valueOf(plot.getId()));
        // Show the owner of the spatial storage plot
        int playerId = plot.getOwner();
        if (playerId != -1) {
            var profileId = IPlayerRegistry.getMapping(server).getProfileId(playerId);

            if (profileId == null) {
                sendKeyValuePair(source, PlayerMessages.Owner.text(), PlayerMessages.UnknownAE2Player.text(playerId));
            } else {
                var player = server.getPlayerList().getPlayerByUUID(profileId);
                if (player != null) {
                    sendKeyValuePair(source, PlayerMessages.Owner.text(),
                            PlayerMessages.PlayerConnected.text(player.getGameProfile().getName()));
                } else {
                    var cachedProfile = server.getPlayerProfileCache().getProfileByUUID(profileId);
                    if (cachedProfile != null) {
                        sendKeyValuePair(source, PlayerMessages.Owner.text(),
                                PlayerMessages.PlayerDisconnected.text(cachedProfile.getName()));
                    } else {
                        sendKeyValuePair(source, PlayerMessages.Owner.text(),
                                PlayerMessages.MinecraftProfile.text(profileId));
                    }
                }
            }
        } else {
            sendKeyValuePair(source, PlayerMessages.Owner.text(), PlayerMessages.Unknown.text());
        }

        sendKeyValuePair(source, PlayerMessages.Size.text(), formatBlockPos(plot.getSize(), "x"));

        // Show the plot's origin and make it clickable to teleport directly to it
        var origin = new TextComponentString(formatBlockPos(plot.getOrigin(), ","));
        makeCommandLink(origin, "/ae2 spatial tp " + plot.getId(), PlayerMessages.ClickToTeleport.text());
        sendKeyValuePair(source, PlayerMessages.Origin.text(), origin);

        sendKeyValuePair(source, PlayerMessages.RegionFile.text(), plot.getRegionFilename());

        // Show information about what was last transfered into the plot (with a
        // clickable link to the source)
        TransitionInfo lastTransition = plot.getLastTransition();
        if (lastTransition != null) {
            var title = PlayerMessages.LastTransition.text();
            title.getStyle().setUnderlined(true).setBold(true);
            AECommand.sendSuccess(source, title);

            String sourceWorldId = String.valueOf(lastTransition.getDimension());
            ITextComponent sourceLink = PlayerMessages.SourceLink.text(sourceWorldId,
                    formatBlockPos(lastTransition.getMin(), ","), formatBlockPos(lastTransition.getMax(), ","));
            makeCommandLink(sourceLink, "/ae2 spatial tpback " + plot.getId(), PlayerMessages.ClickToTeleport.text());

            sendKeyValuePair(source, PlayerMessages.Source.text(), sourceLink);
            sendKeyValuePair(source, PlayerMessages.When.text(), lastTransition.getTimestamp().toString());
        } else {
            AECommand.sendSuccess(source, PlayerMessages.LastTransitionUnknown.text());
        }

    }

    private static void teleportToPlot(ICommandSender source, int plotId) throws CommandException {
        SpatialStoragePlot plot = getPlot(plotId);
        AECommand.teleport(AECommand.getPlayer(source), SpatialStorageDimensionIds.getDimensionId(),
                plot.getOrigin().up());
    }

    private void giveCell(ICommandSender source, int plotId) throws CommandException {
        var player = AECommand.getPlayer(source);

        SpatialStoragePlot plot = getPlot(plotId);

        ItemStack cell;
        int longestSide = getLongestSide(plot.getSize());
        if (longestSide <= 2) {
            cell = AEItems.SPATIAL_CELL2.stack();
        } else if (longestSide <= 16) {
            cell = AEItems.SPATIAL_CELL16.stack();
        } else {
            cell = AEItems.SPATIAL_CELL128.stack();
        }

        if (!(cell.getItem() instanceof SpatialStorageCellItem spatialCellItem)) {
            throw new CommandException(PlayerMessages.NotStorageCell.getTranslationKey());
        }

        spatialCellItem.setStoredDimension(cell, plotId, plot.getSize());

        ItemHandlerHelper.giveItemToPlayer(player, cell);
    }

    private static int getLongestSide(BlockPos size) {
        return Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
    }

    private void listPlots(ICommandSender sender) {

        // Test if the storage level has gone missing, which has occurred in the past. Instead of just not
        // printing anything or a non-descript error, give the user some hint as to what's happening.
        try {
            SpatialStoragePlotManager.INSTANCE.getLevel();
        } catch (IllegalStateException e) {
            AECommand.sendSuccess(sender, PlayerMessages.NoSpatialIOLevel.text(e.getMessage()));
            return;
        }

        List<SpatialStoragePlot> plots = new ArrayList<>(SpatialStoragePlotManager.INSTANCE.getPlots());

        if (plots.isEmpty()) {
            AECommand.sendSuccess(sender, PlayerMessages.NoSpatialIOPlots.text());
            return;
        }

        // Prints the least recently used plots
        plots.sort(Comparator.comparing((SpatialStoragePlot plot) -> {
            TransitionInfo lastTransition = plot.getLastTransition();
            if (lastTransition != null) {
                return lastTransition.getTimestamp();
            } else {
                return Instant.MIN;
            }
        }).reversed());

        for (int i = 0; i < Math.min(5, plots.size()); i++) {
            SpatialStoragePlot plot = plots.get(i);
            String size = formatBlockPos(plot.getSize(), "x");
            BlockPos originPos = plot.getOrigin();
            String origin = formatBlockPos(originPos, ",");

            ITextComponent infoLink = PlayerMessages.Plot.text().appendText(" #" + plot.getId());
            makeCommandLink(infoLink, "/ae2 spatial info " + plot.getId(), PlayerMessages.ClickToShowDetails.text());
            ITextComponent tpLink = PlayerMessages.Origin.text().appendText(": " + origin);
            makeCommandLink(tpLink, "/ae2 spatial tp " + plot.getId(), PlayerMessages.ClickToTeleport.text());

            ITextComponent message = new TextComponentString("").appendSibling(infoLink).appendText(" ")
                    .appendSibling(PlayerMessages.Size.text()).appendText(": " + size + " ").appendSibling(tpLink);

            AECommand.sendSuccess(sender, message);
        }

    }

    private static String formatBlockPos(BlockPos size, String separator) {
        return size.getX() + separator + size.getY() + separator + size.getZ();
    }

    private static void makeCommandLink(ITextComponent component, String command, ITextComponent tooltip) {
        component.getStyle().setUnderlined(true)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));
    }

    private static SpatialStoragePlot getPlot(int plotId) throws CommandException {
        SpatialStoragePlot plot = SpatialStoragePlotManager.INSTANCE.getPlot(plotId);
        if (plot == null) {
            throw new CommandException(PlayerMessages.PlotNotFound.getTranslationKey(), plotId);
        }
        return plot;
    }

    private static void sendKeyValuePair(ICommandSender source, ITextComponent label, ITextComponent value) {
        var line = label.createCopy().appendText(": ");
        line.getStyle().setBold(true);
        AECommand.sendSuccess(source, new TextComponentString("").appendSibling(line).appendSibling(value));
    }

    private static void sendKeyValuePair(ICommandSender source, ITextComponent label, String value) {
        sendKeyValuePair(source, label, new TextComponentString(value));
    }

    /**
     * Gets the spatial storage plot that the command source is currently in.
     */
    private static SpatialStoragePlot getCurrentPlot(ICommandSender source) throws CommandException {
        var world = source.getEntityWorld();
        if (world.provider.getDimension() != SpatialStorageDimensionIds.getDimensionId()) {
            throw new CommandException(PlayerMessages.NotInSpatialStorageLevel.getTranslationKey());
        }

        BlockPos playerPos = source.getPosition();
        int x = playerPos.getX();
        int z = playerPos.getZ();

        // This is slow, but for an admin-command it's acceptable
        for (SpatialStoragePlot plot : SpatialStoragePlotManager.INSTANCE.getPlots()) {
            BlockPos origin = plot.getOrigin();
            BlockPos size = plot.getSize();
            if (x >= origin.getX() && x <= origin.getX() + size.getX() && z >= origin.getZ()
                    && z <= origin.getZ() + size.getZ()) {
                return plot;
            }
        }

        throw new CommandException(PlayerMessages.PlotNotFoundForCurrentPosition.getTranslationKey());
    }

}
