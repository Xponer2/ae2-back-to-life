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

package appeng.blockentity;

import net.minecraft.block.properties.IProperty;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import appeng.api.capabilities.RegisterCapabilitiesEvent;
import appeng.api.components.DataComponentMap;
import appeng.api.ids.AEComponents;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.IOrientationHolder;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.Clearable;
import appeng.api.util.GlobalPos;
import appeng.block.AEBaseEntityBlock;
import appeng.client.render.model.AEModelData;
import appeng.client.render.model.ModelData;
import appeng.core.AELog;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.hooks.VisualStateSaving;
import appeng.hooks.ticking.TickHandler;
import appeng.items.tools.MemoryCardItem;
import appeng.util.AECodecs;
import appeng.util.DataResults;
import appeng.util.IDebugExportable;
import appeng.util.JsonStreamUtil;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import appeng.util.helpers.ItemComparisonHelper;

/**
 * Base class of all AE2 tile entities. Provides the Minecraft 1.21 block entity API that AE2's code is written against
 * ({@link #getLevel()}, {@link #getBlockState()}, {@link #saveAdditional}, {@link #loadTag}, {@link #clearRemoved()},
 * {@link #setRemoved()}, ...) on top of 1.12.2's {@link TileEntity}. It also stores the orientation of the block (see
 * {@link IOrientationHolder}).
 */
public class AEBaseBlockEntity extends TileEntity
        implements IWorldNameable, ISegmentedInventory, Clearable, IDebugExportable, IOrientationHolder {
    private static final Logger LOG = LogManager.getLogger(AEBaseBlockEntity.class);

    private static final Map<BlockEntityType<?>, Item> REPRESENTATIVE_ITEMS = new HashMap<>();
    private static final Map<Class<?>, java.util.List<BlockEntityType<?>>> TYPES_BY_CLASS = new HashMap<>();

    @Nullable
    private ITextComponent customName;
    private boolean setChangedQueued = false;
    private BlockOrientation orientation = BlockOrientation.NORTH_UP;
    /**
     * For diagnosing issues with the delayed block entity initialization, this tracks how often this BE has been queued
     * for defered initializiation using {@link GridHelper#onFirstTick}.
     */
    private byte queuedForReady = 0;
    /**
     * Tracks how often {@link #onReady()} has been called. This should always be less than {@link #queuedForReady}, and
     * subsequently be equal.
     */
    private byte readyInvoked = 0;
    private boolean removed = true;
    // Set when invalidate() removed this block entity, see validate()
    private boolean removedByInvalidate;

    public AEBaseBlockEntity() {
        super();
    }

    public static void registerBlockEntityItem(BlockEntityType<?> type, Item wat) {
        REPRESENTATIVE_ITEMS.put(type, wat);
    }

    public static void registerType(BlockEntityType<?> type) {
        TYPES_BY_CLASS.computeIfAbsent(type.getBlockEntityClass(), c -> new java.util.ArrayList<>()).add(type);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Modern BlockEntity API
    // ---------------------------------------------------------------------------------------------------------------

    @Nullable
    public World getLevel() {
        return world;
    }

    public boolean hasLevel() {
        return world != null;
    }

    /**
     * @return The AE2 block entity type of this tile entity (determined by its class and, for shared classes, block).
     */
    @Nullable
    public BlockEntityType<?> getType() {
        var types = TYPES_BY_CLASS.get(getClass());
        if (types == null || types.isEmpty()) {
            return null;
        }
        if (types.size() == 1 || world == null) {
            return types.get(0);
        }
        var block = world.getBlockState(pos).getBlock();
        for (var type : types) {
            if (type.getValidBlocks().contains(block)) {
                return type;
            }
        }
        return types.get(0);
    }

    /**
     * The actual block state (including the orientation and the state derived from this block entity).
     */
    // ---------------------------------------------------------------------------------------------------------------
    // Block state properties kept by the block entity
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Block state properties of this block that 1.21 keeps in the block state (controller status, cell fullness,
     * formed, ...). On 1.12.2 they don't fit into the metadata of a block entity block, so they are stored here, saved,
     * synchronized with the block entity and applied by {@link AEBaseEntityBlock#getActualState}.
     */
    private final Map<String, String> stateProperties = new HashMap<>();
    // The last state of the block of this block entity, see getBlockState()
    @Nullable
    private IBlockState lastKnownState;

    /**
     * 1.21 {@code level.setBlockAndUpdate(pos, state.setValue(property, value))}: the clients and the neighbors are
     * notified.
     */
    public <T extends Comparable<T>> void setBlockStateProperty(IProperty<T> property, T value) {
        setBlockStateProperty(property, value, true);
    }

    /**
     * @param notifyNeighbors false for 1.21 {@code Block.UPDATE_CLIENTS} (multiblocks and controllers, which would
     *                        otherwise update each other in loops)
     */
    public <T extends Comparable<T>> void setBlockStateProperty(IProperty<T> property, T value,
            boolean notifyNeighbors) {
        var name = property.getName(value);
        if (!name.equals(stateProperties.put(property.getName(), name))) {
            saveChanges();
            markForUpdate();
            if (notifyNeighbors && world != null && !world.isRemote && !notLoaded()) {
                world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
            }
            // vanilla 1.21 tells the block entity about its new block state
            if (world != null) {
                setBlockState(getBlockState());
            }
        }
    }

    public IBlockState applyBlockStateProperties(IBlockState state) {
        for (var entry : stateProperties.entrySet()) {
            var property = findProperty(state, entry.getKey());
            if (property != null) {
                state = withValue(state, property, entry.getValue());
            }
        }
        return state;
    }

    @Nullable
    private static IProperty<?> findProperty(IBlockState state, String name) {
        for (var property : state.getPropertyKeys()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    private static <T extends Comparable<T>> IBlockState withValue(IBlockState state, IProperty<T> property,
            String valueName) {
        var value = property.parseValue(valueName);
        return value.isPresent() ? state.withProperty(property, value.get()) : state;
    }

    private void writeStateProperties(RegistryFriendlyByteBuf data) {
        data.writeVarInt(stateProperties.size());
        for (var entry : stateProperties.entrySet()) {
            data.writeUtf(entry.getKey());
            data.writeUtf(entry.getValue());
        }
    }

    private boolean readStateProperties(RegistryFriendlyByteBuf data) {
        var read = new HashMap<String, String>();
        int count = data.readVarInt();
        for (int i = 0; i < count; i++) {
            read.put(data.readUtf(), data.readUtf());
        }
        if (read.equals(stateProperties)) {
            return false;
        }
        stateProperties.clear();
        stateProperties.putAll(read);
        return true;
    }

    public IBlockState getBlockState() {
        if (world == null) {
            var block = getBlockType();
            if (block == null) {
                // Not in a world yet (constructor, loading from NBT): the default state of the block of this type, like
                // the state that newer versions pass to the constructor
                var type = getType();
                if (type != null && !type.getValidBlocks().isEmpty()) {
                    block = type.getValidBlocks().iterator().next();
                }
            }
            return block != null ? block.getDefaultState() : Blocks.AIR.getDefaultState();
        }
        var state = world.getBlockState(pos);
        if (state.getBlock() instanceof AEBaseEntityBlock<?>) {
            lastKnownState = state.getActualState(world, pos);
            return lastKnownState;
        }
        // The block is already gone while the block entity is being removed: newer versions keep the state of a block
        // entity until it is removed (energy cells read their priority from it then)
        if (lastKnownState != null) {
            return lastKnownState;
        }
        return state;
    }

    public boolean isRemoved() {
        return removed || isInvalid();
    }

    /**
     * Modern clearRemoved: called when the block entity is added to a loaded level.
     */
    @MustBeInvokedByOverriders
    public void clearRemoved() {
        removed = false;
        removedByInvalidate = false;
    }

    /**
     * Modern setRemoved: called when the block entity is removed from the level.
     */
    @MustBeInvokedByOverriders
    public void setRemoved() {
        removed = true;
    }

    /**
     * Modern onChunkUnloaded.
     */
    public void onChunkUnloaded() {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        clearRemoved();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!removed) {
            setRemoved();
            removedByInvalidate = true;
        }
    }

    /**
     * Block entities of a chunk that is loaded while the level ticks its block entities (a neighbor reading into the
     * chunk, for example) are added later in the same tick: {@code World#updateEntities} calls onLoad and then puts
     * the same block entity into the chunk again, which calls invalidate() and validate() on it. It stays in the level,
     * so the removal is undone, otherwise it would never send updates to the clients again (markForUpdate skips
     * removed block entities). The initialization queued by onLoad hasn't run at that point and still creates the grid
     * node, so it is not queued again (a node can only be created once). Block entities without a deferred
     * initialization simply become loaded again.
     */
    @Override
    public void validate() {
        super.validate();
        if (removedByInvalidate) {
            removedByInvalidate = false;
            if (queuedForReady > readyInvoked) {
                removed = false;
            } else if (queuedForReady == 0) {
                clearRemoved(); // no deferred initialization
            } else {
                // Not a path of the game itself: the grid node was destroyed and can't be created again
                AELog.warn("Block entity %s at %s was invalidated and validated again after its initialization",
                        getClass().getSimpleName(), pos);
            }
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        onChunkUnloaded();
        if (!removed) {
            setRemoved();
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        // Only recreate the tile entity if the block changes, not for metadata changes
        return oldState.getBlock() != newSate.getBlock();
    }

    /**
     * Modern requestModelDataUpdate: re-renders the block on the client.
     */
    public void requestModelDataUpdate() {
        if (world != null && world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    /**
     * Capabilities are queried on demand on 1.12.2, nothing to invalidate.
     */
    public void invalidateCapabilities() {
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return RegisterCapabilitiesEvent.get().hasBlockEntityCapability(this, capability, facing)
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <C> C getCapability(Capability<C> capability, @Nullable EnumFacing facing) {
        var result = RegisterCapabilitiesEvent.get().getBlockEntityCapability(this, capability, facing);
        if (result != null) {
            return result;
        }
        return super.getCapability(capability, facing);
    }

    public boolean notLoaded() {
        return this.world == null || !this.world.isBlockLoaded(this.pos);
    }

    public final GlobalPos getGlobalPos() {
        if (world == null) {
            throw new IllegalStateException("Block entity is not in a level");
        }
        return GlobalPos.of(world, getPos());
    }

    public TileEntity getTileEntity() {
        return this;
    }

    protected Item getItemFromBlockEntity() {
        var type = getType();
        var item = type != null ? REPRESENTATIVE_ITEMS.get(type) : null;
        if (item != null) {
            return item;
        }
        var block = getBlockType();
        return block != null ? Item.getItemFromBlock(block) : Items.AIR;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Saving / loading
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public final void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        loadAdditional(tag);
    }

    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        saveAdditional(compound);
        return compound;
    }

    public final void loadAdditional(NBTTagCompound tag) {
        // Load visual client-side data
        if (tag.hasKey("visual", Constants.NBT.TAG_COMPOUND)) {
            loadVisualState(tag.getCompoundTag("visual"));
        }

        if (tag.hasKey("orientation", Constants.NBT.TAG_STRING)) {
            BlockOrientation loaded;
            try {
                loaded = BlockOrientation.valueOf(tag.getString("orientation"));
            } catch (IllegalArgumentException e) {
                loaded = BlockOrientation.NORTH_UP;
            }
            if (loaded != this.orientation) {
                this.orientation = loaded;
                // 1.21 passes the block state to the constructor, so what depends on the orientation (the sides the
                // grid node is exposed on, the sides that accept power) is set up with it right away. Here the
                // orientation is only known now.
                onOrientationChanged(loaded);
            }
        }

        this.stateProperties.clear();
        if (tag.hasKey("stateProperties", Constants.NBT.TAG_COMPOUND)) {
            var stateTag = tag.getCompoundTag("stateProperties");
            for (var key : stateTag.getKeySet()) {
                this.stateProperties.put(key, stateTag.getString(key));
            }
        }

        loadTag(tag);
    }

    public void loadTag(NBTTagCompound data) {
        if (data.hasKey("customName")) {
            this.customName = new TextComponentString(data.getString("customName"));
        } else {
            this.customName = null;
        }
    }

    public void saveAdditional(NBTTagCompound data) {
        // Save visual state first, so that it can never overwrite normal state
        if (VisualStateSaving.isEnabled(world)) {
            var visualTag = new NBTTagCompound();
            saveVisualState(visualTag);
            data.setTag("visual", visualTag);
        }

        if (this.orientation != BlockOrientation.NORTH_UP) {
            data.setString("orientation", this.orientation.name());
        }

        if (!this.stateProperties.isEmpty()) {
            var stateTag = new NBTTagCompound();
            this.stateProperties.forEach(stateTag::setString);
            data.setTag("stateProperties", stateTag);
        }

        if (this.customName != null) {
            data.setString("customName", this.customName.getUnformattedText());
        }
    }

    /**
     * Deferred initialization when block entities actually start first ticking in a chunk. The block entity needs to
     * override {@link #clearRemoved()} and call {@link #scheduleInit()} to make this work.
     */
    @MustBeInvokedByOverriders
    public void onReady() {
        readyInvoked++;
    }

    protected void scheduleInit() {
        queuedForReady++;
        GridHelper.onFirstTick(this, AEBaseBlockEntity::onReady);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Client synchronization
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * The tag sent with the chunk data: the vanilla position/id plus our update stream.
     */
    @Override
    public NBTTagCompound getUpdateTag() {
        var data = super.writeToNBT(new NBTTagCompound());

        var stream = new RegistryFriendlyByteBuf(Unpooled.buffer());
        this.writeToStream(stream);
        stream.writeByte(this.orientation.ordinal());
        writeStateProperties(stream);

        var bytes = new byte[stream.readableBytes()];
        stream.readBytes(bytes);
        data.setByteArray("#upd", bytes);
        return data;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        if (tag.hasKey("#upd", Constants.NBT.TAG_BYTE_ARRAY)) {
            if (readUpdateData(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("#upd"))))) {
                // Triggers a chunk re-render if the level is already loaded
                if (world != null) {
                    requestModelDataUpdate();
                }
            }
        }
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 64, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    private boolean readUpdateData(RegistryFriendlyByteBuf stream) {
        boolean output = false;

        try {
            output = this.readFromStream(stream);
            if (stream.isReadable()) {
                var newOrientation = BlockOrientation.values()[stream.readByte()];
                if (newOrientation != this.orientation) {
                    this.orientation = newOrientation;
                    onOrientationChanged(newOrientation);
                    output = true;
                }
                if (stream.isReadable() && readStateProperties(stream)) {
                    output = true;
                }
            }
        } catch (Throwable t) {
            AELog.warn(t);
        }

        return output;
    }

    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        return false;
    }

    protected void writeToStream(RegistryFriendlyByteBuf data) {
    }

    /**
     * Used to store the state that is synchronized to clients for the visual appearance as NBT (structure tools).
     */
    @MustBeInvokedByOverriders
    protected void saveVisualState(NBTTagCompound data) {
    }

    /**
     * @see #saveVisualState(NBTTagCompound)
     */
    @MustBeInvokedByOverriders
    protected void loadVisualState(NBTTagCompound data) {
    }

    /**
     * Mark this block to be updated for clients.
     */
    public void markForClientUpdate() {
        this.requestModelDataUpdate();

        if (this.world != null && !this.isRemoved() && !notLoaded()) {
            var state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 2);
        }
    }

    public void markForUpdate() {
        // Clearing the cached model-data is always harmless regardless of status
        this.requestModelDataUpdate();

        if (this.world != null && !this.isRemoved() && !notLoaded()) {
            // On 1.12.2 the derived block state is computed from this block entity, so sending the update is all
            // that's needed. Like 1.21, the neighbors are only notified when a block state property changes
            // (setBlockStateProperty).
            var state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Orientation
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public final BlockOrientation getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(BlockOrientation orientation) {
        if (this.orientation == orientation) {
            return;
        }
        this.orientation = orientation;
        if (world != null) {
            markDirty();
            if (!world.isRemote) {
                markForUpdate();
            } else {
                requestModelDataUpdate();
            }
        }
        onOrientationChanged(orientation);
    }

    public EnumFacing getFront() {
        return getOrientation().getSide(RelativeSide.FRONT);
    }

    public EnumFacing getTop() {
        return getOrientation().getSide(RelativeSide.TOP);
    }

    /**
     * Called when the orientation of the block the block-entity is attached to is changed.
     */
    @ApiStatus.OverrideOnly
    protected void onOrientationChanged(BlockOrientation orientation) {
        invalidateCapabilities();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Settings, drops, naming
    // ---------------------------------------------------------------------------------------------------------------

    public final DataComponentMap exportSettings(SettingsFrom mode, @Nullable EntityPlayer player) {
        var builder = DataComponentMap.builder();
        exportSettings(mode, builder, player);
        return builder.build();
    }

    /**
     * @param mode    source of settings
     * @param builder
     * @param player  The (optional) player, who is exporting the settings
     */
    @MustBeInvokedByOverriders
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder, @Nullable EntityPlayer player) {
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            builder.set(AEComponents.CUSTOM_NAME, this.customName);
        } else if (mode == SettingsFrom.MEMORY_CARD) {
            builder.set(AEComponents.EXPORTED_CUSTOM_NAME, this.customName);
        }

        if (mode == SettingsFrom.MEMORY_CARD) {
            MemoryCardItem.exportGenericSettings(this, builder);
            builder.set(AEComponents.EXPORTED_SETTINGS_SOURCE, Platform.getItemDescription(getItemFromBlockEntity()));
        }
    }

    /**
     * Depending on the mode, different settings will be accepted.
     *
     * @param input  source of settings
     * @param player The (optional) player, who is importing the settings
     */
    @MustBeInvokedByOverriders
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable EntityPlayer player) {
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var name = input.get(AEComponents.CUSTOM_NAME);
            if (name != null) {
                this.customName = name;
            }
        } else if (mode == SettingsFrom.MEMORY_CARD) {
            this.customName = input.get(AEComponents.EXPORTED_CUSTOM_NAME);
        }

        MemoryCardItem.importGenericSettings(this, input, player);
    }

    /**
     * returns the contents of the block entity but not the block itself, to drop into the world.
     * <p>
     * Ensure you also clear the inventories that contribute to additional drops in {@link #clearContent()} when you
     * override this method.
     *
     * @param level level
     * @param pos   block position
     * @param drops drops of block entity
     */
    @MustBeInvokedByOverriders
    public void addAdditionalDrops(World level, BlockPos pos, List<ItemStack> drops) {
    }

    /**
     * Clears the contents of this block-entity, which would otherwise be dropped by {@link #addAdditionalDrops}.
     */
    @Override
    @MustBeInvokedByOverriders
    public void clearContent() {
    }

    /**
     * Modern Nameable.getName(): custom name or the item's name.
     */
    public ITextComponent getNameComponent() {
        return this.customName != null ? this.customName : Platform.getItemDescription(getItemFromBlockEntity());
    }

    @Override
    public String getName() {
        return getNameComponent().getUnformattedText();
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null;
    }

    @Override
    public ITextComponent getDisplayName() {
        return getNameComponent();
    }

    @Nullable
    public ITextComponent getCustomName() {
        return this.customName;
    }

    /**
     * Checks if this block entity is remote (we are running on the logical client side).
     */
    public boolean isClientSide() {
        World level = getLevel();
        return level == null || level.isRemote;
    }

    public void saveChanges() {
        if (this.world == null) {
            return;
        }

        // Clientside is marked immediately as dirty as there is no queue processing
        // Serverside is only queued once per tick to avoid costly operations
        if (this.world.isRemote) {
            this.markDirty();
        } else {
            this.world.markChunkDirty(this.pos, this);
            if (!this.setChangedQueued) {
                TickHandler.instance().addCallable(null, this::setChangedAtEndOfTick);
                this.setChangedQueued = true;
            }
        }
    }

    private Object setChangedAtEndOfTick(World level) {
        this.markDirty();
        this.setChangedQueued = false;
        return null;
    }

    public void setName(String name) {
        this.customName = new TextComponentString(name);
    }

    @Override
    @Nullable
    @MustBeInvokedByOverriders
    public InternalInventory getSubInventory(ResourceLocation id) {
        return null;
    }

    public ModelData getModelData() {
        return AEModelData.create();
    }

    /**
     * Called when a player uses a wrench on this block entity to disassemble it.
     */
    public EnumActionResult disassembleWithWrench(EntityPlayer player, World level, RayTraceResult hitResult,
            ItemStack wrench) {
        var pos = hitResult.getBlockPos();
        var state = level.getBlockState(pos);
        var block = state.getBlock();

        if (level instanceof WorldServer) {
            // Drops of the block itself (without extra block entity inventory)
            NonNullList<ItemStack> drops = NonNullList.create();
            block.getDrops(drops, level, pos, state, 0);

            var op = new ItemStack(state.getBlock());
            for (var ol : drops) {
                if (ItemComparisonHelper.isEqualItemType(ol, op)) {
                    var settings = exportSettings(SettingsFrom.DISMANTLE_ITEM, player);
                    settings.applyTo(ol);
                    break;
                }
            }

            // Add and remove extra block entity inventory
            addAdditionalDrops(level, pos, drops);
            clearContent();

            for (var item : drops) {
                Platform.placeItemBackInInventory(player, item);
            }
        }

        block.onBlockHarvested(level, pos, state, player);
        level.setBlockToAir(pos);
        block.onPlayerDestroy(level, pos, state);

        return EnumActionResult.SUCCESS;
    }

    public byte getQueuedForReady() {
        return queuedForReady;
    }

    public byte getReadyInvoked() {
        return readyInvoked;
    }

    /**
     * Modern setBlockState: on 1.12.2 only the orientation part can change (the rest is derived).
     */
    public void setBlockState(IBlockState state) {
        if (state.getBlock() instanceof AEBaseEntityBlock<?> block
                && !block.getOrientationStrategy().getProperties().isEmpty()) {
            setOrientation(BlockOrientation.get(block.getOrientationStrategy(), state));
        }
    }

    @Override
    public void debugExport(JsonWriter writer, Reference2IntMap<Object> machineIds,
            Reference2IntMap<IGridNode> nodeIds)
            throws IOException {
        var data = new NBTTagCompound();
        saveAdditional(data);

        var ops = JsonOps.INSTANCE;
        JsonStreamUtil.writeProperties(ImmutableMap.of(
                "blockState", DataResults.getOrThrow(AECodecs.BLOCK_STATE.encodeStart(ops, getBlockState())),
                "level", "DIM" + world.provider.getDimension(),
                "pos", getPos(),
                "data", DataResults.getOrThrow(AECodecs.COMPOUND_TAG.encodeStart(ops, data))), writer);
    }
}
