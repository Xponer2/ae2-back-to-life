/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.tesr;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;

/**
 * Adapter from the 1.12.2 {@link TileEntitySpecialRenderer} to the shape of the 1.21 BlockEntityRenderer: the pose
 * stack starts at the block origin and the packed light of the block is passed in.
 * <p/>
 * When this is called, the block atlas is bound and the lightmap is set to the light at the block's position (like in
 * vanilla TESRs).
 */
@SideOnly(Side.CLIENT)
public abstract class AEBlockEntityRenderer<T extends TileEntity> extends TileEntitySpecialRenderer<T> {

    @Override
    public final void render(T blockEntity, double x, double y, double z, float partialTicks, int destroyStage,
            float alpha) {
        var level = blockEntity.getWorld();
        int combinedLight = level != null ? level.getCombinedLight(blockEntity.getPos(), 0)
                : BlockEntityRenderHelper.FULL_BRIGHT;

        var poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        try {
            render(blockEntity, partialTicks, poseStack, combinedLight);
        } finally {
            while (poseStack.depth() > 0) {
                poseStack.popPose();
            }
        }
    }

    /**
     * Same as the 1.21 BlockEntityRenderer.render without the buffer source and the overlay.
     */
    public abstract void render(T blockEntity, float partialTicks, PoseStack poseStack, int combinedLight);
}
