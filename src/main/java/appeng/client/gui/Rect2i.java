/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.gui;

/**
 * Backport of Minecraft's Rect2i.
 */
public class Rect2i {
    private int xPos;
    private int yPos;
    private int width;
    private int height;

    public Rect2i(int x, int y, int width, int height) {
        this.xPos = x;
        this.yPos = y;
        this.width = width;
        this.height = height;
    }

    public Rect2i intersect(Rect2i other) {
        int x0 = this.xPos;
        int y0 = this.yPos;
        int x1 = this.xPos + this.width;
        int y1 = this.yPos + this.height;
        int x2 = other.getX();
        int y2 = other.getY();
        int x3 = x2 + other.getWidth();
        int y3 = y2 + other.getHeight();
        this.xPos = Math.max(x0, x2);
        this.yPos = Math.max(y0, y2);
        this.width = Math.max(0, Math.min(x1, x3) - this.xPos);
        this.height = Math.max(0, Math.min(y1, y3) - this.yPos);
        return this;
    }

    public int getX() {
        return this.xPos;
    }

    public int getY() {
        return this.yPos;
    }

    public void setX(int x) {
        this.xPos = x;
    }

    public void setY(int y) {
        this.yPos = y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.xPos = x;
        this.yPos = y;
    }

    public boolean contains(int x, int y) {
        return x >= this.xPos && x <= this.xPos + this.width && y >= this.yPos && y <= this.yPos + this.height;
    }

    @Override
    public String toString() {
        return "Rect2i[" + xPos + "," + yPos + " " + width + "x" + height + "]";
    }
}
