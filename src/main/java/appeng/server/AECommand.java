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

package appeng.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;

import appeng.core.AEConfig;

/**
 * {@code /ae2 <sub command> ...}: the 1.21 brigadier command tree on top of 1.12.2's {@link CommandBase}.
 */
public final class AECommand extends CommandBase {

    @Override
    public String getName() {
        return "ae2";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        var literals = new ArrayList<String>();
        for (var command : Commands.values()) {
            if (isAvailable(command, sender)) {
                literals.add(command.literal());
            }
        }
        return "/ae2 <" + String.join("|", literals) + ">";
    }

    @Override
    public int getRequiredPermissionLevel() {
        // each sub command checks its own level
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        var command = find(args[0]);
        if (command == null || !isAvailable(command, sender)) {
            throw new WrongUsageException(getUsage(sender));
        }
        command.command.call(server, sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            var literals = new ArrayList<String>();
            for (var command : Commands.values()) {
                if (isAvailable(command, sender)) {
                    literals.add(command.literal());
                }
            }
            return getListOfStringsMatchingLastWord(args, literals);
        }
        var command = find(args[0]);
        if (command == null || !isAvailable(command, sender)) {
            return Collections.emptyList();
        }
        return command.command.getTabCompletions(server, sender, Arrays.copyOfRange(args, 1, args.length),
                targetPos);
    }

    @Nullable
    private static Commands find(String literal) {
        for (var command : Commands.values()) {
            if (command.literal().equals(literal)) {
                return command;
            }
        }
        return null;
    }

    private static boolean isAvailable(Commands command, ICommandSender sender) {
        if (command.test && !AEConfig.instance().isDebugToolsEnabled()) {
            return false;
        }
        return sender.canUseCommand(command.level, "ae2");
    }

    /**
     * 1.21 {@code CommandSourceStack.sendSuccess}.
     */
    public static void sendSuccess(ICommandSender sender, ITextComponent message) {
        sender.sendMessage(message);
    }

    /**
     * Teleports a player to a position, changing the dimension if needed (1.21 runs {@code /execute in ... tp}, which
     * 1.12.2 doesn't have).
     */
    public static void teleport(EntityPlayerMP player, int dimension, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (player.dimension != dimension) {
            player.changeDimension(dimension, new ITeleporter() {
                @Override
                public void placeEntity(net.minecraft.world.World world, Entity entity, float yaw) {
                    entity.setLocationAndAngles(x, y, z, yaw, entity.rotationPitch);
                    entity.motionX = entity.motionY = entity.motionZ = 0;
                }

                @Override
                public boolean isVanilla() {
                    return false;
                }
            });
        }
        player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
    }

    /**
     * The player executing the command, or an exception like 1.21's {@code getPlayerOrException}.
     */
    public static EntityPlayerMP getPlayer(ICommandSender sender) throws CommandException {
        return getCommandSenderAsPlayer(sender);
    }

    public static WorldServer getLevel(MinecraftServer server, ICommandSender sender) {
        var world = sender.getEntityWorld();
        return world instanceof WorldServer ws ? ws : server.getWorld(0);
    }
}
