/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.server.AECommand;
import appeng.server.ISubCommand;

public class ChunkLogger implements ISubCommand {

    private boolean enabled = false;

    private void displayStack() {
        if (AEConfig.instance().isChunkLoggerTraceEnabled()) {
            boolean output = false;
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                if (output) {
                    AELog.info(
                            "\t\t" + e.getClassName() + '.' + e.getMethodName() + " (" + e.getLineNumber() + ')');
                } else {
                    output = e.getClassName().contains("EventBus") && e.getMethodName().contains("post");
                }
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoadEvent(final ChunkEvent.Load event) {
        if (event.getWorld() instanceof WorldServer level) {
            var chunk = event.getChunk();
            AELog.info("Loaded chunk " + chunk.x + "," + chunk.z + " [center: " + getCenter(chunk) + "] in "
                    + level.provider.getDimensionType().getName());
            this.displayStack();
        }
    }

    @SubscribeEvent
    public void onChunkUnloadEvent(final ChunkEvent.Unload event) {
        if (event.getWorld() instanceof WorldServer level) {
            var chunk = event.getChunk();
            AELog.info("Unloaded chunk " + chunk.x + "," + chunk.z + " [center: " + getCenter(chunk) + "] in "
                    + level.provider.getDimensionType().getName());
            this.displayStack();
        }
    }

    private static String getCenter(Chunk chunk) {
        var x = chunk.getPos().getXStart() + 8;
        var z = chunk.getPos().getZStart() + 8;
        var y = chunk.getHeightValue(8, 8) + 1;
        return x + " " + y + " " + z;
    }

    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) {
        this.enabled = !this.enabled;

        if (this.enabled) {
            MinecraftForge.EVENT_BUS.register(this);
            AECommand.sendSuccess(sender, new TextComponentTranslation("commands.ae2.ChunkLoggerOn"));
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
            AECommand.sendSuccess(sender, new TextComponentTranslation("commands.ae2.ChunkLoggerOff"));
        }
    }
}
