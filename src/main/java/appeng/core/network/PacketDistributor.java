/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Same static methods as NeoForge's {@code PacketDistributor}, for the AE2 channel of {@link InitNetwork}. Call it on
 * the main thread of the sending side.
 */
public final class PacketDistributor {
    private PacketDistributor() {
    }

    public static void sendToServer(CustomAppEngPayload payload, CustomAppEngPayload... payloads) {
        var channel = InitNetwork.channel();
        channel.sendToServer(InitNetwork.toProxyPacket(InitNetwork.encode(payload, false)));
        for (var p : payloads) {
            channel.sendToServer(InitNetwork.toProxyPacket(InitNetwork.encode(p, false)));
        }
    }

    public static void sendToPlayer(EntityPlayerMP player, CustomAppEngPayload payload,
            CustomAppEngPayload... payloads) {
        var channel = InitNetwork.channel();
        for (byte[] bytes : encodeAll(payload, payloads)) {
            channel.sendTo(InitNetwork.toProxyPacket(bytes), player);
        }
    }

    public static void sendToPlayersInDimension(WorldServer level, CustomAppEngPayload payload,
            CustomAppEngPayload... payloads) {
        var channel = InitNetwork.channel();
        int dimension = level.provider.getDimension();
        for (byte[] bytes : encodeAll(payload, payloads)) {
            channel.sendToDimension(InitNetwork.toProxyPacket(bytes), dimension);
        }
    }

    public static void sendToPlayersNear(WorldServer level, @Nullable EntityPlayerMP excluded, double x, double y,
            double z, double radius, CustomAppEngPayload payload, CustomAppEngPayload... payloads) {
        var channel = InitNetwork.channel();
        var all = encodeAll(payload, payloads);
        double radiusSq = radius * radius;
        for (EntityPlayer p : level.playerEntities) {
            if (p != excluded && p instanceof EntityPlayerMP player && player.getDistanceSq(x, y, z) < radiusSq) {
                for (byte[] bytes : all) {
                    channel.sendTo(InitNetwork.toProxyPacket(bytes), player);
                }
            }
        }
    }

    public static void sendToAllPlayers(CustomAppEngPayload payload, CustomAppEngPayload... payloads) {
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        var channel = InitNetwork.channel();
        for (byte[] bytes : encodeAll(payload, payloads)) {
            channel.sendToAll(InitNetwork.toProxyPacket(bytes));
        }
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomAppEngPayload payload,
            CustomAppEngPayload... payloads) {
        var channel = InitNetwork.channel();
        for (byte[] bytes : encodeAll(payload, payloads)) {
            channel.sendToAllTracking(InitNetwork.toProxyPacket(bytes), entity);
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomAppEngPayload payload,
            CustomAppEngPayload... payloads) {
        sendToPlayersTrackingEntity(entity, payload, payloads);
        if (entity instanceof EntityPlayerMP player) {
            sendToPlayer(player, payload, payloads);
        }
    }

    public static void sendToPlayersTrackingChunk(WorldServer level, ChunkPos chunkPos, CustomAppEngPayload payload,
            CustomAppEngPayload... payloads) {
        var channel = InitNetwork.channel();
        var all = encodeAll(payload, payloads);
        var chunkMap = level.getPlayerChunkMap();
        for (EntityPlayer p : level.playerEntities) {
            if (p instanceof EntityPlayerMP player
                    && chunkMap.isPlayerWatchingChunk(player, chunkPos.x, chunkPos.z)) {
                for (byte[] bytes : all) {
                    channel.sendTo(InitNetwork.toProxyPacket(bytes), player);
                }
            }
        }
    }

    private static byte[][] encodeAll(CustomAppEngPayload payload, CustomAppEngPayload[] payloads) {
        byte[][] result = new byte[payloads.length + 1][];
        result[0] = InitNetwork.encode(payload, true);
        for (int i = 0; i < payloads.length; i++) {
            result[i + 1] = InitNetwork.encode(payloads[i], true);
        }
        return result;
    }
}
