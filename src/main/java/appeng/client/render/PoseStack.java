/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render;

import org.lwjgl.util.vector.Quaternion;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;

/**
 * Stand-in for the 1.21 PoseStack. Minecraft 1.12.2 renders in immediate mode, so every operation is applied to the
 * current OpenGL model-view matrix right away (through {@link GlStateManager}). There is no CPU-side matrix: vertices
 * are emitted untransformed and OpenGL applies the matrix.
 * <p/>
 * {@code mulPose(Axis.XP.rotationDegrees(a))} becomes {@link #rotateX(float)}, and so on.
 */
@SideOnly(Side.CLIENT)
public final class PoseStack {
    private int depth;

    public void pushPose() {
        GlStateManager.pushMatrix();
        depth++;
    }

    public void popPose() {
        if (depth <= 0) {
            throw new IllegalStateException("popPose without matching pushPose");
        }
        depth--;
        GlStateManager.popMatrix();
    }

    /**
     * @return How many poses are currently pushed by this stack.
     */
    public int depth() {
        return depth;
    }

    public void translate(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        GlStateManager.scale(x, y, z);
    }

    public void mulPose(Quaternion quaternion) {
        GlStateManager.rotate(quaternion);
    }

    /**
     * Same as {@code mulPose(orientation.getQuaternion())} in 1.21: rotates from the default orientation (front north,
     * top up) to the given one. The 1.21 quaternion is {@code rotateYXZ(-angleY, -angleX, -angleZ)}.
     */
    public void mulPose(BlockOrientation orientation) {
        if (orientation.isRedundant()) {
            return;
        }
        rotateY(-orientation.getAngleY());
        rotateX(-orientation.getAngleX());
        rotateZ(-orientation.getAngleZ());
    }

    /**
     * Rotates around an arbitrary axis, angle in degrees (counter-clockwise when looking along the axis towards the
     * origin, same as 1.21 {@code Axis.of(axis).rotationDegrees(angle)}).
     */
    public void rotate(float degrees, float x, float y, float z) {
        GlStateManager.rotate(degrees, x, y, z);
    }

    /**
     * Same as {@code mulPose(Axis.XP.rotationDegrees(degrees))}. Use a negative angle for {@code Axis.XN}.
     */
    public void rotateX(float degrees) {
        GlStateManager.rotate(degrees, 1, 0, 0);
    }

    /**
     * Same as {@code mulPose(Axis.YP.rotationDegrees(degrees))}. Use a negative angle for {@code Axis.YN}.
     */
    public void rotateY(float degrees) {
        GlStateManager.rotate(degrees, 0, 1, 0);
    }

    /**
     * Same as {@code mulPose(Axis.ZP.rotationDegrees(degrees))}. Use a negative angle for {@code Axis.ZN}.
     */
    public void rotateZ(float degrees) {
        GlStateManager.rotate(degrees, 0, 0, 1);
    }
}
