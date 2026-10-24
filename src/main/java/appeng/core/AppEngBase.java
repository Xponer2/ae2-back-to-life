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

package appeng.core;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

import appeng.api.capabilities.AECapabilityRegistration;
import appeng.api.capabilities.RegisterCapabilitiesEvent;
import appeng.api.parts.CableRenderMode;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypesInternal;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEEntities;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.InitNetwork;
import appeng.core.network.PacketDistributor;
import appeng.core.tags.TagManager;
import appeng.hooks.SkyStoneBreakSpeed;
import appeng.hooks.WrenchHook;
import appeng.hooks.ticking.TickHandler;
import appeng.hotkeys.HotkeyActions;
import appeng.init.InitAdvancementTriggers;
import appeng.init.InitCapabilityProviders;
import appeng.init.InitDispenserBehavior;
import appeng.init.InitStats;
import appeng.init.internal.InitBlockEntityMoveStrategies;
import appeng.init.internal.InitGridLinkables;
import appeng.init.internal.InitGridServices;
import appeng.init.internal.InitP2PAttunements;
import appeng.init.internal.InitStorageCells;
import appeng.init.internal.InitUpgrades;
import appeng.server.services.ChunkLoadingService;
import appeng.sounds.AppEngSounds;

/**
 * Mod functionality that is common to both dedicated server and client.
 * <p>
 * Note that a client will still have zero or more embedded servers (although only one at a time).
 */
public abstract class AppEngBase implements AppEng {
    /**
     * While we process a player-specific part placement/cable interaction packet, we need to use that player's
     * transparent-facade mode to understand whether the player can see through facades or not.
     * <p>
     * We need to use this method since the collision shape methods do not know about the player that the shape is being
     * requested for, so they will call {@link #getCableRenderMode()} below, which then will use this field to figure
     * out which player it's for.
     */
    private final ThreadLocal<EntityPlayer> partInteractionPlayer = new ThreadLocal<>();

    static AppEngBase INSTANCE;

    public AppEngBase() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // FML lifecycle
    // ---------------------------------------------------------------------------------------------------------------

    public void preInit(FMLPreInitializationEvent event) {
        AEConfig.register(event.getModConfigurationDirectory());
        AECapabilityRegistration.register();

        registerKeyTypes();
        InitGridServices.init();
        InitBlockEntityMoveStrategies.init();
        AEParts.init();
        FacadeCreativeTab.init();

        InitNetwork.init();
        TickHandler.instance().init();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(WrenchHook.class);
        MinecraftForge.EVENT_BUS.register(SkyStoneBreakSpeed.class);
        HotkeyActions.init();
    }

    public void init(FMLInitializationEvent event) {
        postRegistrationInitialization();
        var capabilities = RegisterCapabilitiesEvent.get();
        InitCapabilityProviders.markProxyableCapabilities(capabilities);
        InitCapabilityProviders.register(capabilities);
        InitCapabilityProviders.registerGenericAdapters(capabilities);
        InitAdvancementTriggers.init();
        InitStats.init();
        appeng.core.compat.OreDictionaryCompat.register();
    }

    public void postInit(FMLPostInitializationEvent event) {
        // All mods registered their ore dictionary entries now
        TagManager.invalidate();
        appeng.integration.Integrations.init();
    }

    /**
     * Runs after all mods have had time to run their registrations into registries.
     */
    public void postRegistrationInitialization() {
        InitGridLinkables.init();
        InitStorageCells.init();
        InitP2PAttunements.init();
        InitDispenserBehavior.init();
        InitUpgrades.init();
    }

    public void registerKeyTypes() {
        AEKeyTypesInternal.register(AEKeyType.items());
        AEKeyTypesInternal.register(AEKeyType.fluids());
    }

    public void onServerAboutToStart(FMLServerAboutToStartEvent evt) {
        ChunkLoadingService.getInstance().onServerAboutToStart(evt.getServer());
    }

    public void onServerStarting(FMLServerStartingEvent evt) {
        appeng.server.AECommand.register(evt);
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        ChunkLoadingService.getInstance().onServerStopping();
    }

    public void onServerStopped(FMLServerStoppedEvent event) {
        TickHandler.instance().shutdown();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Forge registry events
    // ---------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        AEBlocks.registerBlocks(event.getRegistry());
        AEBlockEntities.register();
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        AEBlocks.registerItems(event.getRegistry());
        AEItems.registerItems(event.getRegistry());
    }

    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        AppEngSounds.register(event.getRegistry());
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        AEEntities.register(event.getRegistry());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        appeng.recipes.loader.RecipeLoader.loadAll(event.getRegistry());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // AppEng API
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public Collection<EntityPlayerMP> getPlayers() {
        var server = getCurrentServer();
        if (server != null) {
            return server.getPlayerList().getPlayers();
        }

        return Collections.emptyList();
    }

    @Override
    public void sendToAllNearExcept(EntityPlayer p, double x, double y, double z,
            double dist, World level, ClientboundPacket packet) {
        if (level instanceof WorldServer serverLevel) {
            EntityPlayerMP except = null;
            if (p instanceof EntityPlayerMP) {
                except = (EntityPlayerMP) p;
            }
            PacketDistributor.sendToPlayersNear(serverLevel, except, x, y, z, dist, packet);
        }
    }

    @Override
    public void setPartInteractionPlayer(EntityPlayer player) {
        this.partInteractionPlayer.set(player);
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        return this.getCableRenderModeForPlayer(partInteractionPlayer.get());
    }

    @Nullable
    @Override
    public MinecraftServer getCurrentServer() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    protected final CableRenderMode getCableRenderModeForPlayer(@Nullable EntityPlayer player) {
        if (player != null) {
            if (AEItems.NETWORK_TOOL.is(player.getHeldItem(EnumHand.MAIN_HAND))
                    || AEItems.NETWORK_TOOL.is(player.getHeldItem(EnumHand.OFF_HAND))) {
                return CableRenderMode.CABLE_VIEW;
            }
        }

        return CableRenderMode.STANDARD;
    }
}
