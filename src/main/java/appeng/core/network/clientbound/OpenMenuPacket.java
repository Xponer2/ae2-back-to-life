/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.network.clientbound;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.AELog;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.MenuScreens;
import appeng.menu.MenuType;

/**
 * Opens an AE2 menu on the client (1.12.2 replacement for the vanilla open screen packet with extra data). The data is
 * read by the {@link MenuType}'s client factory: menu host locator, "returned from sub menu" flag, initial data.
 */
public final class OpenMenuPacket implements ClientboundPacket {
    private final int windowId;
    private final ResourceLocation menuType;
    private final ITextComponent title;
    private final byte[] data;

    public OpenMenuPacket(int windowId, ResourceLocation menuType, ITextComponent title, byte[] data) {
        this.windowId = windowId;
        this.menuType = menuType;
        this.title = title;
        this.data = data;
    }

    public int windowId() {
        return windowId;
    }

    public ResourceLocation menuType() {
        return menuType;
    }

    public ITextComponent title() {
        return title;
    }

    public byte[] data() {
        return data;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPacket> STREAM_CODEC = StreamCodec.ofMember(
            OpenMenuPacket::write,
            OpenMenuPacket::decode);

    public static final Type<OpenMenuPacket> TYPE = CustomAppEngPayload.createType("open_menu");

    @Override
    public Type<OpenMenuPacket> type() {
        return TYPE;
    }

    public static OpenMenuPacket decode(RegistryFriendlyByteBuf buf) {
        var windowId = buf.readVarInt();
        var menuType = buf.readResourceLocation();
        var title = buf.readComponent();
        var data = buf.readByteArray();
        return new OpenMenuPacket(windowId, menuType, title, data);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(windowId);
        buf.writeResourceLocation(menuType);
        buf.writeComponent(title);
        buf.writeByteArray(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        var type = MenuType.byId(menuType);
        if (type == null) {
            AELog.warn("Server opened unknown AE2 menu type %s", menuType);
            closeOnServer();
            return;
        }
        open(type, player);
    }

    @SideOnly(Side.CLIENT)
    private <M extends Container> void open(MenuType<M> type, EntityPlayer player) {
        M menu;
        try {
            menu = type.create(windowId, player.inventory,
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data)));
        } catch (RuntimeException e) {
            AELog.warn(e, "Failed to open AE2 menu " + menuType + " on the client");
            closeOnServer();
            return;
        }
        menu.windowId = windowId;
        if (!MenuScreens.open(type, menu, player, title)) {
            closeOnServer();
        }
    }

    @SideOnly(Side.CLIENT)
    private void closeOnServer() {
        var connection = Minecraft.getMinecraft().getConnection();
        if (connection != null) {
            connection.sendPacket(new CPacketCloseWindow(windowId));
        }
    }
}
