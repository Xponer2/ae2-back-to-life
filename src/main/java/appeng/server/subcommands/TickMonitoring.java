/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021 TeamAppliedEnergistics, All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import appeng.me.service.TickManagerService;
import appeng.server.ISubCommand;

public class TickMonitoring implements ISubCommand {

    @Override
    public String getArgumentUsage() {
        return "<true|false>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        return args.length == 1 ? CommandBase.getListOfStringsMatchingLastWord(args, Arrays.asList("true", "false"))
                : Collections.emptyList();
    }

    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException("/ae2 tickmonitor " + getArgumentUsage());
        }
        TickManagerService.MONITORING_ENABLED = CommandBase.parseBoolean(args[0]);
    }
}
