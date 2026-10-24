/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.parts;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.crash.CrashReportCategory;
import appeng.api.components.DataComponentMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import appeng.api.util.Clearable;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.client.render.PoseStack;
import appeng.client.render.model.ModelData;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.util.SettingsFrom;

public interface IPart extends ICustomCableConnection, Clearable {

    /**
     * Gets the item from which this part was created. Will be used to save and load this part from NBT Data or to
     * Packets when synchronizing it with the client. It will also be used to drop the part when it is dismantled or
     * broken.
     *
     * @return The item from which this part was placed.
     */
    IPartItem<?> getPartItem();

    /**
     * Render dynamic portions of this part, as part of the cable bus TESR. This part has to return true for
     * {@link #requireDynamicRender()} in order for this method to be called.
     */
    @SideOnly(Side.CLIENT)
    default void renderDynamic(float partialTicks, PoseStack poseStack, int combinedLightIn) {
    }

    /**
     * return true only if your part require dynamic rendering, must be consistent.
     *
     * @return true to enable {@link #renderDynamic}
     */
    default boolean requireDynamicRender() {
        return false;
    }

    /**
     * @return if the bus has a solid side, and you can place random stuff on it like torches or levers
     */
    default boolean isSolid() {
        return false;
    }

    /**
     * @return true if this part can connect to redstone
     */
    default boolean canConnectRedstone() {
        return false;
    }

    /**
     * Write the part information for saving. This information will be saved alongside the {@link #getPartItem()} to
     * save settings, inventory or other values to the world.
     *
     * @param data       to be written nbt data
     * @param registries
     */
    default void writeToNBT(NBTTagCompound data) {
    }

    /**
     * Read the previously written NBT Data. this is the mirror for {@link #writeToNBT}.
     *
     * @param data       to be read nbt data
     * @param registries
     */
    default void readFromNBT(NBTTagCompound data) {
    }

    /**
     * Exports settings for attaching it to a memory card or item stack.
     *
     * @param mode    The purpose to export settings for.
     * @param builder The map to write the settings to. Written settings must be part of the exported settings tag.
     */
    default void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder) {
    }

    /**
     * Depending on the mode, different settings will be accepted.
     *
     * @param input  source of settings
     * @param player the (optional) player who is importing the settings
     */
    default void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable EntityPlayer player) {
    }

    /**
     * @return get the amount of light produced by the bus
     */
    default int getLightLevel() {
        return 0;
    }

    /**
     * does this part act like a ladder?
     *
     * @param entity climbing entity
     * @return true if entity can climb
     */
    default boolean isLadder(EntityLivingBase entity) {
        return false;
    }

    /**
     * a block around the bus's host has been changed.
     * 
     * @see net.neoforged.neoforge.common.extensions.IBlockExtension#onNeighborChange
     */
    default void onNeighborChanged(IBlockAccess level, BlockPos pos, BlockPos neighbor) {
    }

    /**
     * The block state in a block adjacent to the part host may have changed. Note that this may be called quite often
     * and should not lead to immediate block notifications. Any action resulting from this notification should be
     * delayed until at least the end of tick.
     * <p/>
     * It is the parts responsibility to only react to changes on sides that are relevant for it.
     *
     * @see net.minecraft.world.level.block.Block#updateShape
     */
    default void onUpdateShape(EnumFacing side) {
    }

    /**
     * @return output redstone on facing side
     */
    default int isProvidingStrongPower() {
        return 0;
    }

    /**
     * @return output redstone on facing side
     */
    default int isProvidingWeakPower() {
        return 0;
    }

    /**
     * Write variable data that should be synchronized to clients to the synchronization packet.
     *
     * @param data to be written data
     */
    default void writeToStream(RegistryFriendlyByteBuf data) {
    }

    /**
     * Used to store the state that is synchronized to clients for the visual appearance of this part as NBT. This is
     * only used to store this state for tools such as Create Ponders in Structure NBT. Actual synchronization uses
     * {@link #writeToStream(RegistryFriendlyByteBuf)} and {@link #readFromStream(RegistryFriendlyByteBuf)}. Any data
     * that is saved to the NBT tag in {@link #writeToNBT(NBTTagCompound, HolderLookup.Provider)} does not need to be saved
     * here again.
     * <p>
     * The data saved should be equivalent to the data sent to the client in {@link #writeToStream}.
     * <p>
     * Please note that this may both be called on the client-side (i.e. when using Create Ponder) and on the
     * server-side when saving structures with a structure block. To not lose the previously saved data in PonderJS, you
     * need to write back the data you read in {@link #readVisualStateFromNBT} if the current level is a client-side
     * level.
     */
    @ApiStatus.Experimental
    default void writeVisualStateToNBT(NBTTagCompound data) {
    }

    /**
     * read data from bus packet.
     *
     * @param data to be read data
     * @return true will re-draw the part.
     */
    default boolean readFromStream(RegistryFriendlyByteBuf data) {
        return false;
    }

    /**
     * Used to store the state that is synchronized to clients for the visual appearance of this part as NBT. This is
     * only used to store this state for tools such as Create Ponders in Structure NBT. Actual synchronization uses
     * {@link #writeToStream(RegistryFriendlyByteBuf)} and {@link #readFromStream(RegistryFriendlyByteBuf)}. Any data
     * that is saved to the NBT tag in {@link #writeToNBT(NBTTagCompound, HolderLookup.Provider)} already does not need to
     * be saved here again.
     */
    @ApiStatus.Experimental
    default void readVisualStateFromNBT(NBTTagCompound data) {
    }

    /**
     * get the Grid Node for the Bus, be sure your IGridBlock is NOT isWorldAccessible, if it is your going to cause
     * crashes.
     * <p>
     * or null if you don't have a grid node.
     *
     * @return grid node
     */
    @Nullable
    IGridNode getGridNode();

    /**
     * called when an entity collides with the bus.
     *
     * @param entity colliding entity
     */
    default void onEntityCollision(Entity entity) {
    }

    /**
     * called when your part is being removed from the world.
     */
    default void removeFromWorld() {
    }

    /**
     * called when your part is being added to the world.
     */
    default void addToWorld() {
    }

    /**
     * used for tunnels.
     *
     * @return a grid node that represents the external facing side and allows external connections. this nodes
     *         {@link IManagedGridNode#setExposedOnSides(Set)} will be automatically updated with the side the part is
     *         placed on.
     */
    @Nullable
    default IGridNode getExternalFacingNode() {
        return null;
    }

    /**
     * This method controls the cable type that is returned for
     * {@link appeng.api.networking.IInWorldGridNodeHost#getCableConnectionType(EnumFacing)} by the part host for the
     * side this part is on.
     */
    default AECableType getExternalCableConnectionType() {
        return AECableType.GLASS;
    }

    /**
     * called by the Part host to keep your part informed.
     *
     * @param side        The side the part is attached to, or null to indicate the part is at the center.
     * @param blockEntity block entity of part
     */
    void setPartHostInfo(@Nullable EnumFacing side, IPartHost host, TileEntity blockEntity);

    /**
     * Called when a player right-clicks the part without an item in hand.
     *
     * @param player right clicking player
     * @param pos    position of block
     * @return if your activate method performed something.
     */
    default boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        return false;
    }

    /**
     * Called when a player right-clicks the part with an item in hand.
     *
     * @param heldItem item triggering the interaction
     * @param player   right clicking player
     * @param hand     hand used
     * @param pos      position of block
     * @return if your activate method performed something.
     */
    default boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d pos) {
        return false;
    }

    /**
     * Called when you left click the part, very similar to Block.onBlockClicked a
     *
     * @param player left clicking player
     * @param pos    clicked position, in block-local coordinates
     * @return True if your part wants to suppress the default behavior of attacking the part host.
     */
    default boolean onClicked(EntityPlayer player, Vec3d pos) {
        return false;
    }

    /**
     * Called when you shift-left click the part, very similar to Block.onBlockClicked
     *
     * @param player shift-left clicking player
     * @param pos    clicked position, in block-local coordinates
     * @return True if your part wants to suppress the default behavior of attacking the part host.
     */
    default boolean onShiftClicked(EntityPlayer player, Vec3d pos) {
        return false;
    }

    /**
     * Add the stack of the part itself to the drop list.
     *
     * @param wrenched control flag for wrenched vs broken
     */
    default void addPartDrop(List<ItemStack> drops, boolean wrenched) {
        var stack = new ItemStack(getPartItem().asItem());
        var builder = DataComponentMap.builder();
        exportSettings(SettingsFrom.DISMANTLE_ITEM, builder);
        builder.build().applyTo(stack);
        drops.add(stack);
    }

    /**
     * Add additional drops to the drop list (the contents of the part, but not the part itself).
     *
     * @param wrenched control flag for wrenched vs broken
     */
    @MustBeInvokedByOverriders
    default void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
    }

    /**
     * Clears the contents of the part, which would otherwise be dropped by {@link #addAdditionalDrops}.
     */
    @Override
    @MustBeInvokedByOverriders
    default void clearContent() {
    }

    /**
     * @return 0 - 8, reasonable default 3-4, this controls the cable connection to the node. -1 to render connection
     *         yourself.
     */
    @Override
    float getCableConnectionLength(AECableType cable);

    /**
     * same as Block.animateTick, for but parts.
     *
     * @param level level of block
     * @param pos   location of block
     * @param r     random
     */
    default void animateTick(World level, BlockPos pos, Random r) {
    }

    /**
     * Called when placed in the world by a player, this happens before addWorld.
     * 
     * @param player placing player
     *
     */
    default void onPlacement(EntityPlayer player) {
    }

    /**
     * Used to determine which parts can be placed on what cables.
     * <p>
     * Dense cables are not allowed for functional (getGridNode returns a node) parts. Doing so will result in crashes.
     *
     * @param what placed part
     * @return true if the part can be placed on this support.
     */
    default boolean canBePlacedOn(BusSupport what) {
        return what == BusSupport.CABLE;
    }

    /**
     * This method is used when a chunk is rebuilt to determine how this part should be rendered. The returned models
     * should represent the part oriented north. They will be automatically rotated to match the part's actual
     * orientation. Tint indices 1-4 can be used in the models to access the parts color.
     *
     * <dl>
     * <dt>Tint Index 1</dt>
     * <dd>The {@link AEColor#blackVariant dark variant color} of the cable that this part is attached to.</dd>
     * <dt>Tint Index 2</dt>
     * <dd>The {@link AEColor#mediumVariant color} of the cable that this part is attached to.</dd>
     * <dt>Tint Index 3</dt>
     * <dd>The {@link AEColor#whiteVariant bright variant color} of the cable that this part is attached to.</dd>
     * <dt>Tint Index 4</dt>
     * <dd>A color variant that is between the cable's {@link AEColor#mediumVariant color} and its
     * {@link AEColor#whiteVariant bright variant}.</dd>
     * </dl>
     *
     * <b>Important:</b> All models must have been registered via the {@link PartModels} API before use.
     */
    default IPartModel getStaticModels() {
        return new IPartModel() {
        };
    }

    /**
     * Additional model data to be passed to the models for rendering this part.
     *
     * @return The model data to pass to the model. Only useful if custom models are used.
     */
    default ModelData getModelData() {
        return ModelData.EMPTY;
    }

    /**
     * add your collision information to the list.
     *
     * @param bch collision boxes
     */
    void getBoxes(IPartCollisionHelper bch);

    /**
     * This will be used by the core to add information about this part to a crash report if it is attached to a host
     * that caused a crash during tick processing.
     *
     * @param section The crash report section the information will be added to.
     */
    default void addEntityCrashInfo(CrashReportCategory section) {
    }

    /**
     * This method may be implemented by a part to request a specific type of cable connection for rendering. Mechanics
     * are not affected by this in any way.
     */
    default AECableType getDesiredConnectionType() {
        return AECableType.GLASS;
    }
}
