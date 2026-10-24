/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.AELog;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.PacketDistributor;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.clientbound.GuiDataSyncPacket;
import appeng.core.network.serverbound.GuiActionPacket;
import appeng.helpers.InventoryAction;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.me.helpers.PlayerSource;
import appeng.menu.guisync.DataSynchronization;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.CraftingMatrixSlot;
import appeng.menu.slot.CraftingTermSlot;
import appeng.menu.slot.DisabledSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.AEBasePart;
import appeng.util.ConfigMenuInventory;
import appeng.util.Platform;

/**
 * Base of all AE2 menus. Extends the 1.12.2 {@link Container} but keeps the API of the 1.21 menus
 * ({@code broadcastChanges}, {@code quickMoveStack}, {@code stillValid}, {@code clicked}, {@code removed},
 * {@code slotsChanged}, {@code getCarried}/{@code setCarried}, {@code slots}, {@code containerId}): the vanilla 1.12.2
 * methods are overridden here and forward to them, so the subclasses override the 1.21 names.
 */
public abstract class AEBaseMenu extends Container {
    private static final int MAX_STRING_LENGTH = 32767;
    private static final int MAX_CONTAINER_TRANSFER_ITERATIONS = 256;
    private static final String HIDE_SLOT = "HideSlot";
    /**
     * Index of the off-hand slot in the player inventory (36 main + 4 armor).
     */
    private static final int SLOT_OFFHAND = 40;

    /**
     * 1.21 name of {@link #inventorySlots} (same list).
     */
    protected final List<Slot> slots;
    /**
     * 1.21 name of {@link #windowId}, which is never changed after construction.
     */
    public final int containerId;
    private final MenuType<?> menuType;
    /**
     * Result of the vanilla slot click, see {@link #slotClick}.
     */
    private ItemStack clickResult = ItemStack.EMPTY;

    private final IActionSource mySrc;
    @Nullable
    private final TileEntity blockEntity;
    @Nullable
    private final IPart part;
    @Nullable
    protected final ItemMenuHost<?> itemMenuHost;
    private final DataSynchronization dataSync = new DataSynchronization(this);
    private final InventoryPlayer playerInventory;
    private final Set<Integer> lockedPlayerInventorySlots = new HashSet<>();
    private final Map<Slot, SlotSemantic> semanticBySlot = new HashMap<>();
    private final ArrayListMultimap<SlotSemantic, Slot> slotsBySemantic = ArrayListMultimap.create();
    private final Map<String, ClientAction<?>> clientActions = new HashMap<>();
    private boolean menuValid = true;
    private MenuHostLocator locator;
    // Slots that are only present on the client-side
    private final Set<Slot> clientSideSlot = new HashSet<>();
    /**
     * Indicates that the menu was created after returning from a {@link ISubMenu}. Previous screen state amount on the
     * client should be restored.
     */
    private boolean returnedFromSubScreen;

    public AEBaseMenu(MenuType<?> menuType, int id, InventoryPlayer playerInventory,
            Object host) {
        this.menuType = menuType;
        this.windowId = id;
        this.containerId = id;
        this.slots = this.inventorySlots;
        this.playerInventory = playerInventory;
        this.blockEntity = host instanceof TileEntity ? (TileEntity) host : null;
        this.part = host instanceof IPart ? (IPart) host : null;
        this.itemMenuHost = host instanceof ItemMenuHost<?> ? (ItemMenuHost<?>) host : null;

        if (host != null && this.blockEntity == null && this.part == null && this.itemMenuHost == null) {
            throw new IllegalArgumentException("Must have a valid host, instead " + host + " in " + playerInventory);
        }

        if (itemMenuHost != null && itemMenuHost.getPlayerInventorySlot() != null) {
            lockPlayerInventorySlot(itemMenuHost.getPlayerInventorySlot());
        }

        this.mySrc = new PlayerSource(getPlayer(), this.getActionHost());
        registerClientAction(HIDE_SLOT, String.class, this::hideSlot);
    }

    protected final IActionHost getActionHost() {
        if (this.itemMenuHost instanceof IActionHost) {
            return (IActionHost) this.itemMenuHost;
        }

        if (this.blockEntity instanceof IActionHost) {
            return (IActionHost) this.blockEntity;
        }

        if (this.part instanceof IActionHost) {
            return (IActionHost) this.part;
        }

        return null;
    }

    protected final boolean isActionHost() {
        return this.itemMenuHost instanceof IActionHost
                || this.blockEntity instanceof IActionHost
                || this.part instanceof IActionHost;
    }

    /**
     * Convenience method to get the player owning this menu.
     */

    public EntityPlayer getPlayer() {
        return getPlayerInventory().player;
    }

    public MenuType<?> getType() {
        return menuType;
    }

    public ItemStack getCarried() {
        return playerInventory.getItemStack();
    }

    public void setCarried(ItemStack stack) {
        playerInventory.setItemStack(stack);
    }

    public IActionSource getActionSource() {
        return this.mySrc;
    }

    public InventoryPlayer getPlayerInventory() {
        return this.playerInventory;
    }

    public void lockPlayerInventorySlot(int invSlot) {
        Preconditions.checkArgument(invSlot >= 0 && invSlot < playerInventory.getSizeInventory(),
                "cannot lock player inventory slot: %s", invSlot);
        this.lockedPlayerInventorySlots.add(invSlot);
    }

    public final boolean isPlayerInventorySlotLocked(int invSlot) {
        return this.lockedPlayerInventorySlots.contains(invSlot);
    }

    public Object getTarget() {
        if (this.blockEntity != null) {
            return this.blockEntity;
        }
        if (this.part != null) {
            return this.part;
        }
        return this.itemMenuHost;
    }

    public TileEntity getTileEntity() {
        return this.blockEntity;
    }

    protected final void createPlayerInventorySlots(InventoryPlayer playerInventory) {
        Preconditions.checkState(
                getSlots(SlotSemantics.PLAYER_INVENTORY).isEmpty(),
                "EntityPlayer inventory was already created");

        for (int i = 0; i < playerInventory.mainInventory.size(); i++) {
            Slot slot;
            if (this.lockedPlayerInventorySlots.contains(i)) {
                slot = new DisabledSlot(playerInventory, i);
            } else {
                slot = new Slot(playerInventory, i, 0, 0);
            }
            var s = i < InventoryPlayer.getHotbarSize()
                    ? SlotSemantics.PLAYER_HOTBAR
                    : SlotSemantics.PLAYER_INVENTORY;
            addSlot(slot, s);
        }
    }

    @Override
    public final ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        clickResult = ItemStack.EMPTY;
        clicked(slotId, dragType, clickType, player);
        return clickResult;
    }

    public void clicked(int slotId, int button, ClickType clickType, EntityPlayer player) {
        // Do not allow swapping with off-hand if the off-hand slot is locked
        if (clickType == ClickType.SWAP && isPlayerInventorySlotLocked(SLOT_OFFHAND)) {
            return;
        }

        clickResult = super.slotClick(slotId, button, clickType, player);
    }

    protected Slot addSlot(Slot slot, SlotSemantic semantic) {
        slot = this.addSlot(slot);

        Preconditions.checkState(!semanticBySlot.containsKey(slot));
        semanticBySlot.put(slot, semantic);
        slotsBySemantic.put(semantic, slot);
        return slot;
    }

    /**
     * Allows screens to add client-side only slots.
     * <p/>
     * For sub-screens that need their slots to be removed when returning to the parent, use the
     * <code>addClientSideSlot</code> method in {@link appeng.client.gui.implementations.AESubScreen} instead.
     */
    public Slot addClientSideSlot(Slot slot, SlotSemantic semantic) {
        Preconditions.checkState(isClientSide(), "Can only add client-side slots on the client");
        if (!clientSideSlot.add(slot)) {
            throw new IllegalStateException("Client-side slot already exists");
        }

        // We're only doing this on the client-side.
        // The synchronization related fields are not used
        slot.slotNumber = slots.size();
        slots.add(slot);

        if (semantic != null) {
            semanticBySlot.put(slot, semantic);
            slotsBySemantic.put(semantic, slot);
        }
        return slot;
    }

    public void removeClientSideSlot(Slot slot) {
        if (slots.get(slot.slotNumber) != slot) {
            throw new IllegalStateException("Trying to remove slot which isn't currently in the menu");
        }
        if (!clientSideSlot.remove(slot)) {
            throw new IllegalStateException("Trying to remove slot which isn't a client-side slot");
        }

        slots.remove(slot.slotNumber);
        semanticBySlot.remove(slot);
        slotsBySemantic.values().remove(slot);

        // Update the slot index for any subsequent slots
        for (int i = slot.slotNumber; i < slots.size(); i++) {
            slots.get(i).slotNumber = i;
        }
    }

    public boolean isClientSideSlot(Slot slot) {
        return clientSideSlot.contains(slot);
    }

    public List<Slot> getSlots(SlotSemantic semantic) {
        return slotsBySemantic.get(semantic);
    }

    protected Slot addSlot(Slot newSlot) {
        if (newSlot instanceof AppEngSlot s) {
            s.setMenu(this);
        }
        return addSlotToContainer(newSlot);
    }

    @Override
    public final void setAll(List<ItemStack> items) {
        initializeContents(0, items, getCarried());
    }

    @Override
    public final void putStackInSlot(int slotID, ItemStack stack) {
        setItem(slotID, 0, stack);
    }

    /**
     * 1.21 name of {@link Container#putStackInSlot} (slot update received by the client).
     */
    public void setItem(int slotID, int stateId, ItemStack stack) {
        super.putStackInSlot(slotID, stack);
    }

    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        for (int i = 0; i < items.size() && i < this.slots.size(); ++i) {
            var slot = this.getSlot(i);
            if (slot instanceof AppEngSlot aeSlot) {
                aeSlot.initialize(items.get(i));
            } else {
                slot.putStack(items.get(i));
            }
        }

        this.setCarried(carried);
    }

    @Override
    public final void detectAndSendChanges() {
        broadcastChanges();
    }

    public void broadcastChanges() {
        if (!isValidMenu()) {
            return;
        }

        if (itemMenuHost != null) {
            if (!itemMenuHost.isValid()) {
                setValidMenu(false);
                return;
            }
            itemMenuHost.tick();
        }

        if (isServerSide()) {
            if (this.blockEntity != null
                    && this.blockEntity.getWorld().getTileEntity(this.blockEntity.getPos()) != this.blockEntity) {
                this.setValidMenu(false);
            }
            if (this.part instanceof AEBasePart basePart) {
                var host = basePart.getHost();
                if (host == null || !host.isInWorld() || host.getPart(basePart.getSide()) != basePart) {
                    setValidMenu(false);
                }
            }

            if (dataSync.hasChanges()) {
                sendPacketToClient(new GuiDataSyncPacket(containerId, dataSync::writeUpdate));
            }
        }

        super.detectAndSendChanges();
    }

    /**
     * Get a slots priority for quick moving items (higher priority slots are tried first)
     */
    protected int getQuickMovePriority(Slot slot) {
        var semantic = getSlotSemantic(slot);
        if (semantic == null) {
            return 0;
        }
        return semantic.quickMovePriority();
    }

    /**
     * Check if a given slot is considered to be "on the player side" for the purposes of shift-clicking items back and
     * forth between the opened menu and the player's inventory.
     */
    public boolean isPlayerSideSlot(Slot slot) {
        if (slot.inventory == playerInventory) {
            return true;
        }

        var slotSemantic = semanticBySlot.get(slot);
        return slotSemantic != null && slotSemantic.playerSide();
    }

    @Nullable
    public SlotSemantic getSlotSemantic(Slot s) {
        return semanticBySlot.get(s);
    }

    public void hideSlot(String semantic) {
        if (isClientSide()) {
            sendClientAction(HIDE_SLOT, semantic);
        }
        var slotSemantic = SlotSemantics.get(semantic);
        if (slotSemantic == null)
            return;
        if (canSlotsBeHidden(slotSemantic)) {
            for (Slot s : getSlots(slotSemantic)) {
                if (s instanceof AppEngSlot slot) {
                    slot.setSlotEnabled(false);
                }
            }
        }
    }

    /**
     * Return true if the client is allowed to hide slots with this semantic, false otherwise. You should return false
     * unless you have checked that hiding the slots doesn't risk crashing the game or causing other severe issues.
     */
    protected boolean canSlotsBeHidden(SlotSemantic semantic) {
        return false;
    }

    @Override
    public final ItemStack transferStackInSlot(EntityPlayer player, int idx) {
        return quickMoveStack(player, idx);
    }

    public ItemStack quickMoveStack(EntityPlayer player, int idx) {
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }

        var clickSlot = this.slots.get(idx);

        // Vanilla will check this before calling this method, but we do use it in other contexts as well (move region)
        if (!clickSlot.canTakeStack(player)) {
            return ItemStack.EMPTY;
        }

        var stackToMove = clickSlot.getStack();
        if (stackToMove.isEmpty()) {
            return ItemStack.EMPTY;
        }

        boolean fromPlayerSide = isPlayerSideSlot(clickSlot);

        // Allow moving items from player-side slots into some "remote" inventory that is not slot-based
        // This is used to move items into the network inventory
        if (fromPlayerSide) {
            // With a Storage Bus on a Just Dire Things player accessor, the stack to move might be modified during the
            // transfer. So keep track of how much was transferred and only subtract it at the end.
            int transferred = transferStackToMenu(stackToMove.copy());
            if (transferred > 0) {
                clickSlot.decrStackSize(transferred);
            }
        }
        stackToMove = clickSlot.getStack();
        if (stackToMove.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var originalStackToMove = stackToMove.copy();

        stackToMove = quickMoveToOtherSlots(stackToMove, isPlayerSideSlot(clickSlot));

        // While we did modify stackToMove in-place, this causes the container to be notified of the change
        if (!ItemStack.areItemStacksEqual(originalStackToMove, stackToMove)) {
            clickSlot.putStack(stackToMove.isEmpty() ? ItemStack.EMPTY : stackToMove);
        }

        return ItemStack.EMPTY;
    }

    private ItemStack quickMoveToOtherSlots(ItemStack stackToMove, boolean fromPlayerSide) {
        var destinationSlots = getQuickMoveDestinationSlots(stackToMove, fromPlayerSide);

        // If no actual targets were available, allow moving into filter slots too
        if (destinationSlots.isEmpty() && fromPlayerSide) {
            for (Slot cs : this.slots) {
                if (cs instanceof FakeSlot && !isPlayerSideSlot(cs)) {
                    var destination = cs.getStack();
                    if (Platform.isSameItemSameComponents(destination, stackToMove)) {
                        break; // Item is already in the filter
                    } else if (destination.isEmpty()) {
                        cs.putStack(stackToMove.copy());
                        // ???
                        this.broadcastChanges();
                        break;
                    }
                }
            }
            return stackToMove; // Since destinationSlots was empty, nothing else to do
        }

        // Try stacking the item into filled slots first
        for (var dest : destinationSlots) {
            if (dest.getHasStack() && (stackToMove = safeInsert(dest, stackToMove)).isEmpty()) {
                return stackToMove;
            }
        }

        // Now try placing it in empty slots, if it's not already fully consumed
        for (var dest : destinationSlots) {
            if (!dest.getHasStack() && (stackToMove = safeInsert(dest, stackToMove)).isEmpty()) {
                return stackToMove;
            }
        }

        return stackToMove;
    }

    /**
     * 1.21 {@code Slot.safeInsert(stack)}: puts as much of the stack into the slot as allowed, returns the rest (the
     * given stack, shrunk).
     */
    public static ItemStack safeInsert(Slot slot, ItemStack stack) {
        if (stack.isEmpty() || !slot.isItemValid(stack)) {
            return stack;
        }
        var current = slot.getStack();
        int amount = Math.min(stack.getCount(), slot.getItemStackLimit(stack) - current.getCount());
        if (amount <= 0) {
            return stack;
        }
        if (current.isEmpty()) {
            slot.putStack(stack.splitStack(amount));
        } else if (Platform.isSameItemSameComponents(current, stack)) {
            stack.shrink(amount);
            current.grow(amount);
            slot.putStack(current);
        }
        return stack;
    }

    protected List<Slot> getQuickMoveDestinationSlots(ItemStack stackToMove, boolean fromPlayerSide) {
        // Find potential destination slots
        var destinationSlots = new ArrayList<Slot>();
        for (var candidateSlot : this.slots) {
            if (isValidQuickMoveDestination(candidateSlot, stackToMove, fromPlayerSide)) {
                destinationSlots.add(candidateSlot);
            }
        }

        // Order slots by the priority of their semantic
        destinationSlots.sort(Comparator.comparingInt(this::getQuickMovePriority).reversed());
        return destinationSlots;
    }

    /**
     * Check if a given candidate slot is a valid destination for {@link #quickMoveStack}.
     */
    protected boolean isValidQuickMoveDestination(Slot candidateSlot, ItemStack stackToMove,
            boolean fromPlayerSide) {
        return isPlayerSideSlot(candidateSlot) != fromPlayerSide
                && !(candidateSlot instanceof FakeSlot)
                && !(candidateSlot instanceof CraftingMatrixSlot)
                && candidateSlot.isItemValid(stackToMove);
    }

    protected int getPlaceableAmount(Slot s, AEItemKey what) {
        if (!s.isItemValid(what.toStack())) {
            return 0;
        }

        var currentItem = s.getStack();
        if (currentItem.isEmpty()) {
            return s.getItemStackLimit(what.getReadOnlyStack());
        } else if (what.matches(currentItem)) {
            return Math.max(0, s.getItemStackLimit(currentItem) - currentItem.getCount());
        } else {
            return 0;
        }
    }

    @Override
    public final boolean canInteractWith(EntityPlayer player) {
        return stillValid(player);
    }

    public boolean stillValid(EntityPlayer player) {
        if (this.isValidMenu()) {
            if (this.blockEntity instanceof IInventory) {
                return ((IInventory) this.blockEntity).isUsableByPlayer(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public final boolean canDragIntoSlot(Slot s) {
        return canDragTo(s);
    }

    public boolean canDragTo(Slot s) {
        if (s instanceof AppEngSlot) {
            return ((AppEngSlot) s).isDraggable();
        } else {
            return super.canDragIntoSlot(s);
        }
    }

    @Override
    public final void onCraftMatrixChanged(IInventory inventory) {
        slotsChanged(inventory);
    }

    /**
     * 1.21 name of {@link Container#onCraftMatrixChanged}.
     */
    public void slotsChanged(IInventory inventory) {
        super.onCraftMatrixChanged(inventory);
    }

    @Override
    public final void onContainerClosed(EntityPlayer player) {
        removed(player);
    }

    /**
     * 1.21 name of {@link Container#onContainerClosed}.
     */
    public void removed(EntityPlayer player) {
        super.onContainerClosed(player);
    }

    /**
     * Sets a filter slot based on a <b>non-existent</b> item sent by the client.
     */
    public void setFilter(int slotIndex, ItemStack item) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return;
        }
        final Slot s = this.getSlot(slotIndex);
        if (!(s instanceof AppEngSlot appEngSlot)) {
            return;
        }
        if (!appEngSlot.isSlotEnabled()) {
            return;
        }

        if (s instanceof FakeSlot fakeSlot && fakeSlot.canSetFilterTo(item)) {
            s.putStack(item);
        }
    }

    public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
        if (slot < 0 || slot >= this.slots.size()) {
            return;
        }
        var s = this.getSlot(slot);

        if (s instanceof CraftingTermSlot craftingTermSlot) {
            switch (action) {
                case CRAFT_SHIFT:
                case CRAFT_ALL:
                case CRAFT_ITEM:
                case CRAFT_STACK:
                    craftingTermSlot.doClick(action, player);
                default:
            }
        }

        if (s instanceof FakeSlot fakeSlot) {
            handleFakeSlotAction(fakeSlot, action);
            return; // No further interaction allowed with fake slots
        }

        // We support filling and emptying for slots backed by generic inventories
        if (s instanceof AppEngSlot appEngSlot
                && appEngSlot.getInventory() instanceof ConfigMenuInventory configInv
                && configInv.getDelegate().getMode() == GenericStackInv.Mode.STORAGE) {
            var realInv = configInv.getDelegate();
            var realInvSlot = appEngSlot.getSlotIndex();

            if (action == InventoryAction.FILL_ITEM || action == InventoryAction.FILL_ENTIRE_ITEM) {
                var what = realInv.getKey(realInvSlot);
                handleFillingHeldItem(
                        (amount, mode) -> realInv.extract(realInvSlot, what, amount, mode),
                        what, action == InventoryAction.FILL_ENTIRE_ITEM);
            } else if (action == InventoryAction.EMPTY_ITEM || action == InventoryAction.EMPTY_ENTIRE_ITEM) {
                handleEmptyHeldItem((what, amount, mode) -> realInv.insert(realInvSlot, what, amount, mode),
                        action == InventoryAction.EMPTY_ENTIRE_ITEM);
            }
        }

        if (action == InventoryAction.MOVE_REGION) {
            var slotSemantic = getSlotSemantic(s);
            if (slotSemantic != null) {
                // Avoid potential concurrent modification i.e. if an upgrade is moved into the machine
                List<Slot> slotsToMove = ImmutableList.copyOf(getSlots(slotSemantic));
                for (var slotToMove : slotsToMove) {
                    quickMoveStack(player, slotToMove.slotNumber);
                }
            } else {
                quickMoveStack(player, s.slotNumber);
            }
        }

    }

    protected interface FillingSource {
        long extract(long amount, Actionable mode);
    }

    protected final void handleFillingHeldItem(FillingSource source, AEKey what, boolean fillAll) {
        var ctx = ContainerItemStrategies.findCarriedContextForKey(what, getPlayer(), this);
        if (ctx == null) {
            return;
        }

        long amount = fillAll ? Long.MAX_VALUE : what.getAmountPerUnit();
        boolean filled = false;
        int maxIterations = fillAll ? MAX_CONTAINER_TRANSFER_ITERATIONS : 1;

        while (maxIterations > 0) {
            // Check if we can pull out of the system
            var canPull = source.extract(amount, Actionable.SIMULATE);
            if (canPull <= 0) {
                break;
            }

            // Check how much we can store in the item
            long amountAllowed = ctx.insert(what, canPull, Actionable.SIMULATE);
            if (amountAllowed == 0) {
                break; // Nothing.
            }

            // Now actually pull out of the system
            var extracted = source.extract(amountAllowed, Actionable.MODULATE);
            if (extracted <= 0) {
                // Something went wrong
                AELog.error("Unable to pull fluid out of the ME system even though the simulation said yes ");
                break;
            }

            // Actually store possible amount in the held item
            long inserted = ctx.insert(what, extracted, Actionable.MODULATE);
            if (inserted == 0) {
                break;
            }

            filled = true;
            maxIterations--;
        }

        if (filled) {
            ctx.playFillSound(getPlayer(), what);
        }
    }

    protected interface EmptyingSink {
        long insert(AEKey what, long amount, Actionable mode);
    }

    protected final void handleEmptyHeldItem(EmptyingSink sink, boolean emptyAll) {
        var ctx = ContainerItemStrategies.findCarriedContext(null, getPlayer(), this);
        if (ctx == null) {
            return;
        }

        // See how much we can drain from the item
        var content = ctx.getExtractableContent();
        if (content == null || content.amount() == 0) {
            return;
        }

        var what = content.what();
        long amount = emptyAll ? Long.MAX_VALUE : what.getAmountPerUnit();
        int maxIterations = emptyAll ? MAX_CONTAINER_TRANSFER_ITERATIONS : 1;
        boolean emptied = false;

        while (maxIterations > 0) {
            // Check if we can pull out of the container
            var canExtract = ctx.extract(what, amount, Actionable.SIMULATE);
            if (canExtract <= 0) {
                break;
            }

            // Check if we can push into the system
            var amountAllowed = sink.insert(what, canExtract, Actionable.SIMULATE);
            if (amountAllowed <= 0) {
                break;
            }

            // Actually drain
            var extracted = ctx.extract(what, amountAllowed, Actionable.MODULATE);
            if (extracted != amountAllowed) {
                AELog.error(
                        "Fluid item [%s] reported a different possible amount to drain than it actually provided.",
                        getCarried());
                break;
            }

            // Actually push into the system
            if (sink.insert(what, extracted, Actionable.MODULATE) != extracted) {
                AELog.error("Failed to insert previously simulated %s into ME system", what);
                break;
            }

            emptied = true;
            maxIterations--;
        }

        if (emptied) {
            ctx.playEmptySound(getPlayer(), what);
        }
    }

    private void handleFakeSlotAction(FakeSlot fakeSlot, InventoryAction action) {
        var hand = getCarried();

        switch (action) {
            case PICKUP_OR_SET_DOWN:
                fakeSlot.increase(hand);
                break;
            case PLACE_SINGLE:
                if (!hand.isEmpty()) {
                    var is = hand.copy();
                    is.setCount(1);
                    fakeSlot.increase(is);
                }

                break;
            case SPLIT_OR_PLACE_SINGLE:
                var is = fakeSlot.getItem();
                if (!is.isEmpty()) {
                    fakeSlot.decrease(hand);
                } else if (!hand.isEmpty()) {
                    is = hand.copy();
                    is.setCount(1);
                    fakeSlot.set(is);
                }

                break;
            case EMPTY_ITEM: {
                var emptyingAction = ContainerItemStrategies.getEmptyingAction(hand);
                if (emptyingAction != null) {
                    fakeSlot.set(GenericStack.wrapInItemStack(emptyingAction.what(), emptyingAction.maxAmount()));
                }
            }
                break;
            default:
                break;
        }
    }

    /**
     * @return Returns how many items were transfered.
     */
    protected int transferStackToMenu(ItemStack input) {
        return 0;
    }

    public void swapSlotContents(int slotA, int slotB) {
        final Slot a = this.getSlot(slotA);
        final Slot b = this.getSlot(slotB);

        // NPE protection...
        if (a == null || b == null) {
            return;
        }

        final ItemStack isA = a.getStack();
        final ItemStack isB = b.getStack();

        // something to do?
        if (isA.isEmpty() && isB.isEmpty()) {
            return;
        }

        // can take?

        if (!isA.isEmpty() && !a.canTakeStack(this.getPlayerInventory().player)) {
            return;
        }

        if (!isB.isEmpty() && !b.canTakeStack(this.getPlayerInventory().player)) {
            return;
        }

        // swap valid?

        if (!isB.isEmpty() && !a.isItemValid(isB)) {
            return;
        }

        if (!isA.isEmpty() && !b.isItemValid(isA)) {
            return;
        }

        ItemStack testA = isB.isEmpty() ? ItemStack.EMPTY : isB.copy();
        ItemStack testB = isA.isEmpty() ? ItemStack.EMPTY : isA.copy();

        // can put some back?
        if (!testA.isEmpty() && testA.getCount() > a.getSlotStackLimit()) {
            if (!testB.isEmpty()) {
                return;
            }

            final int totalA = testA.getCount();
            testA.setCount(a.getSlotStackLimit());
            testB = testA.copy();

            testB.setCount(totalA - testA.getCount());
        }

        if (!testB.isEmpty() && testB.getCount() > b.getSlotStackLimit()) {
            if (!testA.isEmpty()) {
                return;
            }

            final int totalB = testB.getCount();
            testB.setCount(b.getSlotStackLimit());
            testA = testB.copy();

            testA.setCount(totalB - testA.getCount());
        }

        a.putStack(testA);
        b.putStack(testB);
    }

    /**
     * Can be overridden in subclasses to be notified of GUI data updates sent by the server.
     */
    @MustBeInvokedByOverriders
    public void onServerDataSync(ShortSet updatedFields) {
    }

    public void onSlotChange(Slot s) {

    }

    public boolean isValidForSlot(Slot s, ItemStack i) {
        return true;
    }

    public boolean isValidMenu() {
        return this.menuValid;
    }

    public void setValidMenu(boolean isContainerValid) {
        this.menuValid = isContainerValid;
    }

    public MenuHostLocator getLocator() {
        return this.locator;
    }

    public void setLocator(MenuHostLocator locator) {
        this.locator = locator;
    }

    /**
     * Returns whether this menu instance lives on the client.
     */
    public boolean isClientSide() {
        return getPlayer().getEntityWorld().isRemote;
    }

    /**
     * Returns whether this menu instance lives on the server.
     */
    protected boolean isServerSide() {
        return !isClientSide();
    }

    protected final void sendPacketToClient(ClientboundPacket packet) {
        if (getPlayer() instanceof EntityPlayerMP serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        if (listener instanceof EntityPlayerMP) {
            sendAllDataToRemote();
        }
    }

    /**
     * Sends the full state of the {@code @GuiSync} fields (the slots are sent by vanilla when the listener is added).
     */
    public void sendAllDataToRemote() {
        if (dataSync.hasFields()) {
            sendPacketToClient(new GuiDataSyncPacket(containerId, dataSync::writeFull));
        }
    }

    /**
     * Receives data from the server for synchronizing fields of this class.
     */
    public final void receiveServerSyncData(RegistryFriendlyByteBuf data) {
        ShortSet updatedFields = new ShortOpenHashSet();
        this.dataSync.readUpdate(data, updatedFields);
        this.onServerDataSync(updatedFields);
    }

    /**
     * Receives a menu action from the client.
     */
    public final void receiveClientAction(String actionName, @Nullable String jsonPayload) {
        ClientAction<?> action = clientActions.get(actionName);
        if (action == null) {
            throw new IllegalArgumentException("Unknown client action: '" + actionName + "'");
        }

        action.handle(jsonPayload);
    }

    /**
     * Registers a handler for a client-initiated GUI action. Keep in mind in your handler that the client can send
     * arbitrary data. The given argument class will be serialized as JSON across the wire, so it must be GSON
     * serializable.
     */
    protected final <T> void registerClientAction(String name, Class<T> argClass, Consumer<T> handler) {
        if (clientActions.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate client action registered: " + name);
        }

        clientActions.put(name, new ClientAction<>(name, argClass, handler));
    }

    /**
     * Convenience function for registering a client action with no argument.
     *
     * @see #registerClientAction(String, Class, Consumer)
     */
    protected final void registerClientAction(String name, Runnable callback) {
        registerClientAction(name, Void.class, arg -> callback.run());
    }

    /**
     * Trigger a client action on the server.
     */
    protected final <T> void sendClientAction(String action, T arg) {
        ClientAction<?> clientAction = clientActions.get(action);
        if (clientAction == null) {
            throw new IllegalArgumentException("Trying to send unregistered client action: " + action);
        }

        // Serialize the argument to JSON for sending it in the packet
        String jsonPayload;
        if (clientAction.argClass == Void.class) {
            if (arg != null) {
                throw new IllegalArgumentException(
                        "Client action " + action + " requires no argument, but it was given");
            }
            jsonPayload = null;
        } else {
            if (arg == null) {
                throw new IllegalArgumentException(
                        "Client action " + action + " requires an argument, but none was given");
            }
            if (clientAction.argClass != arg.getClass()) {
                throw new IllegalArgumentException(
                        "Trying to send client action " + action + " with wrong argument type " + arg.getClass()
                                + ", expected: " + clientAction.argClass);
            }
            jsonPayload = clientAction.gson.toJson(arg);
        }

        // We do not allow for longer strings than 32kb
        if (jsonPayload != null && jsonPayload.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(
                    "Cannot send client action " + action + " because serialized argument is longer than "
                            + MAX_STRING_LENGTH + " (" + jsonPayload.length() + ")");
        }

        ServerboundPacket message = new GuiActionPacket(containerId, clientAction.name, jsonPayload);
        PacketDistributor.sendToServer(message);
    }

    /**
     * Sends a previously registered client action to the server.
     */
    protected final void sendClientAction(String action) {
        sendClientAction(action, (Void) null);
    }

    /**
     * Registration for an action that a client can initiate.
     */
    private static class ClientAction<T> {
        private final Gson gson = new GsonBuilder().create();
        private final String name;
        private final Class<T> argClass;
        private final Consumer<T> handler;

        public ClientAction(String name, Class<T> argClass, Consumer<T> handler) {
            this.name = name;
            this.argClass = argClass;
            this.handler = handler;
        }

        public void handle(@Nullable String jsonPayload) {
            T arg = null;
            if (argClass != Void.class) {
                AELog.debug("Handling client action '%s' with payload %s", name, jsonPayload);
                arg = gson.fromJson(jsonPayload, argClass);
            } else {
                AELog.debug("Handling client action '%s'", name);
            }

            this.handler.accept(arg);
        }
    }

    protected final void setupUpgrades(IUpgradeInventory upgrades) {
        for (int i = 0; i < upgrades.size(); i++) {
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, i);
            slot.setNotDraggable();
            this.addSlot(slot, SlotSemantics.UPGRADE);
        }
    }

    public boolean isReturnedFromSubScreen() {
        return returnedFromSubScreen;
    }

    public void setReturnedFromSubScreen(boolean returnedFromSubScreen) {
        this.returnedFromSubScreen = returnedFromSubScreen;
    }
}
