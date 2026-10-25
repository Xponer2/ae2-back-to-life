/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.gui.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.GuiGraphics;
import appeng.client.render.TooltipImages;

/**
 * A container screen with the API of newer Minecraft versions (menu, leftPos/topPos, imageWidth/imageHeight,
 * renderables and children, init/render/renderBg/renderLabels, keyPressed/charTyped, mouseScrolled, ...) on top of
 * the 1.12.2 {@link GuiContainer}.
 * <p/>
 * Rendering is done here (slots, slot highlight, labels, carried item), so subclasses can replace each step. Clicks
 * on slots use the logic of {@link GuiContainer} (drag splitting, double click, throwing), which reports the result
 * through {@link #slotClicked}. The layout fields of {@link GuiContainer} (guiLeft, xSize, ...) are kept in sync with
 * the fields of this class before every call into it.
 * <p/>
 * Not supported: the dragged item animation of the touchscreen mode (clicks work, the item is not drawn under the
 * mouse).
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractContainerScreen<T extends Container> extends GuiContainer {

    private static final int LABEL_COLOR = 0x404040;
    private static final int SLOT_HIGHLIGHT_COLOR = 0x80FFFFFF;

    protected final T menu;
    protected final ITextComponent title;
    protected final ITextComponent playerInventoryTitle;
    protected Minecraft minecraft;
    protected FontRenderer font;
    protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected int leftPos;
    protected int topPos;
    protected int titleLabelX = 8;
    protected int titleLabelY = 6;
    protected int inventoryLabelX = 8;
    protected int inventoryLabelY;
    @Nullable
    protected Slot hoveredSlot;

    protected final List<Renderable> renderables = new ArrayList<>();
    private final List<GuiEventListener> children = new ArrayList<>();
    @Nullable
    private GuiEventListener focused;
    private boolean dragging;

    // State of the mouse button that is currently held
    private int heldButton = -1;
    private boolean clickHandledByWidget;
    private double lastDragX;
    private double lastDragY;
    private long heldTime;
    private int lastMoveX = Integer.MIN_VALUE;
    private int lastMoveY = Integer.MIN_VALUE;
    // How the carried stack is split over the slots while dragging: 0 = evenly, 1 = one each, 2 = full stacks
    private int splitMode;

    public AbstractContainerScreen(T menu, InventoryPlayer playerInventory, ITextComponent title) {
        super(menu);
        this.menu = menu;
        this.title = title;
        this.playerInventoryTitle = playerInventory.getDisplayName();
        this.minecraft = Minecraft.getMinecraft();
        this.font = this.minecraft.fontRenderer;
        // The game only sets these when the screen is opened, widgets created in constructors need them earlier
        this.mc = this.minecraft;
        this.fontRenderer = this.font;
        this.itemRender = this.minecraft.getRenderItem();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public T getMenu() {
        return this.menu;
    }

    public ITextComponent getTitle() {
        return this.title;
    }

    // ------------------------------------------------------------------------------------------ widgets

    protected <W extends GuiEventListener & Renderable> W addRenderableWidget(W widget) {
        this.renderables.add(widget);
        this.children.add(widget);
        return widget;
    }

    protected <R extends Renderable> R addRenderableOnly(R renderable) {
        this.renderables.add(renderable);
        return renderable;
    }

    protected <L extends GuiEventListener> L addWidget(L listener) {
        this.children.add(listener);
        return listener;
    }

    protected void removeWidget(Object widget) {
        if (widget instanceof Renderable renderable) {
            this.renderables.remove(renderable);
        }
        if (widget instanceof GuiEventListener listener) {
            this.children.remove(listener);
            if (this.focused == listener) {
                setFocused(null);
            }
        }
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children.clear();
        this.focused = null;
    }

    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        for (var child : this.children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
    }

    @Nullable
    public GuiEventListener getFocused() {
        return this.focused;
    }

    public void setFocused(@Nullable GuiEventListener listener) {
        if (this.focused == listener) {
            return;
        }
        if (this.focused != null) {
            this.focused.setFocused(false);
        }
        this.focused = listener;
        if (listener != null) {
            listener.setFocused(true);
        }
    }

    public void setInitialFocus(GuiEventListener listener) {
        setFocused(listener);
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    // ------------------------------------------------------------------------------------------ lifecycle

    @Override
    public final void initGui() {
        this.minecraft = this.mc;
        this.font = this.fontRenderer;
        clearWidgets();
        super.initGui();
        init();
        syncLayout();
    }

    /**
     * Lays out the screen and adds the widgets. Called when the screen is opened and when the window is resized.
     */
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public final void updateScreen() {
        super.updateScreen();
        if (this.mc.currentScreen == this) {
            syncLayout();
            containerTick();
        }
    }

    protected void containerTick() {
    }

    @Override
    public final void onGuiClosed() {
        removed();
    }

    /**
     * Called when the screen is closed or replaced.
     */
    public void removed() {
        super.onGuiClosed();
    }

    /**
     * Closes the screen and the menu on both sides.
     */
    public void onClose() {
        this.mc.player.closeScreen();
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public final boolean doesGuiPauseGame() {
        return isPauseScreen();
    }

    private void syncLayout() {
        this.xSize = this.imageWidth;
        this.ySize = this.imageHeight;
        this.guiLeft = this.leftPos;
        this.guiTop = this.topPos;
    }

    // ------------------------------------------------------------------------------------------ rendering

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        syncLayout();
        render(new GuiGraphics(this.mc), mouseX, mouseY, partialTicks);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        // Buttons and labels that other mods add through the screen events use the lists of the game
        for (GuiButton button : this.buttonList) {
            button.drawButton(this.mc, mouseX, mouseY, partialTick);
        }
        for (GuiLabel label : this.labelList) {
            label.drawLabel(this.mc, mouseX, mouseY);
        }
        for (var renderable : new ArrayList<>(this.renderables)) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(this.leftPos, this.topPos, 0);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        this.hoveredSlot = null;
        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (!slot.isEnabled()) {
                continue;
            }
            renderSlot(guiGraphics, slot);
            if (isHovering(slot, mouseX, mouseY)) {
                this.hoveredSlot = slot;
                renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
            }
        }

        RenderHelper.disableStandardItemLighting();
        renderLabels(guiGraphics, mouseX, mouseY);
        RenderHelper.enableGUIStandardItemLighting();
        MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawForeground(this, mouseX, mouseY));

        renderCarriedItem(guiGraphics, mouseX, mouseY);
        pose.popPose();

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
    }

    /**
     * Darkens the world behind the screen and draws the screen background.
     */
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    protected abstract void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);

    @Override
    protected final void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // Not called, render() draws the background with renderBg
    }

    @Override
    protected final void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Not called, render() draws the foreground with renderLabels
    }

    /**
     * Draws the foreground (relative to leftPos/topPos), by default the title and the name of the player inventory.
     */
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                LABEL_COLOR, false);
    }

    /**
     * Draws the item of a slot (relative to leftPos/topPos), or its background icon when it is empty. While the
     * carried stack is split over several slots, the slots show what they would receive.
     */
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        int x = slot.xPos;
        int y = slot.yPos;
        ItemStack shown = slot.getStack();
        String countText = null;
        boolean splitPreview = false;

        ItemStack carried = this.mc.player.inventory.getItemStack();
        if (this.dragSplitting && this.dragSplittingSlots.contains(slot) && !carried.isEmpty()) {
            if (this.dragSplittingSlots.size() == 1) {
                // A single slot is a normal click, it is shown when the button is released
                return;
            }
            if (Container.canAddItemToSlot(slot, carried, true) && this.inventorySlots.canDragIntoSlot(slot)) {
                int alreadyThere = shown.isEmpty() ? 0 : shown.getCount();
                shown = carried.copy();
                Container.computeStackSize(this.dragSplittingSlots, this.splitMode, shown, alreadyThere);
                int limit = Math.min(shown.getMaxStackSize(), slot.getItemStackLimit(shown));
                if (shown.getCount() > limit) {
                    countText = TextFormatting.YELLOW.toString() + limit;
                    shown.setCount(limit);
                }
                splitPreview = true;
            } else {
                this.dragSplittingSlots.remove(slot);
            }
        }

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 100);
        TextureAtlasSprite backgroundIcon = shown.isEmpty() ? slot.getBackgroundSprite() : null;
        if (backgroundIcon != null) {
            GlStateManager.disableLighting();
            this.mc.getTextureManager().bindTexture(slot.getBackgroundLocation());
            drawTexturedModalRect(x, y, backgroundIcon, 16, 16);
            GlStateManager.enableLighting();
        } else {
            if (splitPreview) {
                guiGraphics.fill(x, y, x + 16, y + 16, SLOT_HIGHLIGHT_COLOR);
            }
            GlStateManager.enableDepth();
            guiGraphics.renderItem(this.mc.player, this.mc.world, shown, x, y, x + y * this.imageWidth);
            guiGraphics.renderItemDecorations(this.font, shown, x, y, countText);
        }
        pose.popPose();
    }

    /**
     * Draws the highlight of the slot under the mouse (relative to leftPos/topPos).
     */
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY,
            float partialTick) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.colorMask(true, true, true, false);
        guiGraphics.fillGradient(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, SLOT_HIGHLIGHT_COLOR,
                SLOT_HIGHLIGHT_COLOR);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
    }

    private void renderCarriedItem(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack carried = this.mc.player.inventory.getItemStack();
        if (carried.isEmpty()) {
            return;
        }
        String countText = null;
        if (this.dragSplitting && this.dragSplittingSlots.size() > 1) {
            carried = carried.copy();
            carried.setCount(getSplitRemainder(carried));
            if (carried.isEmpty()) {
                countText = TextFormatting.YELLOW + "0";
            }
        }
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 232);
        int x = mouseX - this.leftPos - 8;
        int y = mouseY - this.topPos - 8;
        guiGraphics.renderItem(carried, x, y);
        guiGraphics.renderItemDecorations(this.font, carried, x, y, countText);
        pose.popPose();
    }

    /**
     * @return How much of the carried stack is left when it is split over the dragged slots.
     */
    private int getSplitRemainder(ItemStack carried) {
        if (this.splitMode == 2) {
            return carried.getMaxStackSize();
        }
        int remainder = carried.getCount();
        for (Slot slot : this.dragSplittingSlots) {
            ItemStack existing = slot.getStack();
            int alreadyThere = existing.isEmpty() ? 0 : existing.getCount();
            ItemStack placed = carried.copy();
            Container.computeStackSize(this.dragSplittingSlots, this.splitMode, placed, alreadyThere);
            int limit = Math.min(placed.getMaxStackSize(), slot.getItemStackLimit(placed));
            remainder -= Math.min(placed.getCount(), limit) - alreadyThere;
        }
        return remainder;
    }

    /**
     * Draws the tooltip of the item under the mouse, unless an item is carried.
     */
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.hoveredSlot == null || !this.hoveredSlot.getHasStack()
                || !this.mc.player.inventory.getItemStack().isEmpty()) {
            return;
        }
        ItemStack stack = this.hoveredSlot.getStack();
        guiGraphics.renderTooltip(this.font, getTooltipFromContainerItem(stack), TooltipImages.getTooltipImage(stack),
                stack, mouseX, mouseY);
    }

    protected List<ITextComponent> getTooltipFromContainerItem(ItemStack stack) {
        List<String> lines = getItemToolTip(stack);
        List<ITextComponent> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(new TextComponentString(line));
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------ slots

    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return isHovering(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY);
    }

    /**
     * @return True if the mouse is inside the given area (relative to leftPos/topPos), with a 1 pixel margin.
     */
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        double relativeX = mouseX - this.leftPos;
        double relativeY = mouseY - this.topPos;
        return relativeX >= x - 1 && relativeX < x + width + 1 && relativeY >= y - 1 && relativeY < y + height + 1;
    }

    @Nullable
    protected Slot findSlot(double mouseX, double mouseY) {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (slot.isEnabled() && isHovering(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Sends a click on a slot (or outside of the screen with slot id -999) to the server.
     */
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType clickType) {
        super.handleMouseClick(slot, slotId, mouseButton, clickType);
    }

    @Override
    protected final void handleMouseClick(@Nullable Slot slot, int slotId, int mouseButton, ClickType clickType) {
        slotClicked(slot, slotId, mouseButton, clickType);
    }

    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return mouseX < guiLeft || mouseY < guiTop || mouseX >= guiLeft + this.imageWidth
                || mouseY >= guiTop + this.imageHeight;
    }

    @Override
    protected final boolean hasClickedOutside(int mouseX, int mouseY, int guiLeft, int guiTop) {
        return hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, this.heldButton);
    }

    /**
     * Swaps the hovered slot with a hotbar slot when a hotbar key is pressed.
     */
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (!this.mc.player.inventory.getItemStack().isEmpty() || this.hoveredSlot == null) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (Keys.matches(this.mc.gameSettings.keyBindsHotbar[i], keyCode)) {
                slotClicked(this.hoveredSlot, this.hoveredSlot.slotNumber, i, ClickType.SWAP);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ mouse

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        super.handleMouseInput();
        if (this.mc.currentScreen != this) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            syncLayout();
            mouseScrolled(mouseX, mouseY, 0, Integer.signum(wheel));
        } else if (mouseX != this.lastMoveX || mouseY != this.lastMoveY) {
            this.lastMoveX = mouseX;
            this.lastMoveY = mouseY;
            mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    protected final void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        syncLayout();
        this.heldButton = mouseButton;
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
        this.heldTime = Minecraft.getSystemTime();
        mouseClicked((double) mouseX, (double) mouseY, mouseButton);
    }

    @Override
    protected final void mouseReleased(int mouseX, int mouseY, int state) {
        syncLayout();
        this.heldButton = state;
        mouseReleased((double) mouseX, (double) mouseY, state);
        this.heldButton = -1;
    }

    @Override
    protected final void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        syncLayout();
        this.heldButton = clickedMouseButton;
        double dragX = mouseX - this.lastDragX;
        double dragY = mouseY - this.lastDragY;
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
        mouseDragged(mouseX, mouseY, clickedMouseButton, dragX, dragY);
    }

    public void mouseMoved(double mouseX, double mouseY) {
    }

    /**
     * Gives the click to the first widget that accepts it (and focuses it), otherwise to the slots.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (var child : new ArrayList<>(this.children)) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                setFocused(child);
                if (button == Keys.MOUSE_BUTTON_LEFT) {
                    setDragging(true);
                }
                this.clickHandledByWidget = true;
                return true;
            }
        }
        this.clickHandledByWidget = false;

        boolean wasSplitting = this.dragSplitting;
        try {
            super.mouseClicked((int) mouseX, (int) mouseY, button);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!wasSplitting && this.dragSplitting) {
            this.splitMode = button == Keys.MOUSE_BUTTON_LEFT ? 0 : button == Keys.MOUSE_BUTTON_RIGHT ? 1 : 2;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == Keys.MOUSE_BUTTON_LEFT && this.dragging) {
            setDragging(false);
            if (this.focused != null) {
                this.focused.mouseReleased(mouseX, mouseY, button);
            }
        } else {
            getChildAt(mouseX, mouseY).ifPresent(child -> child.mouseReleased(mouseX, mouseY, button));
        }
        if (this.clickHandledByWidget) {
            // The press went to a widget, the slots never saw it
            this.clickHandledByWidget = false;
            return true;
        }
        super.mouseReleased((int) mouseX, (int) mouseY, button);
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.focused != null && this.dragging && button == Keys.MOUSE_BUTTON_LEFT) {
            return this.focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (!this.clickHandledByWidget) {
            super.mouseClickMove((int) mouseX, (int) mouseY, button, Minecraft.getSystemTime() - this.heldTime);
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var child = getChildAt(mouseX, mouseY);
        return child.isPresent() && child.get().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------------------------------ keyboard

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        int key = Keyboard.getEventKey();
        if (!Keyboard.getEventKeyState() && key != Keyboard.KEY_NONE && this.mc.currentScreen == this) {
            keyReleased(key, 0, Keys.currentModifiers());
        }
    }

    /**
     * A key press of the game is split into a key press and the typed character, like in newer versions.
     */
    @Override
    protected final void keyTyped(char typedChar, int keyCode) {
        syncLayout();
        int modifiers = Keys.currentModifiers();
        if (keyCode != Keyboard.KEY_NONE) {
            keyPressed(keyCode, 0, modifiers);
        }
        if (this.mc.currentScreen == this && ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            charTyped(typedChar, modifiers);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == Keyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        if (this.focused != null && this.focused.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        var options = this.mc.gameSettings;
        if (Keys.matches(options.keyBindInventory, keyCode)) {
            onClose();
            return true;
        }

        boolean handled = checkHotbarKeyPressed(keyCode, scanCode);
        if (this.hoveredSlot != null && this.hoveredSlot.getHasStack()) {
            if (Keys.matches(options.keyBindPickBlock, keyCode)) {
                slotClicked(this.hoveredSlot, this.hoveredSlot.slotNumber, 0, ClickType.CLONE);
                handled = true;
            } else if (Keys.matches(options.keyBindDrop, keyCode)) {
                slotClicked(this.hoveredSlot, this.hoveredSlot.slotNumber, hasControlDown() ? 1 : 0,
                        ClickType.THROW);
                handled = true;
            }
        }
        return handled;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.focused != null && this.focused.keyReleased(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return this.focused != null && this.focused.charTyped(codePoint, modifiers);
    }

    public static boolean hasShiftDown() {
        return isShiftKeyDown();
    }

    public static boolean hasControlDown() {
        return isCtrlKeyDown();
    }

    public static boolean hasAltDown() {
        return isAltKeyDown();
    }
}
