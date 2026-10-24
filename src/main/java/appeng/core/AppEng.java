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

package appeng.core;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import appeng.api.ids.AEConstants;
import appeng.api.parts.CableRenderMode;
import appeng.client.EffectType;
import appeng.core.network.ClientboundPacket;

public interface AppEng {

    String MOD_NAME = "AE2: Back to Life";
    String MOD_ID = AEConstants.MOD_ID;

    static AppEng instance() {
        return AppEngBase.INSTANCE;
    }

    static ResourceLocation makeId(String id) {
        return new ResourceLocation(MOD_ID, id);
    }

    /**
     * Allows common item use methods to get the current mouse over without relying on client-only methods.
     */
    default RayTraceResult getCurrentMouseOver() {
        return null;
    }

    /**
     * @return A stream of all players in the game. On the client it'll be empty if no level is loaded.
     */
    Collection<EntityPlayerMP> getPlayers();

    void sendToAllNearExcept(EntityPlayer p, double x, double y, double z, double dist, World level,
            ClientboundPacket packet);

    void spawnEffect(EffectType effect, World level, double posX, double posY,
            double posZ, Object o);

    /**
     * Sets the player that is currently interacting with a cable or part attached to a cable. This will return that
     * player's cable render mode from calls to {@link #getCableRenderMode()}, until another player or null is set.
     *
     * @param player Null to revert to the default cable render mode.
     */
    void setPartInteractionPlayer(EntityPlayer player);

    CableRenderMode getCableRenderMode();

    /**
     * Can be used to get the current level the client is in.
     *
     * @return null if no client level is available (i.e. on a dedicated server)
     */
    @Nullable
    World getClientLevel();

    /**
     * Since in a Minecraft client, multiple servers can be launched and stopped during a single session, the result of
     * this method should not be stored globally.
     *
     * @return The currently running Minecraft server instance, if there is one.
     */
    @Nullable
    MinecraftServer getCurrentServer();

    /**
     * registers Hotkeys for {@link appeng.hotkeys.HotkeyActions}
     */
    void registerHotkey(String id);

    /**
     * Opens the guidebook (if this is a client) on the last opened page, or the given initial page, if no page has ever
     * been opened yet.
     */
    default void openGuideAtAnchor(@Nullable ResourceLocation page) {
    }
}
