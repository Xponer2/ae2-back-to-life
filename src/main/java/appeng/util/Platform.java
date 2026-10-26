
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

package appeng.util;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.mojang.authlib.GameProfile;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;

import appeng.api.config.AccessRestriction;
import appeng.api.config.PowerUnit;
import appeng.api.config.SortOrder;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.util.DimensionalBlockPos;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.hooks.ticking.TickHandler;
import appeng.util.helpers.P2PHelper;

public class Platform {

    private static final P2PHelper P2P_HELPER = new P2PHelper();

    public static final EnumFacing[] DIRECTIONS_WITH_NULL = new EnumFacing[] { EnumFacing.DOWN, EnumFacing.UP,
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null };

    public static P2PHelper p2p() {
        return P2P_HELPER;
    }

    /**
     * This displays the value for encoded longs ( double *100 )
     *
     * @param n      to be formatted long value
     * @param isRate if true it adds a /t to the formatted string
     * @return formatted long value
     */
    public static String formatPowerLong(long n, boolean isRate) {
        return formatPower((double) n / 100, isRate);
    }

    public static String formatPower(double p, boolean isRate) {
        var displayUnits = AEConfig.instance().getSelectedEnergyUnit();
        p = PowerUnit.AE.convertTo(displayUnits, p);

        final String[] preFixes = { "k", "M", "G", "T", "P", "T", "P", "E", "Z", "Y" };
        var unitName = displayUnits.getSymbolName();

        String level = "";
        int offset = 0;
        while (p > 1000 && offset < preFixes.length) {
            p /= 1000;
            level = preFixes[offset];
            offset++;
        }

        final DecimalFormat df = new DecimalFormat("#.##");
        return df.format(p) + ' ' + level + unitName + (isRate ? "/t" : "");
    }

    public static String formatTimeMeasurement(long nanos) {
        if (nanos <= 0) {
            return "0 ns";
        } else if (nanos < 1000) {
            return "<1 µs";
        } else if (nanos <= 1000 * 1000) {
            final long ms = TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS);
            return ms + "µs";
        }

        final long ms = TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
        return ms + "ms";
    }

    public static EnumFacing crossProduct(EnumFacing forward, EnumFacing up) {
        final int west_x = forward.getYOffset() * up.getZOffset() - forward.getZOffset() * up.getYOffset();
        final int west_y = forward.getZOffset() * up.getXOffset() - forward.getXOffset() * up.getZOffset();
        final int west_z = forward.getXOffset() * up.getYOffset() - forward.getYOffset() * up.getXOffset();

        return switch (west_x + west_y * 2 + west_z * 3) {
            case 1 -> EnumFacing.EAST;
            case -1 -> EnumFacing.WEST;
            case 2 -> EnumFacing.UP;
            case -2 -> EnumFacing.DOWN;
            case 3 -> EnumFacing.SOUTH;
            case -3 -> EnumFacing.NORTH;
            // something is better then nothing?
            default -> EnumFacing.NORTH;
        };

    }

    /**
     * @return True if client-side classes (such as Renderers) are available.
     */
    public static boolean hasClientClasses() {
        return FMLCommonHandler.instance().getSide() == Side.CLIENT;
    }

    /*
     * returns true if the code is on the client.
     */
    public static boolean isClient() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    public static boolean hasPermissions(DimensionalBlockPos dc, EntityPlayer player) {
        if (!dc.isInWorld(player.world)) {
            return false;
        }
        return player.world.isBlockModifiable(player, dc.getPos());
    }

    /*
     * Generates Item entities in the level similar to how items are generally dropped.
     */
    public static void spawnDrops(World level, BlockPos pos, List<ItemStack> drops) {
        if (!level.isRemote) {
            for (var i : drops) {
                InventoryHelper.spawnItemStack(level, pos.getX(), pos.getY(), pos.getZ(), i);
            }
        }
    }

    /*
     * returns true if the code is on the server.
     */
    public static boolean isServer() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER;
    }

    /**
     * Throws an exception if the current thread is not one of the server threads.
     */
    public static void assertServerThread() {
        if (!isServer()) {
            throw new UnsupportedOperationException(
                    "This code can only be called server-side and this is most likely a bug.");
        }
    }

    public static String formatModName(String modId) {
        return "" + TextFormatting.BLUE + TextFormatting.ITALIC + getModName(modId);
    }

    @Nullable
    public static String getModName(String modId) {
        if ("minecraft".equals(modId)) {
            return "Minecraft";
        }
        var container = Loader.instance().getIndexedModList().get(modId);
        return container != null ? container.getName() : modId;
    }

    public static ITextComponent getFluidDisplayName(Fluid fluid) {
        var fluidStack = new FluidStack(fluid, 1);
        return new TextComponentString(fluidStack.getLocalizedName());
    }

    public static boolean isChargeable(ItemStack i) {
        if (i.isEmpty()) {
            return false;
        }
        if (i.getItem() instanceof IAEItemPowerStorage powerStorage) {
            return powerStorage.getAEMaxPower(i) > 0 &&
                    powerStorage.getPowerFlow(i) != AccessRestriction.READ;
        }
        return false;
    }

    private static final UUID DEFAULT_FAKE_PLAYER_UUID = UUID.fromString("60C173A5-E1E6-4B87-85B1-272CE424521D");

    public static EntityPlayer getFakePlayer(WorldServer level, @Nullable UUID playerUuid) {
        Objects.requireNonNull(level);

        if (playerUuid == null) {
            playerUuid = DEFAULT_FAKE_PLAYER_UUID;
        }

        return FakePlayerFactory.get(level, new GameProfile(playerUuid, "[AE2]"));
    }

    public static EnumFacing rotateAround(EnumFacing forward, EnumFacing axis) {
        if (forward.getAxis() == axis.getAxis()) {
            return forward;
        }
        var newForward = forward.getDirectionVec().crossProduct(axis.getDirectionVec());
        return Objects.requireNonNull(EnumFacing.getFacingFromVector(newForward.getX(), newForward.getY(),
                newForward.getZ()));
    }

    public static void configurePlayer(EntityPlayer player, EnumFacing side, TileEntity blockEntity) {
        float pitch = 0.0f;
        float yaw = 0.0f;
        switch (side) {
            case DOWN, UP -> pitch = 90.0f;
            case EAST -> yaw = -90.0f;
            case NORTH -> yaw = 180.0f;
            case SOUTH -> yaw = 0.0f;
            case WEST -> yaw = 90.0f;
            default -> {
            }
        }

        player.setLocationAndAngles(blockEntity.getPos().getX() + 0.5, blockEntity.getPos().getY() + 0.5,
                blockEntity.getPos().getZ() + 0.5,
                yaw, pitch);
    }

    public static void notifyBlocksOfNeighbors(World level, BlockPos pos) {
        if (level != null && !level.isRemote) {
            TickHandler.instance().addCallable(level, new BlockUpdate(pos));
        }
    }

    public static boolean isSortOrderAvailable(SortOrder order) {
        return true;
    }

    /**
     * Retrieves a TileEntity from a given position, but only if that particular TileEntity would be in a state where
     * it would be ticked by the chunk.
     * <p/>
     * This method also doesn't return a block entity on the client-side.
     */
    @Nullable
    public static TileEntity getTickingBlockEntity(@Nullable World level, BlockPos pos) {
        if (!areBlockEntitiesTicking(level, pos)) {
            return null;
        }

        return level.getTileEntity(pos);
    }

    /**
     * Checks that the chunk at the given position in the given level is loaded (1.12.2 ticks every loaded tile
     * entity).
     */
    public static boolean areBlockEntitiesTicking(@Nullable World level, BlockPos pos) {
        return level instanceof WorldServer && level.isBlockLoaded(pos);
    }

    public static boolean areBlockEntitiesTicking(@Nullable World level, long chunkPos) {
        if (!(level instanceof WorldServer serverLevel)) {
            return false;
        }
        int x = (int) chunkPos;
        int z = (int) (chunkPos >> 32);
        return serverLevel.getChunkProvider().chunkExists(x, z);
    }

    /**
     * Create a full packet of the chunks data with lighting.
     */
    public static Packet<?> getFullChunkPacket(Chunk c) {
        return new SPacketChunkData(c, 65535);
    }

    public static ItemStack getInsertionRemainder(ItemStack original, long inserted) {
        if (inserted >= original.getCount()) {
            return ItemStack.EMPTY;
        } else {
            return copyStackWithSize(original, (int) (original.getCount() - inserted));
        }
    }

    public static ItemStack copyStackWithSize(ItemStack itemStack, int size) {
        if (size <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = itemStack.copy();
        copy.setCount(size);
        return copy;
    }

    /**
     * Send an update packet for the block entity at the given position to the player immediately, if they're a server
     * player.
     */
    public static void sendImmediateBlockEntityUpdate(EntityPlayer player, BlockPos pos) {
        if (player instanceof EntityPlayerMP serverPlayer) {
            var be = player.world.getTileEntity(pos);
            if (be != null) {
                var packet = be.getUpdatePacket();
                if (packet != null) {
                    serverPlayer.connection.sendPacket(packet);
                }
            }
        }
    }

    /**
     * Ponder does not exist on 1.12.2.
     */
    public static boolean isPonderLevel(World level) {
        return false;
    }

    /**
     * @return True if AE2 is being run within a dev environment.
     */
    public static boolean isDevelopmentEnvironment() {
        return (Boolean) net.minecraft.launchwrapper.Launch.blackboard.getOrDefault("fml.deobfuscatedEnvironment",
                false);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers replacing Minecraft 1.20.5+ ItemStack/NBT APIs
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Same as modern {@code ItemStack.copyWithCount}.
     */
    public static ItemStack copyWithCount(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    /**
     * Same as modern {@code ItemStack.isSameItemSameComponents}: item, metadata, NBT and capabilities must match.
     */
    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.getItem() == b.getItem()
                && a.getMetadata() == b.getMetadata()
                && ItemStack.areItemStackTagsEqual(a, b)
                && a.areCapsCompatible(b);
    }

    /**
     * Same as modern {@code ItemStack.isSameItem}: item and (for items with subtypes) metadata must match.
     */
    public static boolean isSameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && (a.isItemStackDamageable() || a.getMetadata() == b.getMetadata());
    }

    /**
     * Same as modern {@code ItemStack.hashItemAndComponents}.
     */
    public static int hashItemAndComponents(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int result = 31 + System.identityHashCode(stack.getItem());
        result = 31 * result + stack.getMetadata();
        var tag = stack.getTagCompound();
        return 31 * result + (tag == null ? 0 : tag.hashCode());
    }

    /**
     * Same as modern {@code ItemStack.getHoverName()}.
     */
    public static ITextComponent getHoverName(ItemStack stack) {
        return new TextComponentString(stack.getDisplayName());
    }

    public static Optional<NBTTagCompound> getCapsTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        var full = stack.writeToNBT(new NBTTagCompound());
        return full.hasKey("ForgeCaps") ? Optional.of(full.getCompoundTag("ForgeCaps")) : Optional.empty();
    }

    public static NBTTagCompound copyTag(NBTTagCompound tag) {
        return tag.copy();
    }

    public static long[] getLongArray(NBTTagLongArray tag) {
        return tag.data;
    }

    public static long[] getLongArray(NBTTagCompound tag, String key) {
        var element = tag.getTag(key);
        return element instanceof NBTTagLongArray arr ? arr.data : new long[0];
    }

    public static void putLongArray(NBTTagCompound tag, String key, long[] value) {
        tag.setTag(key, new NBTTagLongArray(value));
    }

    /**
     * Forge 1.12.2 fluids are only identified by their name. The modern id of a fluid is assumed to be
     * {@code minecraft:<name>} for vanilla fluids, otherwise the registering mod's namespace is used.
     */
    public static String getFluidId(Fluid fluid) {
        var name = FluidRegistry.getFluidName(fluid);
        if (name == null) {
            name = fluid.getName();
        }
        if (name.contains(":")) {
            return name;
        }
        if (fluid == FluidRegistry.WATER || fluid == FluidRegistry.LAVA) {
            return "minecraft:" + name;
        }
        var defaultName = FluidRegistry.getDefaultFluidName(fluid);
        if (defaultName != null && defaultName.contains(":")) {
            return defaultName.substring(0, defaultName.indexOf(':')) + ":" + name;
        }
        return "minecraft:" + name;
    }

    /**
     * Modern {@code ItemStack.is(Item)}.
     */
    public static boolean is(ItemStack stack, net.minecraft.item.Item item) {
        return stack.getItem() == item;
    }

    public static boolean is(ItemStack stack, appeng.api.util.ItemLike item) {
        return stack.getItem() == item.asItem();
    }

    public static boolean is(ItemStack stack, net.minecraft.block.Block block) {
        return stack.getItem() == net.minecraft.item.Item.getItemFromBlock(block);
    }

    public static boolean is(ItemStack stack, appeng.api.util.TagKey<?> tag) {
        return tag.contains(stack);
    }

    public static boolean is(ItemStack stack, java.util.function.Predicate<ItemStack> predicate) {
        return predicate.test(stack);
    }

    /**
     * Modern {@code BlockState.is(Block)}.
     */
    public static boolean is(net.minecraft.block.state.IBlockState state, net.minecraft.block.Block block) {
        return state.getBlock() == block;
    }

    public static boolean is(net.minecraft.block.state.IBlockState state, appeng.api.util.TagKey<?> tag) {
        return tag.contains(state);
    }

    public static boolean isAir(net.minecraft.block.state.IBlockState state) {
        return state.getMaterial() == net.minecraft.block.material.Material.AIR;
    }

    /**
     * Modern {@code Inventory.placeItemBackInInventory}: tries the player inventory, drops the rest.
     */
    public static void placeItemBackInInventory(EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        // addItemStackToInventory also returns true if only a part fit, the rest stays in the stack
        player.inventory.addItemStackToInventory(stack);
        if (!stack.isEmpty()) {
            player.dropItem(stack, false);
        }
    }

    public static boolean listMatches(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.areItemStacksEqual(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static int hashStackList(List<ItemStack> list) {
        int h = 0;
        for (var stack : list) {
            h = h * 31 + hashItemAndComponents(stack) * 31 + stack.getCount();
        }
        return h;
    }

    /**
     * The AE2 block entity type of the given tile entity, or null for foreign tile entities.
     */
    @Nullable
    public static appeng.core.definitions.BlockEntityType<?> getBlockEntityType(TileEntity blockEntity) {
        if (blockEntity instanceof appeng.blockentity.AEBaseBlockEntity aeBlockEntity) {
            return aeBlockEntity.getType();
        }
        return null;
    }

    public static <T> T requireNonNullElseGet(@Nullable T obj, java.util.function.Supplier<? extends T> supplier) {
        return obj != null ? obj : Objects.requireNonNull(supplier.get(), "supplier.get()");
    }

    public static <T> T requireNonNullElse(@Nullable T obj, T defaultObj) {
        return obj != null ? obj : Objects.requireNonNull(defaultObj, "defaultObj");
    }

    /**
     * Modern {@code Item.getDescription()}: the display name of the item's default stack.
     */
    public static ITextComponent getItemDescription(net.minecraft.item.Item item) {
        return new TextComponentString(new ItemStack(item).getDisplayName());
    }

    public static ITextComponent getItemDescription(appeng.api.util.ItemLike item) {
        return getItemDescription(item.asItem());
    }

    /**
     * Modern {@code BlockEntity.getBlockState()}: the actual state at the tile entity's position.
     */
    public static net.minecraft.block.state.IBlockState getBlockState(TileEntity blockEntity) {
        if (blockEntity instanceof appeng.blockentity.AEBaseBlockEntity aeBlockEntity) {
            return aeBlockEntity.getBlockState();
        }
        var level = blockEntity.getWorld();
        if (level == null) {
            return blockEntity.getBlockType() != null ? blockEntity.getBlockType().getDefaultState()
                    : net.minecraft.init.Blocks.AIR.getDefaultState();
        }
        return level.getBlockState(blockEntity.getPos());
    }

    public static Vec3d atCenterOf(Vec3i pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static Vec3d atBottomCenterOf(Vec3i pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    /**
     * Reads a private field reflectively (for the rare cases where no access transformer is warranted).
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPrivateField(Class<?> owner, Object instance, String... names) {
        for (var name : names) {
            try {
                Field f = owner.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(instance);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        AELog.warn("Unable to access field %s of %s", names[0], owner);
        return null;
    }
}
