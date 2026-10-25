/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.google.common.math.StatsAccumulator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.WorldServer;

import appeng.server.AECommand;
import appeng.server.ISubCommand;
import appeng.worldgen.meteorite.MeteoriteSpawnData;
import appeng.worldgen.meteorite.PlacedMeteoriteSettings;

/**
 * Lists the meteorites around the player. 1.12.2 has no structure starts: the meteorites decided so far are in
 * {@link MeteoriteSpawnData}; "force" generates the chunks of the closest ones to finalize them.
 */
public class TestMeteoritesCommand implements ISubCommand {

    @Override
    public String getArgumentUsage() {
        return "[force]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        return args.length == 1 ? CommandBase.getListOfStringsMatchingLastWord(args, "force")
                : Collections.emptyList();
    }

    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) {
        test(srv, sender, args.length > 0 && args[0].equals("force"));
    }

    private static void test(MinecraftServer srv, ICommandSender sender, boolean force) {
        int radius = 100;

        WorldServer level;
        BlockPos centerBlock;
        if (sender instanceof EntityPlayerMP player) {
            level = player.getServerWorld();
            centerBlock = new BlockPos(player.posX, 0, player.posZ);
        } else {
            level = srv.getWorld(0);
            centerBlock = level.getSpawnPoint();
        }

        ChunkPos center = new ChunkPos(centerBlock);
        var data = MeteoriteSpawnData.get(level);

        // Find all meteorites in the given rectangle
        List<PlacedMeteoriteSettings> found = new ArrayList<>(data.getMeteoritesNear(center, radius));
        int chunksChecked = (2 * radius + 1) * (2 * radius + 1);

        // Create stats on how far apart the meteorites are
        StatsAccumulator stats = new StatsAccumulator();
        for (PlacedMeteoriteSettings settings : found) {
            double closestOther = Double.NaN;
            for (PlacedMeteoriteSettings otherSettings : found) {
                if (otherSettings != settings) {
                    double d = settings.getPos().distanceSq(otherSettings.getPos());
                    if (Double.isNaN(closestOther) || d < closestOther) {
                        closestOther = d;
                    }
                }
            }

            if (!Double.isNaN(closestOther)) {
                stats.add(Math.sqrt(closestOther));
            }
        }

        found.sort(Comparator.comparingDouble(settings -> settings.getPos().distanceSq(centerBlock)));

        sendLine(sender, "Chunks checked: %d", chunksChecked);
        sendLine(sender, "Meteorites found: %d", found.size());
        if (stats.count() > 0) {
            sendLine(sender, "Closest: min=%.2f max=%.2f mean=%.2f stddev=%.2f", stats.min(), stats.max(), stats.mean(),
                    stats.populationStandardDeviation());
        }

        int closestCount = Math.min(10, found.size());
        for (int i = 0; i < closestCount; i++) {
            PlacedMeteoriteSettings settings = found.get(i);
            BlockPos pos = settings.getPos();

            String state = "not final";

            if (force && settings.getFallout() == null) {
                // Generating the chunk places the meteorite and finalizes its settings
                level.getChunk(pos);
                var placed = findAt(data.getMeteoritesNear(new ChunkPos(pos), 1), pos);
                if (placed == null) {
                    state = "removed";
                } else {
                    settings = placed;
                    pos = settings.getPos();
                }
            }

            ITextComponent restOfLine;
            if (settings.getFallout() == null) {
                restOfLine = new TextComponentString(
                        String.format(Locale.ROOT, ", radius=%.2f [%s]", settings.getMeteoriteRadius(), state));
            } else {
                restOfLine = new TextComponentString(String.format(Locale.ROOT, ", radius=%.2f, crater=%s, fallout=%s",
                        settings.getMeteoriteRadius(), settings.getCraterType().name().toLowerCase(Locale.ROOT),
                        settings.getFallout().name().toLowerCase(Locale.ROOT)));
            }

            ITextComponent msg = new TextComponentString(" #" + (i + 1) + " ");
            msg.appendSibling(getClickablePosition(level, settings, pos)).appendSibling(restOfLine);

            // Add a tooltip
            var biome = level.getBiome(pos).getRegistryName();
            String biomeId = biome != null ? biome.toString() : "unknown";
            ITextComponent tooltip = new TextComponentString(settings + "\nBiome: " + biomeId);
            msg.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));

            AECommand.sendSuccess(sender, msg);
        }
    }

    @Nullable
    private static PlacedMeteoriteSettings findAt(List<PlacedMeteoriteSettings> list, BlockPos pos) {
        for (var settings : list) {
            if (settings.getPos().equals(pos)) {
                return settings;
            }
        }
        return null;
    }

    // Add a clickable link to teleport the user to the Meteorite
    private static ITextComponent getClickablePosition(WorldServer level, PlacedMeteoriteSettings settings,
            BlockPos pos) {
        BlockPos tpPos = pos.up((int) Math.ceil(settings.getMeteoriteRadius()));
        int surfaceY = level.getHeight(tpPos).getY();
        if (surfaceY > tpPos.getY()) {
            tpPos = new BlockPos(tpPos.getX(), surfaceY, tpPos.getZ());
        }

        String displayText = String.format(Locale.ROOT, "pos=%d,%d,%d", tpPos.getX(), tpPos.getY(), tpPos.getZ());
        String tpCommand = String.format(Locale.ROOT, "/tp @s %d %d %d", tpPos.getX(), tpPos.getY(), tpPos.getZ());

        var link = new TextComponentString(displayText);
        link.getStyle().setUnderlined(true).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand));
        return link;
    }

    private static void sendLine(ICommandSender sender, String text, Object... args) {
        AECommand.sendSuccess(sender, new TextComponentString(String.format(Locale.ROOT, text, args)));
    }

}
