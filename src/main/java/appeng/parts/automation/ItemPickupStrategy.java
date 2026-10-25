package appeng.parts.automation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;

import appeng.api.behaviors.PickupSink;
import appeng.api.behaviors.PickupStrategy;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.Registries;
import appeng.api.util.TagKey;
import appeng.core.AppEng;
import appeng.core.network.clientbound.BlockTransitionEffectPacket;
import appeng.core.network.clientbound.ItemTransitionEffectPacket;
import appeng.util.Platform;

public class ItemPickupStrategy implements PickupStrategy {

    public static final ResourceLocation TAG_BLACKLIST = AppEng.makeId(
            "blacklisted/annihilation_plane");

    private static final TagKey<Block> BLOCK_BLACKLIST = TagKey.create(Registries.BLOCK, TAG_BLACKLIST);

    private static final TagKey<Item> ITEM_BLACKLIST = TagKey.create(Registries.ITEM, TAG_BLACKLIST);

    private final WorldServer level;
    private final BlockPos pos;
    private final EnumFacing side;
    private final Map<Enchantment, Integer> enchantments;
    @Nullable
    private final UUID ownerUuid;

    private boolean isAccepting = true;

    public ItemPickupStrategy(WorldServer level, BlockPos pos, EnumFacing side, TileEntity host,
            Map<Enchantment, Integer> enchantments, @Nullable UUID owningPlayerId) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.enchantments = enchantments;
        this.ownerUuid = owningPlayerId;
    }

    @Override
    public void reset() {
        this.isAccepting = true;
    }

    public boolean canPickUpEntity(Entity entity) {
        return entity instanceof EntityItem;
    }

    public boolean pickUpEntity(IEnergySource energySource, PickupSink sink, Entity entity) {
        if (!this.isAccepting || !(entity instanceof EntityItem itemEntity)) {
            return false;
        }

        if (isItemBlacklisted(itemEntity.getItem().getItem())) {
            return false;
        }

        var changed = this.storeEntityItem(sink, itemEntity);

        if (changed) {
            AppEng.instance().sendToAllNearExcept(null, pos.getX(), pos.getY(), pos.getZ(), 64,
                    level, new ItemTransitionEffectPacket(entity.posX,
                            entity.posY, entity.posZ, side));
        }

        return true;
    }

    @Override
    public Result tryPickup(IEnergySource energySource, PickupSink sink) {
        if (this.isAccepting) {
            var blockState = level.getBlockState(pos);
            if (this.canHandleBlock(level, pos, blockState)) {
                // Query the loot-table and get a potential outcome of the loot-table evaluation
                var items = this.obtainBlockDrops(level, pos);
                var requiredPower = this.calculateEnergyUsage(level, pos, items);

                var hasPower = energySource.extractAEPower(requiredPower, Actionable.SIMULATE,
                        PowerMultiplier.CONFIG) > requiredPower - 0.1;
                var canStore = this.canStoreItemStacks(sink, items);

                if (hasPower && canStore) {
                    this.completePickup(energySource, sink, items, requiredPower, blockState);
                    return Result.PICKED_UP;
                } else {
                    return Result.CANT_STORE;
                }
            }
        }

        return Result.CANT_PICKUP;
    }

    private void completePickup(IEnergySource energySource, PickupSink sink, List<ItemStack> items, float requiredPower,
            IBlockState blockState) {
        if (!this.breakBlockAndStoreExtraItems(sink, level, pos)) {
            // We failed to actually replace the block with air, or it already was the case
            return;
        }

        for (var item : items) {
            var inserted = storeItemStack(sink, item);
            // If inserting the item fully was not possible, drop it as an item entity instead if the storage clears up,
            // we'll pick it up that way
            // This will mainly be the case with storages like a compacting drawer (where two inserts can simulate
            // correctly but only one will succeed)
            if (inserted < item.getCount()) {
                item.shrink(inserted);
                Platform.spawnDrops(level, pos, Collections.singletonList(item));
            }
        }

        energySource.extractAEPower(requiredPower, Actionable.MODULATE, PowerMultiplier.CONFIG);

        AppEng.instance().sendToAllNearExcept(null, pos.getX(), pos.getY(), pos.getZ(), 64, level,
                new BlockTransitionEffectPacket(pos, blockState, side, BlockTransitionEffectPacket.SoundMode.NONE));
    }

    /**
     * Stores an {@link EntityItem} inside the network and either marks it as dead or sets it to the leftover stackSize.
     *
     * @param entityItem {@link EntityItem} to store
     */
    private boolean storeEntityItem(PickupSink sink, EntityItem entityItem) {
        if (entityItem.isEntityAlive()) {
            var inserted = this.storeItemStack(sink, entityItem.getItem());

            return this.handleOverflow(entityItem, inserted);
        }

        return false;
    }

    /**
     * Stores an {@link ItemStack} inside the network.
     *
     * @param item {@link ItemStack} to store
     * @return count inserted
     */
    private int storeItemStack(PickupSink sink, ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }

        var what = AEItemKey.of(item);
        var amount = item.getCount();
        var inserted = (int) sink.insert(what, amount, Actionable.MODULATE);

        this.isAccepting = inserted >= amount;

        return inserted;
    }

    /**
     * Handles a possible overflow or none at all. It will update the entity to match the leftover stack size as well as
     * mark it as dead without any leftover amount.
     *
     * @param entityItem the entity to update or destroy
     * @param inserted   amount inserted
     * @return true, if the entity was changed otherwise false.
     */
    private boolean handleOverflow(EntityItem entityItem, int inserted) {
        int entityItemCount = entityItem.getItem().getCount();
        if (inserted >= entityItemCount) {
            entityItem.setDead();
            return true;
        }

        var newStackSize = entityItemCount - inserted;
        var changed = entityItemCount != newStackSize;

        entityItem.getItem().setCount(newStackSize);

        return changed;
    }

    /**
     * Checks if this plane can handle the block at the specific coordinates.
     */
    private boolean canHandleBlock(WorldServer level, BlockPos pos, IBlockState state) {
        if (Platform.isAir(state)) {
            return false;
        }

        if (isBlockBlacklisted(state.getBlock())) {
            return false;
        }

        // Note: bedrock, portals, and other unbreakable blocks have a hardness < 0, hence the >= 0 check below.
        var hardness = state.getBlockHardness(level, pos);
        var ignoreAirAndFluids = Platform.isAir(state) || state.getMaterial().isLiquid();

        return !ignoreAirAndFluids && hardness >= 0f && level.isBlockLoaded(pos)
                && level.isBlockModifiable(Platform.getFakePlayer(level, ownerUuid), pos);
    }

    protected List<ItemStack> obtainBlockDrops(WorldServer level, BlockPos pos) {
        var fakePlayer = Platform.getFakePlayer(level, ownerUuid);
        var state = level.getBlockState(pos);
        var block = state.getBlock();

        var harvestTool = createHarvestTool(state);
        var harvestToolItem = harvestTool.item();

        if (state.getMaterial().isToolNotRequired() && harvestTool.fallback()) {
            // Do not use a tool when not required, no hints about it and not enchanted in cases like silk touch.
            harvestToolItem = ItemStack.EMPTY;
        }

        // Drops of this version only look at fortune and silk touch, the tool is held for blocks that check it
        fakePlayer.setHeldItem(EnumHand.MAIN_HAND, harvestToolItem);
        try {
            var fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, harvestToolItem);
            var silkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, harvestToolItem) > 0
                    && block.canSilkHarvest(level, pos, state, fakePlayer);

            NonNullList<ItemStack> drops = NonNullList.create();
            if (silkTouch) {
                drops.add(getSilkTouchDrop(block, state));
            } else {
                block.getDrops(drops, level, pos, state, fortune);
            }
            var chance = ForgeEventFactory.fireBlockHarvesting(drops, level, pos, state, fortune, 1.0f, silkTouch,
                    fakePlayer);

            // Some modded blocks can have empty stacks, filter them out!
            return drops.stream()
                    .filter(stack -> !stack.isEmpty() && level.rand.nextFloat() <= chance)
                    .collect(Collectors.toList());
        } finally {
            fakePlayer.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    /**
     * Same as the (protected) silk touch drop of the game: the block item with the metadata of the state.
     */
    private static ItemStack getSilkTouchDrop(Block block, IBlockState state) {
        var item = Item.getItemFromBlock(block);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        var meta = item.getHasSubtypes() ? block.getMetaFromState(state) : 0;
        return new ItemStack(item, 1, meta);
    }

    /**
     * Checks if this plane can handle the block at the specific coordinates.
     */
    protected float calculateEnergyUsage(WorldServer level, BlockPos pos, List<ItemStack> items) {
        boolean useEnergy = true;

        var state = level.getBlockState(pos);
        var hardness = state.getBlockHardness(level, pos);

        var requiredEnergy = 1 + hardness;
        for (var is : items) {
            requiredEnergy += is.getCount();
        }

        if (enchantments != null) {
            var efficiencyFactor = 1f;
            int efficiencyLevel = enchantments.getOrDefault(Enchantments.EFFICIENCY, 0);
            if (efficiencyLevel > 0) {
                // Reduce total energy usage incurred by other enchantments by 15% per Efficiency level.
                efficiencyFactor *= Math.pow(0.85, efficiencyLevel);
            }
            int unbreakingLevel = enchantments.getOrDefault(Enchantments.UNBREAKING, 0);
            if (unbreakingLevel > 0) {
                // Give plane only a (100 / (level + 1))% chance to use energy.
                // This is similar to vanilla Unbreaking behaviour for tools.
                int randomNumber = level.rand.nextInt(unbreakingLevel + 1);
                useEnergy = randomNumber == 0;
            }
            // Increase energy usage 8x for each *other* enchantment that might affect the pickup.
            // Ignore efficiency & unbreaking since we did account for them here explicitly.
            var levelSum = enchantments.entrySet().stream().map(Map.Entry::getValue).reduce(0, Integer::sum)
                    - efficiencyLevel - unbreakingLevel;
            requiredEnergy *= (levelSum > 0 ? 8 * levelSum : 1) * efficiencyFactor;
        }

        return useEnergy ? requiredEnergy : 0;
    }

    /**
     * Checks if the network can store the drops.
     * <p>
     * It also sets isAccepting to false, if the item can not be stored.
     *
     * @param itemStacks an array of {@link ItemStack} to test
     * @return true, if the network can store all drops or no drops are reported
     */
    private boolean canStoreItemStacks(PickupSink sink, List<ItemStack> itemStacks) {
        var canStore = itemStacks.isEmpty();

        for (var itemStack : itemStacks) {
            var itemToTest = AEItemKey.of(itemStack);
            var inserted = sink.insert(itemToTest, itemStack.getCount(), Actionable.SIMULATE);
            if (inserted == itemStack.getCount()) {
                canStore = true;
            }
        }

        this.isAccepting = canStore;
        return canStore;
    }

    private boolean breakBlockAndStoreExtraItems(PickupSink sink, WorldServer level, BlockPos pos) {
        // Kill the block, but signal no drops
        if (!level.destroyBlock(pos, false)) {
            // The block was no longer there
            return false;
        }

        // This handles items that do not spawn via loot-tables but rather normal block breaking i.e. our cable-buses do
        // this (bad practice, really)
        var box = new AxisAlignedBB(pos).grow(0.2);
        for (var itemEntity : level.getEntitiesWithinAABB(EntityItem.class, box)) {
            this.storeEntityItem(sink, itemEntity);
        }
        return true;
    }

    /**
     * Creates the fake (and temporary) tool based on the provided hints in case a loot table relies on it.
     * <p>
     * Generally could use a stick as tool or anything else which can be enchanted. {@link ItemStack#EMPTY} is not an
     * option as at least anything having a fortune effect need something enchantable even without the enchantment,
     * otherwise it will not drop anything.
     *
     * @param state The block state of the block about to be broken.
     */
    private HarvestTool createHarvestTool(IBlockState state) {
        ItemStack tool;
        boolean fallback = false;

        // The tool class the block asks for (there are no "mineable" tags in this version)
        var toolClass = state.getBlock().getHarvestTool(state);
        if ("pickaxe".equals(toolClass)) {
            tool = new ItemStack(Items.DIAMOND_PICKAXE, 1);
        } else if ("axe".equals(toolClass)) {
            tool = new ItemStack(Items.DIAMOND_AXE, 1);
        } else if ("shovel".equals(toolClass)) {
            tool = new ItemStack(Items.DIAMOND_SHOVEL, 1);
        } else if ("hoe".equals(toolClass)) {
            tool = new ItemStack(Items.DIAMOND_HOE, 1);
        } else {
            // Use a pickaxe for everything else, to allow enchanting it
            tool = new ItemStack(Items.DIAMOND_PICKAXE, 1);
            fallback = true;
        }

        if (!enchantments.isEmpty()) {
            // For silk touch / fortune purposes, enchant the fake tool
            EnchantmentHelper.setEnchantments(enchantments, tool);

            // Setting fallback to false ensures it'll be used even if not strictly required
            fallback = false;
        }

        return new HarvestTool(tool, fallback);
    }

    public static boolean isBlockBlacklisted(Block b) {
        return BLOCK_BLACKLIST.contains(b);
    }

    public static boolean isItemBlacklisted(Item i) {
        return ITEM_BLACKLIST.contains(i);
    }

    final static class HarvestTool {
        private final ItemStack item;
        private final boolean fallback;

        public HarvestTool(ItemStack item, boolean fallback) {
            this.item = item;
            this.fallback = fallback;
        }

        public ItemStack item() {
            return item;
        }

        public boolean fallback() {
            return fallback;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HarvestTool)) return false;
            HarvestTool that = (HarvestTool) o;
            return java.util.Objects.equals(item, that.item)
                    && fallback == that.fallback;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(item, fallback);
        }

        @Override
        public String toString() {
            return "HarvestTool[" + "item=" + item + ", " + "fallback=" + fallback + "]";
        }

    }

}
