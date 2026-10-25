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

package appeng.parts.automation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import appeng.api.components.DataComponentMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;

import appeng.api.behaviors.PickupStrategy;
import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import appeng.client.render.model.ModelData;
import appeng.core.AEConfig;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.AEBasePart;
import appeng.util.NbtOps;
import appeng.util.SettingsFrom;

public class AnnihilationPlanePart extends AEBasePart implements IGridTickable {

    private static final Logger LOG = LogManager.getLogger(AnnihilationPlanePart.class);

    private static final PlaneModels MODELS = new PlaneModels("part/annihilation_plane",
            "part/annihilation_plane_on");

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    private final IActionSource actionSource = new MachineSource(this);

    private final PlaneConnectionHelper connectionHelper = new PlaneConnectionHelper(this);

    @Nullable
    protected List<PickupStrategy> pickupStrategies;

    /**
     * Enchantments found on the plane when it was placed will be used to enchant the fake tool used for picking up
     * blocks.
     */
    private Map<Enchantment, Integer> enchantments = Collections.emptyMap();

    // Allows annihilation planes to stop pickup and instead go into a continuous generation mode
    private ContinuousGeneration continuousGeneration;
    private int continuousGenerationTicks;

    public AnnihilationPlanePart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IGridTickable.class, this);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();

        var host = getTileEntity();
        var buildHeight = host.getWorld().getHeight();

        continuousGenerationTicks = 0;
        continuousGeneration = null;
        // When placed at max build height facing up, continuously generate 1 sky stone dust / 10 seconds
        if (AEConfig.instance().isAnnihilationPlaneSkyDustGenerationEnabled()
                && host.getPos().getY() + 1 >= buildHeight && getSide() == EnumFacing.UP) {
            continuousGeneration = new ContinuousGeneration(
                    AEItemKey.of(AEItems.SKY_DUST),
                    1,
                    200);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);

        // Stored like the enchantments of an item stack (an "ench" list)
        var enchantmentsTag = data.getCompoundTag("enchantments");
        var loaded = AEComponents.ENCHANTMENTS.get(enchantmentsTag);
        this.enchantments = loaded != null ? loaded : Collections.emptyMap();
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);

        if (!enchantments.isEmpty()) {
            var enchantmentsTag = new NBTTagCompound();
            AEComponents.ENCHANTMENTS.set(enchantmentsTag, enchantments);
            data.setTag("enchantments", enchantmentsTag);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap data, @Nullable EntityPlayer player) {
        super.importSettings(mode, data, player);
        // Import enchants only when the plan is placed, not from memory cards
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var imported = data.get(AEComponents.ENCHANTMENTS);
            this.enchantments = imported != null ? new LinkedHashMap<>(imported) : Collections.emptyMap();
        }
        pickupStrategies = null;
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder data) {
        super.exportSettings(mode, data);
        // Save enchants only when the actual plane is dismantled
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            data.set(AEComponents.ENCHANTMENTS, enchantments.isEmpty() ? null : enchantments);
        }
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    protected List<PickupStrategy> getPickupStrategies() {
        if (pickupStrategies == null) {
            // Don't initialize if the node is not initialized yet
            var node = getMainNode().getNode();
            if (node == null) {
                return Collections.emptyList();
            }
            var self = this.getHost().getTileEntity();
            var pos = self.getPos().offset(this.getSide());
            var side = getSide().getOpposite();
            var owner = node.getOwningPlayerProfileId();
            pickupStrategies = StackWorldBehaviors.createPickupStrategies((WorldServer) self.getWorld(),
                    pos, side, self, enchantments, owner);
        }
        return pickupStrategies;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {

        // For collision, we're using a simplified bounding box
        if (bch.isBBCollision()) {
            // The smaller collision hitbox here is needed to allow for the entity collision event
            bch.addBox(0, 0, 14, 16, 16, 15.5);
            return;
        }

        connectionHelper.getBoxes(bch);

    }

    /**
     * @return An object describing which adjacent planes this plane connects to visually.
     */
    public PlaneConnections getConnections() {
        return connectionHelper.getConnections();
    }

    @Override
    public void onNeighborChanged(IBlockAccess level, BlockPos pos, BlockPos neighbor) {
        if (pos.offset(this.getSide()).equals(neighbor)) {
            if (!isClientSide()) {
                this.refresh();
            }
        }
    }

    @Override
    public void onUpdateShape(EnumFacing side) {
        var ourSide = getSide();
        // A block might have been placed in front of us
        if (side.equals(ourSide)) {
            if (!isClientSide()) {
                this.refresh();
            }
        } else if (ourSide.getAxis() != side.getAxis()) {
            // Changes perpendicular to our side may change the connected plane model to change
            connectionHelper.updateConnections();
        }
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (!entity.isEntityAlive() || isClientSide() || !this.getMainNode().isActive()) {
            return;
        }

        var grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        PickupStrategy strategy = null;
        for (PickupStrategy pickupStrategy : getPickupStrategies()) {
            if (pickupStrategy.canPickUpEntity(entity)) {
                strategy = pickupStrategy;
                break;
            }
        }
        if (strategy == null) {
            return;
        }

        var pos = getHost().getTileEntity().getPos();
        var planePosX = pos.getX();
        var planePosY = pos.getY();
        var planePosZ = pos.getZ();

        // This is the middle point of the entities BB, which is better suited for comparisons
        // that don't rely on it "touching" the plane
        var posYMiddle = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0D;
        var entityPosX = entity.posX;
        var entityPosY = entity.posY;
        var entityPosZ = entity.posZ;

        var captureX = entityPosX > planePosX && entityPosX < planePosX + 1;
        var captureY = posYMiddle > planePosY && posYMiddle < planePosY + 1;
        var captureZ = entityPosZ > planePosZ && entityPosZ < planePosZ + 1;

        var capture = switch (getSide()) {
            case DOWN -> captureX && captureZ && entityPosY < planePosY + 0.1;
            case UP -> captureX && captureZ && entityPosY > planePosY + 0.9;
            case SOUTH -> captureX && captureY && entityPosZ > planePosZ + 0.9;
            case NORTH -> captureX && captureY && entityPosZ < planePosZ + 0.1;
            case EAST -> captureZ && captureY && entityPosX > planePosX + 0.9;
            case WEST -> captureZ && captureY && entityPosX < planePosX + 0.1;
        };

        if (capture) {
            if (!strategy.pickUpEntity(grid.getEnergyService(), this::insertIntoGrid, entity)) {
                // we need to wake up the block entity in case an entity pickup fails
                // to reset the "blocked" flags internal to the pickup strategy.
                getMainNode().ifPresent((g, n) -> g.getTickManager().alertDevice(n));
            }
        }
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);

        if (getMainNode().hasGridBooted()) {
            this.refresh();
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.AnnihilationPlane, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!isActive()) {
            return TickRateModulation.SLEEP;
        }

        var grid = node.getGrid();

        if (continuousGeneration != null) {
            continuousGenerationTicks += ticksSinceLastCall;
            if (continuousGenerationTicks >= continuousGeneration.ticks) {
                long amount = continuousGenerationTicks / continuousGeneration.ticks;
                insertIntoGrid(continuousGeneration.what, amount, Actionable.MODULATE);
                continuousGenerationTicks -= amount * continuousGeneration.ticks;
            }
            return TickRateModulation.IDLE;
        }

        // Reset to allow more entity pickups
        for (var pickupStrategy : getPickupStrategies()) {
            pickupStrategy.reset();
        }

        for (PickupStrategy pickupStrategy : getPickupStrategies()) {
            var pickupResult = pickupStrategy.tryPickup(grid.getEnergyService(), this::insertIntoGrid);

            if (pickupResult == PickupStrategy.Result.PICKED_UP) {
                return TickRateModulation.URGENT;
            } else if (pickupResult == PickupStrategy.Result.CANT_STORE) {
                // If there's a compatible block, but we can't store it, wait longer
                return TickRateModulation.IDLE;
            }
        }

        return TickRateModulation.SLEEP;
    }

    private void refresh() {
        for (var pickupStrategy : getPickupStrategies()) {
            pickupStrategy.reset();
        }

        getMainNode().ifPresent((g, n) -> g.getTickManager().alertDevice(n));
    }

    private long insertIntoGrid(AEKey what, long amount, Actionable mode) {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        return StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(),
                what, amount, this.actionSource, mode);
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(PlaneModelData.CONNECTIONS, getConnections())
                .build();
    }

    private final static class ContinuousGeneration {
        private final AEKey what;
        private final long amount;
        private final int ticks;

        public ContinuousGeneration(AEKey what, long amount, int ticks) {
            this.what = what;
            this.amount = amount;
            this.ticks = ticks;
        }

        public AEKey what() {
            return what;
        }

        public long amount() {
            return amount;
        }

        public int ticks() {
            return ticks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ContinuousGeneration)) return false;
            ContinuousGeneration that = (ContinuousGeneration) o;
            return java.util.Objects.equals(what, that.what)
                    && amount == that.amount
                    && ticks == that.ticks;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(what, amount, ticks);
        }

        @Override
        public String toString() {
            return "ContinuousGeneration[" + "what=" + what + ", " + "amount=" + amount + ", " + "ticks=" + ticks + "]";
        }

    }
}
