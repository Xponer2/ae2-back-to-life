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

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;

/**
 * Receives the AE2 packets on the client (registered by {@link InitNetwork} on the client only).
 */
@SideOnly(Side.CLIENT)
public final class ClientNetworkHandler {
    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        CustomAppEngPayload packet;
        try {
            packet = InitNetwork.decode(event.getPacket(), true);
        } catch (RuntimeException e) {
            AELog.warn(e, "Failed to decode an AE2 packet from the server");
            return;
        }
        if (packet instanceof ClientboundPacket clientboundPacket) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if (mc.player != null) {
                    clientboundPacket.handleOnClient(mc.player);
                }
            });
        }
    }
}
