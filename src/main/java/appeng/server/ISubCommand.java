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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

/**
 * A sub command of {@code /ae2}. 1.12.2 has no brigadier: the arguments after the sub command's literal are passed as
 * strings, and the sub command parses them itself.
 */
public interface ISubCommand {

    /**
     * Usage of the arguments after the literal, e.g. {@code "[on|off]"}.
     */
    default String getArgumentUsage() {
        return "";
    }

    default List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        return Collections.emptyList();
    }

    void call(MinecraftServer srv, ICommandSender sender, String[] args) throws CommandException;
}
