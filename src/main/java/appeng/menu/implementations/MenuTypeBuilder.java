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

package appeng.menu.implementations;

import java.util.function.Function;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IWorldNameable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;

import appeng.core.AppEng;
import appeng.core.network.PacketDistributor;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.clientbound.OpenMenuPacket;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.MenuType;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.locator.MenuLocators;

/**
 * Builder that allows creation of menu types which can be opened from multiple types of hosts.
 */
public final class MenuTypeBuilder<M extends AEBaseMenu, I> {

    @Nullable
    private ResourceLocation id;

    private final Class<I> hostInterface;

    private final MenuFactory<M, I> factory;

    private Function<I, ITextComponent> menuTitleStrategy = this::getDefaultMenuTitle;

    @Nullable
    private InitialDataSerializer<I> initialDataSerializer;

    @Nullable
    private InitialDataDeserializer<M, I> initialDataDeserializer;

    private MenuType<M> menuType;

    private MenuTypeBuilder(Class<I> hostInterface, TypedMenuFactory<M, I> typedFactory) {
        this.hostInterface = hostInterface;
        this.factory = (containerId, playerInv, accessObj) -> typedFactory.create(menuType, containerId, playerInv,
                accessObj);
    }

    private MenuTypeBuilder(Class<I> hostInterface, MenuFactory<M, I> factory) {
        this.hostInterface = hostInterface;
        this.factory = factory;
    }

    public static <C extends AEBaseMenu, I> MenuTypeBuilder<C, I> create(MenuFactory<C, I> factory,
            Class<I> hostInterface) {
        return new MenuTypeBuilder<>(hostInterface, factory);
    }

    public static <C extends AEBaseMenu, I> MenuTypeBuilder<C, I> create(TypedMenuFactory<C, I> factory,
            Class<I> hostInterface) {
        return new MenuTypeBuilder<>(hostInterface, factory);
    }

    /**
     * Specifies a custom strategy for obtaining a custom menu name.
     * <p>
     * The strategy should return {@link ITextComponent#empty()} if there's no custom name.
     */
    public MenuTypeBuilder<M, I> withMenuTitle(Function<I, ITextComponent> menuTitleStrategy) {
        this.menuTitleStrategy = menuTitleStrategy;
        return this;
    }

    /**
     * Sets a serializer and deserializer for additional data that should be transmitted from server->client when the
     * menu is being first opened.
     */
    public MenuTypeBuilder<M, I> withInitialData(InitialDataSerializer<I> initialDataSerializer,
            InitialDataDeserializer<M, I> initialDataDeserializer) {
        this.initialDataSerializer = initialDataSerializer;
        this.initialDataDeserializer = initialDataDeserializer;
        return this;
    }

    /**
     * Opens a menu that is based around a single block entity. The block entity's position is encoded in the packet
     * buffer.
     */
    private M fromNetwork(int containerId, InventoryPlayer inv, RegistryFriendlyByteBuf packetBuf) {
        var locator = MenuLocators.readFromPacket(packetBuf);
        I host = locator.locate(inv.player, hostInterface);
        if (host == null) {
            // OpenMenuPacket closes the menu on the server when this fails
            throw new IllegalStateException("Couldn't find menu host at " + locator + " for " + this.id
                    + " on client. Closing menu.");
        }
        M menu = factory.create(containerId, inv, host);
        menu.setReturnedFromSubScreen(packetBuf.readBoolean());
        if (initialDataDeserializer != null) {
            initialDataDeserializer.deserializeInitialData(host, menu, packetBuf);
        }
        return menu;
    }

    private boolean open(EntityPlayer player, MenuHostLocator locator, boolean fromSubMenu) {
        if (!(player instanceof EntityPlayerMP serverPlayer)) {
            // Cannot open menus on the client or for non-players
            return false;
        }

        var accessInterface = locator.locate(player, hostInterface);

        if (accessInterface == null) {
            return false;
        }

        ITextComponent title = menuTitleStrategy.apply(accessInterface);

        // Data the client needs to create its menu instance (see fromNetwork)
        var buffer = RegistryFriendlyByteBuf.create();
        MenuLocators.writeToPacket(buffer, locator);
        buffer.writeBoolean(fromSubMenu);
        if (initialDataSerializer != null) {
            initialDataSerializer.serializeInitialData(accessInterface, buffer);
        }
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        buffer.release();

        // Switching between AE menus must not close the screen on the client (1.21:
        // shouldTriggerClientSideContainerClosingOnOpen), other open containers are closed like vanilla does it.
        // The item on the cursor lives in the player inventory in 1.12.2 and closing a container drops it, so it is
        // kept across the switch like in 1.21.
        var carried = ItemStack.EMPTY;
        if (serverPlayer.openContainer instanceof AEBaseMenu) {
            carried = serverPlayer.inventory.getItemStack();
            serverPlayer.inventory.setItemStack(ItemStack.EMPTY);
            serverPlayer.closeContainer();
        } else if (serverPlayer.openContainer != serverPlayer.inventoryContainer) {
            serverPlayer.closeScreen();
        }

        serverPlayer.getNextWindowId();
        int windowId = serverPlayer.currentWindowId;
        M menu = factory.create(windowId, serverPlayer.inventory, accessInterface);
        // Set the original locator on the opened server-side menu for it to more
        // easily remember how to re-open after being closed.
        menu.setLocator(locator);

        // Same order as vanilla: open packet first, then the listener sends the slot contents
        PacketDistributor.sendToPlayer(serverPlayer, new OpenMenuPacket(windowId, id, title, data));
        serverPlayer.openContainer = menu;
        menu.windowId = windowId;
        menu.addListener(serverPlayer);
        if (!carried.isEmpty()) {
            serverPlayer.inventory.setItemStack(carried);
            serverPlayer.updateHeldItem();
        }
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(serverPlayer, menu));
        return true;
    }

    public MenuType<M> build(String id) {
        return build(AppEng.makeId(id));
    }

    /**
     * Creates a menu type that uses this helper as a factory and network deserializer.
     */
    public MenuType<M> buildUnregistered(ResourceLocation id) {
        Preconditions.checkState(menuType == null, "build was already called");
        Preconditions.checkState(this.id == null, "id should not be set");

        this.id = id;
        menuType = new MenuType<>(id, this::fromNetwork);
        MenuOpener.addOpener(menuType, this::open);
        return menuType;
    }

    /**
     * Creates a menu type that uses this helper as a factory and network deserializer, and registers it (1.12.2 has no
     * menu registry, {@link MenuType#byId} is used by the client).
     */
    public MenuType<M> build(ResourceLocation id) {
        return MenuType.register(buildUnregistered(id));
    }

    @FunctionalInterface
    public interface MenuFactory<C, I> {
        C create(int containerId, InventoryPlayer playerInv, I menuHost);
    }

    @FunctionalInterface
    public interface TypedMenuFactory<C extends Container, I> {
        C create(MenuType<C> type, int containerId, InventoryPlayer playerInv, I accessObj);
    }

    /**
     * Strategy used to serialize initial data for opening the menu on the client-side into the packet that is sent to
     * the client.
     */
    @FunctionalInterface
    public interface InitialDataSerializer<I> {
        void serializeInitialData(I host, RegistryFriendlyByteBuf buffer);
    }

    /**
     * Strategy used to deserialize initial data for opening the menu on the client-side from the packet received by the
     * server.
     */
    @FunctionalInterface
    public interface InitialDataDeserializer<C, I> {
        void deserializeInitialData(I host, C menu, RegistryFriendlyByteBuf buffer);
    }

    private ITextComponent getDefaultMenuTitle(I accessInterface) {
        if (accessInterface instanceof IWorldNameable nameable) {
            if (nameable.hasCustomName()) {
                return nameable.getDisplayName();
            }
        }

        return new TextComponentString("");
    }

}
