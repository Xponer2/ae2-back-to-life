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

import java.util.Objects;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.parts.CableRenderMode;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.EffectType;
import appeng.client.commands.ClientCommands;
import appeng.client.Hotkeys;
import appeng.client.gui.me.common.PendingCraftingJobs;
import appeng.client.gui.me.common.PinnedKeys;
import appeng.client.gui.style.StyleManager;
import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.EnergyParticleData;
import appeng.client.render.effects.ParticleTypes;
import appeng.core.definitions.AEAttachmentTypes;
import appeng.core.network.PacketDistributor;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.MouseWheelPacket;
import appeng.core.network.serverbound.UpdateHoldingCtrlPacket;
import appeng.hooks.BlockAttackHook;
import appeng.init.client.InitRendering;
import appeng.init.client.InitScreens;
import appeng.init.client.InitStackRenderHandlers;
import appeng.helpers.IMouseWheelItem;
import appeng.integration.modules.patchouli.PatchouliGuide;
import appeng.util.Platform;

/**
 * Client-specific functionality (selected by FML's sided proxy).
 */
@SideOnly(Side.CLIENT)
public class AppEngClient extends AppEngBase {
    private static final Logger LOG = LogManager.getLogger(AppEngClient.class);

    private static AppEngClient INSTANCE;

    /**
     * Last known cable render mode. Used to update all rendered blocks once at the end of the tick when the mode is
     * changed.
     */
    private CableRenderMode prevCableRenderMode = CableRenderMode.STANDARD;

    /**
     * This modifier key has to be held to activate mouse wheel items.
     */
    private static final KeyBinding MOUSE_WHEEL_ITEM_MODIFIER = new KeyBinding(
            "key.ae2.mouse_wheel_item_modifier", KeyConflictContext.IN_GAME, Keyboard.KEY_LSHIFT,
            "key.ae2.category");

    private static final KeyBinding PART_PLACEMENT_OPPOSITE = new KeyBinding(
            "key.ae2.part_placement_opposite", KeyConflictContext.IN_GAME, Keyboard.KEY_LCONTROL,
            "key.ae2.category");

    private boolean previousCtrlDown;

    public AppEngClient() {
        INSTANCE = this;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        InitRendering.preInit();
        ClientRegistry.registerKeyBinding(MOUSE_WHEEL_ITEM_MODIFIER);
        ClientRegistry.registerKeyBinding(PART_PLACEMENT_OPPOSITE);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        Hotkeys.finalizeRegistration(ClientRegistry::registerKeyBinding);
        InitScreens.init();
        BlockAttackHook.install();
        ClientCommands.register();
        PatchouliGuide.init();
    }

    @Override
    public void openGuideAtAnchor(@Nullable ResourceLocation page) {
        if (PatchouliGuide.isAvailable()) {
            if (page == null) {
                PatchouliGuide.open(null, null);
            } else {
                PatchouliGuide.openTopic(page.toString());
            }
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        var minecraft = Minecraft.getMinecraft();
        try {
            StyleManager.initialize(minecraft.getResourceManager());
            InitStackRenderHandlers.init();
        } catch (Throwable e) {
            LOG.error("AE2 failed postClientSetup", e);
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        PendingCraftingJobs.clearPendingJobs();
        PinnedKeys.clearPinnedKeys();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            updateCableRenderMode();
        } else {
            tickPinnedKeys(Minecraft.getMinecraft());
            Hotkeys.checkHotkeys();
            checkCtrlKey();
        }
    }

    private void tickPinnedKeys(Minecraft minecraft) {
        // Only prune pinned keys when no screen is currently open
        if (minecraft.currentScreen == null) {
            PinnedKeys.prune();
        }
    }

    @Override
    public World getClientLevel() {
        return Minecraft.getMinecraft().world;
    }

    @Override
    public void registerHotkey(String id) {
        Hotkeys.registerHotkey(id);
    }

    public static AppEngClient instance() {
        return Objects.requireNonNull(INSTANCE, "AppEngClient is not initialized");
    }

    @SubscribeEvent
    public void wheelEvent(MouseEvent me) {
        if (me.getDwheel() == 0) {
            return;
        }

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.player;
        if (player != null && MOUSE_WHEEL_ITEM_MODIFIER.isKeyDown()) {
            var mainHand = player.getHeldItem(EnumHand.MAIN_HAND)
                    .getItem() instanceof IMouseWheelItem;
            var offHand = player.getHeldItem(EnumHand.OFF_HAND).getItem() instanceof IMouseWheelItem;

            if (mainHand || offHand) {
                ServerboundPacket message = new MouseWheelPacket(me.getDwheel() > 0);
                PacketDistributor.sendToServer(message);
                me.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void ctrlEvent(InputEvent.KeyInputEvent event) {
        checkCtrlKey();
    }

    private void checkCtrlKey() {
        var player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        var isDown = PART_PLACEMENT_OPPOSITE.isKeyDown();
        if (isDown != previousCtrlDown) {
            previousCtrlDown = isDown;
            var previousIsDown = AEAttachmentTypes.HOLDING_CTRL.get(player);
            if (previousIsDown != isDown) {
                AEAttachmentTypes.HOLDING_CTRL.set(player, isDown);
                PacketDistributor.sendToServer(new UpdateHoldingCtrlPacket(isDown));
            }
        }
    }

    public boolean shouldAddParticles(Random r) {
        return switch (Minecraft.getMinecraft().gameSettings.particleSetting) {
            case 0 -> true;
            case 1 -> r.nextBoolean();
            default -> false;
        };
    }

    @Override
    public RayTraceResult getCurrentMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver;
    }

    @Override
    public void spawnEffect(EffectType effect, World level, double posX, double posY,
            double posZ, Object o) {
        if (AEConfig.instance().isEnableEffects()) {
            switch (effect) {
                case Vibrant:
                    this.spawnVibrant(level, posX, posY, posZ);
                    return;
                case Energy:
                    this.spawnEnergy(level, posX, posY, posZ);
                    return;
                case Lightning:
                    this.spawnLightning(level, posX, posY, posZ);
                    return;
                default:
            }
        }
    }

    private void spawnVibrant(World level, double x, double y, double z) {
        if (shouldAddParticles(level.rand)) {
            final double d0 = (level.rand.nextFloat() - 0.5F) * 0.26D;
            final double d1 = (level.rand.nextFloat() - 0.5F) * 0.26D;
            final double d2 = (level.rand.nextFloat() - 0.5F) * 0.26D;

            AEParticles.add(ParticleTypes.VIBRANT, x + d0, y + d1, z + d2, 0.0D, 0.0D, 0.0D);
        }
    }

    private void spawnEnergy(World level, double posX, double posY, double posZ) {
        var random = level.rand;
        final float x = (float) (Math.abs(random.nextInt()) % 100 * 0.01 - 0.5) * 0.7f;
        final float y = (float) (Math.abs(random.nextInt()) % 100 * 0.01 - 0.5) * 0.7f;
        final float z = (float) (Math.abs(random.nextInt()) % 100 * 0.01 - 0.5) * 0.7f;

        AEParticles.add(EnergyParticleData.FOR_BLOCK, posX + x, posY + y, posZ + z, -x * 0.1, -y * 0.1, -z * 0.1);
    }

    private void spawnLightning(World level, double posX, double posY, double posZ) {
        AEParticles.add(ParticleTypes.LIGHTNING, posX, posY + 0.3f, posZ, 0.0f, 0.0f, 0.0f);
    }

    private void updateCableRenderMode() {
        var currentMode = getCableRenderMode();

        // Handle changes to the cable-rendering mode
        if (currentMode == this.prevCableRenderMode) {
            return;
        }

        this.prevCableRenderMode = currentMode;

        var mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }

        // Re-render every cable bus in the loaded world so the new facade render mode is used
        for (var te : mc.world.loadedTileEntityList) {
            if (te instanceof CableBusBlockEntity) {
                mc.world.markBlockRangeForRenderUpdate(te.getPos(), te.getPos());
            }
        }
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        if (Platform.isServer()) {
            return super.getCableRenderMode();
        }

        var mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return CableRenderMode.STANDARD;
        }

        return this.getCableRenderModeForPlayer(mc.player);
    }
}
