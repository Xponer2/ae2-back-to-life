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

package appeng.client.render.overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.util.DimensionalBlockPos;
import appeng.client.render.PoseStack;

/**
 * This is based on the area render of https://github.com/TeamPneumatic/pnc-repressurized/
 * <p/>
 * Not client-only: block entities show and hide their overlays through it (only ever on the client side). Registered
 * on the event bus by {@code InitRendering}.
 */
public class OverlayManager {

    private final static OverlayManager INSTANCE = new OverlayManager();

    private final Map<DimensionalBlockPos, OverlayRenderer> overlayHandlers = new HashMap<>();

    public static OverlayManager getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderWorldLastEvent(RenderWorldLastEvent event) {
        if (overlayHandlers.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        var renderManager = minecraft.getRenderManager();

        // The model-view matrix already contains the camera rotation, move from the camera to the world origin
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);

        var handlers = new ArrayList<OverlayRenderer>();
        for (var entry : overlayHandlers.entrySet()) {
            if (entry.getKey().getLevel() == minecraft.world) {
                handlers.add(entry.getValue());
            }
        }

        // Same order as the 1.21 render type batches
        OverlayRenderType.beginBlockHilightLineOccluded();
        for (var handler : handlers) {
            handler.renderLinesOccluded();
        }
        OverlayRenderType.beginBlockHilightFace();
        for (var handler : handlers) {
            handler.renderFaces();
        }
        OverlayRenderType.beginBlockHilightLine();
        for (var handler : handlers) {
            handler.renderLines();
        }
        OverlayRenderType.end();

        poseStack.popPose();
    }

    public OverlayRenderer showArea(IOverlayDataSource source) {
        Objects.requireNonNull(source);

        OverlayRenderer handler = new OverlayRenderer(source);
        overlayHandlers.put(source.getOverlaySourceLocation(), handler);
        return handler;
    }

    public boolean isShowing(IOverlayDataSource source) {
        return overlayHandlers.containsKey(source.getOverlaySourceLocation());
    }

    public void removeHandlers(IOverlayDataSource source) {
        overlayHandlers.remove(source.getOverlaySourceLocation());
    }

}
