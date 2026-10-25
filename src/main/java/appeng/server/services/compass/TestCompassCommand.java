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

package appeng.server.services.compass;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;

import appeng.core.localization.PlayerMessages;
import appeng.server.AECommand;
import appeng.server.ISubCommand;

public class TestCompassCommand implements ISubCommand {
    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) {
        var level = AECommand.getLevel(srv, sender);
        var chunkPos = new ChunkPos(sender.getPosition());
        var compassRegion = CompassRegion.get(level, chunkPos);

        // 16 sections of 16 blocks in 1.12.2
        for (var i = 0; i < 16; i++) {
            var hasSkyStone = compassRegion.hasCompassTarget(chunkPos.x, chunkPos.z, i);
            var yMin = i * 16;
            var yMax = (i + 1) * 16 - 1;
            AECommand.sendSuccess(sender, PlayerMessages.CompassTestSection.text(yMin, yMax, i, hasSkyStone));
        }
    }
}
