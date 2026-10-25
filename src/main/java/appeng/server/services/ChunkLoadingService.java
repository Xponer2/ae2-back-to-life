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

package appeng.server.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import appeng.blockentity.spatial.SpatialAnchorBlockEntity;
import appeng.core.AppEngMod;

/**
 * Forced chunks of spatial anchors, on Forge's {@link ForgeChunkManager}: one ticket per anchor, the anchor's position
 * is stored in the ticket's mod data (1.21 uses block-owned tickets of a TicketController).
 */
public class ChunkLoadingService implements ForgeChunkManager.LoadingCallback {

    private static final ChunkLoadingService INSTANCE = new ChunkLoadingService();

    private static final String OWNER_TAG = "owner";

    // Flag to ignore a server after it is stopping as grid nodes might reevaluate their grids during a shutdown.
    private boolean running = true;

    private final Map<World, Map<BlockPos, Ticket>> tickets = new WeakHashMap<>();

    public void register() {
        ForgeChunkManager.setForcedChunkLoadingCallback(AppEngMod.instance, this);
    }

    public void onServerAboutToStart() {
        this.running = true;
    }

    public void onServerStopping() {
        this.running = false;
        this.tickets.clear();
    }

    public static ChunkLoadingService getInstance() {
        return INSTANCE;
    }

    @Override
    public void ticketsLoaded(List<Ticket> loadedTickets, World level) {
        // Re-attach the persisted chunks to their anchors, or drop the tickets of anchors that no longer exist.
        for (var ticket : loadedTickets) {
            var data = ticket.getModData();
            if (!data.hasKey(OWNER_TAG)) {
                ForgeChunkManager.releaseTicket(ticket);
                continue;
            }
            var owner = NBTUtil.getPosFromTag(data.getCompoundTag(OWNER_TAG));
            TileEntity blockEntity = level.getTileEntity(owner);
            if (blockEntity instanceof SpatialAnchorBlockEntity anchor) {
                tickets.computeIfAbsent(level, k -> new HashMap<>()).put(owner, ticket);
                for (var chunk : ticket.getChunkList()) {
                    ForgeChunkManager.forceChunk(ticket, chunk);
                    anchor.registerChunk(chunk);
                }
            } else {
                ForgeChunkManager.releaseTicket(ticket);
            }
        }
    }

    public boolean forceChunk(WorldServer level, BlockPos owner, ChunkPos position) {
        if (!running) {
            return false;
        }
        var byOwner = tickets.computeIfAbsent(level, k -> new HashMap<>());
        var ticket = byOwner.get(owner);
        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(AppEngMod.instance, level, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                // Forge's per-mod ticket limit (config/forgeChunkLoading.cfg) is reached
                return false;
            }
            ticket.getModData().setTag(OWNER_TAG, NBTUtil.createPosTag(owner));
            byOwner.put(owner, ticket);
        }
        ForgeChunkManager.forceChunk(ticket, position);
        return true;
    }

    public boolean releaseChunk(WorldServer level, BlockPos owner, ChunkPos position) {
        if (!running) {
            return false;
        }
        var byOwner = tickets.get(level);
        var ticket = byOwner != null ? byOwner.get(owner) : null;
        if (ticket == null) {
            return false;
        }
        ForgeChunkManager.unforceChunk(ticket, position);
        if (ticket.getChunkList().isEmpty()) {
            ForgeChunkManager.releaseTicket(ticket);
            byOwner.remove(owner);
        }
        return true;
    }

}
