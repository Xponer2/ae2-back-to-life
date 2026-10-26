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

package appeng.parts;

import appeng.api.components.DataComponentMap;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.items.tools.MemoryCardItem;
import appeng.util.BuiltInRegistries;
import appeng.util.IDebugExportable;
import appeng.util.InteractionUtil;
import appeng.util.JsonStreamUtil;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import com.google.common.collect.ImmutableMap;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

public abstract class AEBasePart
        implements IPart, IActionHost, ISegmentedInventory, IPowerChannelState, IWorldNameable, IDebugExportable {

    private final IManagedGridNode mainNode;
    private IPartItem<?> partItem;
    private TileEntity blockEntity = null;
    private IPartHost host = null;
    @Nullable
    private EnumFacing side;
    @Nullable
    private ITextComponent customName;

    // On the client-side this is the state last sent by the server.
    // On the server it's the state last sent to the client.
    private boolean clientSidePowered;
    private boolean clientSideMissingChannel;

    public AEBasePart(IPartItem<?> partItem) {
        this.partItem = Objects.requireNonNull(partItem, "partItem");
        this.mainNode = createMainNode().setVisualRepresentation(AEItemKey.of(this.partItem))
                .setExposedOnSides(EnumSet.noneOf(EnumFacing.class));
    }

    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, NodeListener.INSTANCE);
    }

    /**
     * Called if one of the properties that result in a node becoming active or inactive change.
     *
     * @param reason Indicates which of the properties has changed.
     */
    @MustBeInvokedByOverriders
    protected void onMainNodeStateChanged(State reason) {
        // Client flags shouldn't depend on grid boot, optimize!
        if (reason != State.GRID_BOOT) {
            markForUpdateIfClientFlagsChanged();
        }
    }

    public final boolean isClientSide() {
        return this.blockEntity == null || this.blockEntity.getWorld() == null
                || this.blockEntity.getWorld().isRemote;
    }

    public IPartHost getHost() {
        return this.host;
    }

    protected AEColor getColor() {
        if (this.host == null) {
            return AEColor.TRANSPARENT;
        }
        return this.host.getColor();
    }

    public IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Override
    public IGridNode getActionableNode() {
        return this.mainNode.getNode();
    }

    public final TileEntity getTileEntity() {
        return blockEntity;
    }

    public World getLevel() {
        return this.blockEntity.getWorld();
    }

    /**
     * The custom name of the part, or the name of its item (1.21 {@code Nameable.getName()}).
     */
    @Override
    public ITextComponent getDisplayName() {
        return this.customName != null ? this.customName : getItemDescription(partItem.asItem());
    }

    /**
     * The name for {@link IWorldNameable} of this version is a plain string.
     */
    @Override
    public String getName() {
        return getDisplayName().getUnformattedText();
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null;
    }

    @Nullable
    public ITextComponent getCustomName() {
        return this.customName;
    }

    /**
     * Same as modern {@code Item.getDescription()}: the translatable name of the item.
     */
    protected static ITextComponent getItemDescription(Item item) {
        var key = item.getTranslationKey(new ItemStack(item));
        // The lang files of AE2 use the modern keys (no ".name" suffix), the ones of 1.12.2 end with ".name"
        var id = item.getRegistryName();
        if (id == null || !AppEng.MOD_ID.equals(id.getNamespace())) {
            key += ".name";
        }
        return new TextComponentTranslation(key);
    }

    @Override
    public void addEntityCrashInfo(CrashReportCategory crashreportcategory) {
        crashreportcategory.addCrashSection("Part Side", this.getSide());
        var beHost = getTileEntity();
        if (beHost != null) {
            beHost.addInfoToCrashReport(crashreportcategory);
            var level = beHost.getWorld();
            if (level != null) {
                crashreportcategory.addCrashSection("World", level.provider.getDimension());
            }
        }
    }

    @Override
    public IPartItem<?> getPartItem() {
        return this.partItem;
    }

    /**
     * Advanced method. Take care to properly update any grid related state and update the host after changing the part
     * item.
     */
    protected void setPartItem(IPartItem<?> partItem) {
        if (partItem != this.partItem) {
            this.partItem = Objects.requireNonNull(partItem);
            this.getMainNode().setVisualRepresentation(partItem);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        this.mainNode.loadFromNBT(data);

        if (data.hasKey("customName")) {
            try {
                this.customName = ITextComponent.Serializer.jsonToComponent(data.getString("customName"));
            } catch (Exception ignored) {
            }
        }

        if (data.hasKey("visual", Constants.NBT.TAG_COMPOUND)) {
            readVisualStateFromNBT(data.getCompoundTag("visual"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        this.mainNode.saveToNBT(data);

        if (this.customName != null) {
            data.setString("customName", ITextComponent.Serializer.componentToJson(this.customName));
        }
    }

    @MustBeInvokedByOverriders
    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        this.clientSidePowered = this.isPowered();
        this.clientSideMissingChannel = this.isMissingChannel();

        var flags = 0;
        if (clientSidePowered) {
            flags |= 1;
        }
        if (clientSideMissingChannel) {
            flags |= 2;
        }
        data.writeByte(flags);
    }

    @MustBeInvokedByOverriders
    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        var flags = data.readByte();

        var wasPowered = this.clientSidePowered;
        var wasMissingChannel = this.clientSideMissingChannel;

        this.clientSidePowered = (flags & 1) != 0;
        this.clientSideMissingChannel = (flags & 2) != 0;

        return shouldSendPowerStateToClient() && clientSidePowered != wasPowered
                || shouldSendMissingChannelStateToClient() && clientSideMissingChannel != wasMissingChannel;
    }

    /**
     * Used to store the state that is synchronized to clients for the visual appearance of this part as NBT. This is
     * only used to store this state for tools such as Create Ponders in Structure NBT. Actual synchronization uses
     * {@link IPart#writeToStream(RegistryFriendlyByteBuf)} and {@link IPart#readFromStream(RegistryFriendlyByteBuf)}.
     * Any data that is saved to the NBT tag in {@link IPart#writeToNBT(NBTTagCompound, HolderLookup.Provider)} already
     * does not need to be saved here again.
     */
    @MustBeInvokedByOverriders
    @Override
    public void writeVisualStateToNBT(NBTTagCompound data) {
        data.setBoolean("powered", this.isPowered());
        data.setBoolean("missingChannel", this.isMissingChannel());
    }

    /**
     * Loads data saved by {@link #writeVisualStateToNBT(NBTTagCompound)}.
     */
    @MustBeInvokedByOverriders
    @Override
    public void readVisualStateFromNBT(NBTTagCompound data) {
        this.clientSidePowered = data.getBoolean("powered");
        this.clientSideMissingChannel = data.getBoolean("missingChannel");
    }

    @Override
    public IGridNode getGridNode() {
        return this.mainNode.getNode();
    }

    @Override
    public void removeFromWorld() {
        this.mainNode.destroy();
    }

    @Override
    public void addToWorld() {
        this.mainNode.create(getLevel(), blockEntity.getPos());
    }

    @Override
    public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        this.setSide(side);
        this.blockEntity = blockEntity;
        this.host = host;
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 3;
    }

    /**
     * depending on the from, different settings will be accepted
     *
     * @param mode   source of settings
     * @param input  compound of source
     * @param player the optional player who is importing the settings
     */
    @Override
    @MustBeInvokedByOverriders
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable EntityPlayer player) {
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            this.customName = input.get(AEComponents.CUSTOM_NAME);
        } else if (mode == SettingsFrom.MEMORY_CARD) {
            this.customName = input.get(AEComponents.EXPORTED_CUSTOM_NAME);
        }

        MemoryCardItem.importGenericSettings(this, input, player);
    }

    @Override
    @MustBeInvokedByOverriders
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder) {
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            builder.set(AEComponents.CUSTOM_NAME, this.customName);
        } else if (mode == SettingsFrom.MEMORY_CARD) {
            builder.set(AEComponents.EXPORTED_CUSTOM_NAME, this.customName);
        }

        if (mode == SettingsFrom.MEMORY_CARD) {
            MemoryCardItem.exportGenericSettings(this, builder);
            builder.set(AEComponents.EXPORTED_SETTINGS_SOURCE, getItemDescription(getPartItem().asItem()));
        }
    }

    public final DataComponentMap exportSettings(SettingsFrom mode) {
        var builder = DataComponentMap.builder();
        exportSettings(mode, builder);
        return builder.build();
    }

    public boolean useStandardMemoryCard() {
        return true;
    }

    private boolean useMemoryCard(ItemStack memCardIS, EntityPlayer player) {
        if (!this.useStandardMemoryCard() || !(memCardIS.getItem() instanceof IMemoryCard memoryCard)) {
            return false;
        }

        Item partItem = getPartItem().asItem();

        // Blocks and parts share the same soul!
        if (AEParts.INTERFACE.asItem() == partItem) {
            partItem = AEBlocks.INTERFACE.asItem();
        } else if (AEParts.PATTERN_PROVIDER.asItem() == partItem) {
            partItem = AEBlocks.PATTERN_PROVIDER.asItem();
        }

        var name = getItemDescription(partItem);

        if (InteractionUtil.isInAlternateUseMode(player)) {
            var settings = exportSettings(SettingsFrom.MEMORY_CARD);
            if (!settings.isEmpty()) {
                MemoryCardItem.clearCard(memCardIS);
                settings.applyTo(memCardIS);
                memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
            }
        } else {
            var storedName = AEComponents.EXPORTED_SETTINGS_SOURCE.get(memCardIS);
            if (name.equals(storedName)) {
                importSettings(SettingsFrom.MEMORY_CARD, DataComponentMap.of(memCardIS), player);
                memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            } else {
                MemoryCardItem.importGenericSettingsAndNotify(this, DataComponentMap.of(memCardIS), player);
            }
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (useMemoryCard(heldItem, player)) {
            return true;
        }
        return IPart.super.onUseItemOn(heldItem, player, hand, pos);
    }

    @Override
    public void onPlacement(EntityPlayer player) {
        this.mainNode.setOwningPlayer(player);
    }

    public EnumFacing getSide() {
        return this.side;
    }

    private void setSide(EnumFacing side) {
        this.side = side;
    }

    @Nullable
    @Override
    @MustBeInvokedByOverriders
    public InternalInventory getSubInventory(ResourceLocation id) {
        return null;
    }

    /**
     * Simple {@link IGridNodeListener} for {@link AEBasePart} that host nodes.
     */
    public static class NodeListener<T extends AEBasePart> implements IGridNodeListener<T> {

        public static final NodeListener<AEBasePart> INSTANCE = new NodeListener<>();

        @Override
        public void onSaveChanges(T nodeOwner, IGridNode node) {
            nodeOwner.getHost().markForSave();
        }

        @Override
        public void onStateChanged(T nodeOwner, IGridNode node, State state) {
            nodeOwner.onMainNodeStateChanged(state);
        }
    }

    public boolean isPowered() {
        if (isClientSide()) {
            return clientSidePowered;
        } else {
            var node = getGridNode();
            return node != null && node.isPowered();
        }
    }

    public boolean isMissingChannel() {
        if (isClientSide()) {
            return clientSideMissingChannel;
        } else {
            var node = getGridNode();
            return node == null || !node.meetsChannelRequirements();
        }
    }

    @Override
    public boolean isActive() {
        return isPowered() && !isMissingChannel();
    }

    /**
     * Updates the entire part on the client-side if one of the flags relevant for its visual appearance has changed.
     */
    private void markForUpdateIfClientFlagsChanged() {
        var changed = false;

        if (shouldSendPowerStateToClient()) {
            if (isPowered() != this.clientSidePowered) {
                changed = true;
            }
        }

        if (!changed && shouldSendMissingChannelStateToClient()) {
            if (isMissingChannel() != this.clientSideMissingChannel) {
                changed = true;
            }
        }

        if (changed) {
            getHost().markForUpdate();
        }
    }

    /**
     * Override and return false if your part has no visual indicator for the power state and doesn't need this info on
     * the client.
     */
    protected boolean shouldSendPowerStateToClient() {
        return true;
    }

    /**
     * Override and return false if your part has no visual indicator for the missing channel state and doesn't need
     * this info on the client.
     */
    protected boolean shouldSendMissingChannelStateToClient() {
        return true;
    }

    @Override
    public void debugExport(JsonWriter writer, Reference2IntMap<Object> machineIds,
            Reference2IntMap<IGridNode> nodeIds)
            throws IOException {
        var myId = machineIds.getOrDefault(this, -1);
        JsonStreamUtil.writeProperties(ImmutableMap.of(
                "id", myId,
                "item", BuiltInRegistries.ITEM.getKey(getPartItem().asItem()).toString(),
                "mainNodeId", nodeIds.getOrDefault(mainNode.getNode(), -1)), writer);
    }
}
