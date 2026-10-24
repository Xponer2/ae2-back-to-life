package appeng.api.stacks;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import appeng.api.components.DataComponentType;
import appeng.api.storage.AEKeyFilter;
import appeng.api.util.ItemLike;
import appeng.api.util.TagKey;
import appeng.core.AELog;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.util.AECodecs;
import appeng.util.BuiltInRegistries;
import appeng.util.DataResults;
import appeng.util.NbtOps;
import appeng.util.Platform;

/**
 * An item key on 1.12.2 is the combination of item, metadata, NBT and capability NBT (wrapped in a stack of size 1).
 * The metadata is part of the primary key for items with subtypes (i.e. dye colors are different "items" like on
 * modern Minecraft), while for damageable items it is treated as damage (fuzzy searchable, secondary).
 */
public final class AEItemKey extends AEKey {

    public static final MapCodec<AEItemKey> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    AECodecs.ITEM.comapFlatMap(
                            item -> item == Items.AIR
                                    ? DataResult.<Item>error("Item must not be minecraft:air")
                                    : DataResult.success(item),
                            item -> item)
                            .fieldOf("id").forGetter(key -> key.stack.getItem()),
                    Codec.INT.optionalFieldOf("damage", 0).forGetter(key -> key.stack.getMetadata()),
                    AECodecs.COMPOUND_TAG.optionalFieldOf("tag")
                            .forGetter(key -> Optional.ofNullable(key.stack.getTagCompound())),
                    AECodecs.COMPOUND_TAG.optionalFieldOf("ForgeCaps").forGetter(AEItemKey::getCapsTag))
                    .apply(builder, (item, damage, tag, caps) -> {
                        var stack = new ItemStack(item, 1, damage, caps.orElse(null));
                        tag.ifPresent(t -> stack.setTagCompound(t.copy()));
                        return new AEItemKey(stack);
                    }));
    public static final Codec<AEItemKey> CODEC = MAP_CODEC.codec();

    /**
     * Canonical primary keys for (item, metadata) of items with subtypes, they have to be compared by identity.
     */
    private static final ConcurrentHashMap<Long, ItemVariant> VARIANTS = new ConcurrentHashMap<>();

    private final ItemStack stack;
    private final int hashCode;
    private final int maxStackSize;
    private final int damage;
    private final Object primaryKey;

    private AEItemKey(ItemStack stack) {
        Preconditions.checkArgument(!stack.isEmpty(), "stack is empty");
        if (stack.getCount() != 1) {
            stack.setCount(1);
        }
        this.stack = stack;
        this.hashCode = Platform.hashItemAndComponents(stack);
        this.maxStackSize = stack.getMaxStackSize();
        this.damage = stack.isItemStackDamageable() ? stack.getItemDamage() : 0;
        this.primaryKey = computePrimaryKey(stack);
    }

    private static Object computePrimaryKey(ItemStack stack) {
        var item = stack.getItem();
        if (item.getHasSubtypes() && !stack.isItemStackDamageable()) {
            int meta = stack.getMetadata();
            long key = ((long) Item.getIdFromItem(item) << 32) | (meta & 0xFFFFFFFFL);
            return VARIANTS.computeIfAbsent(key, k -> new ItemVariant(item, meta));
        }
        return item;
    }

    /**
     * Identity-compared primary key for an item with subtypes.
     */
    public static final class ItemVariant {
        private final Item item;
        private final int meta;

        private ItemVariant(Item item, int meta) {
            this.item = item;
            this.meta = meta;
        }

        public Item item() {
            return item;
        }

        public int meta() {
            return meta;
        }

        @Override
        public String toString() {
            return item.getRegistryName() + "@" + meta;
        }
    }

    @Nullable
    public static AEItemKey of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return new AEItemKey(Platform.copyWithCount(stack, 1));
    }

    public static boolean matches(AEKey what, ItemStack itemStack) {
        return what instanceof AEItemKey itemKey && itemKey.matches(itemStack);
    }

    public static boolean is(AEKey what) {
        return what instanceof AEItemKey;
    }

    public static AEKeyFilter filter() {
        return AEItemKey::is;
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.items();
    }

    @Override
    public AEItemKey dropSecondary() {
        var item = stack.getItem();
        int meta = item.getHasSubtypes() && !stack.isItemStackDamageable() ? stack.getMetadata() : 0;
        return of(new ItemStack(item, 1, meta));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AEItemKey aeItemKey = (AEItemKey) o;
        // The hash code comparison is a fast-fail cheap check
        return this.hashCode == aeItemKey.hashCode && Platform.isSameItemSameComponents(stack, aeItemKey.stack);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public static AEItemKey of(ItemLike item) {
        return of(new ItemStack(item.asItem()));
    }

    public static AEItemKey of(Item item) {
        return of(new ItemStack(item));
    }

    public static AEItemKey of(Item item, int meta) {
        return of(new ItemStack(item, 1, meta));
    }

    public boolean is(ItemLike item) {
        return stack.getItem() == item.asItem();
    }

    public boolean is(Item item) {
        return stack.getItem() == item;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && Platform.isSameItemSameComponents(this.stack, stack);
    }

    public boolean matches(Ingredient ingredient) {
        return ingredient.apply(getReadOnlyStack());
    }

    /**
     * @return The ItemStack represented by this key. <strong>NEVER MUTATE THIS</strong>
     */
    public ItemStack getReadOnlyStack() {
        return stack;
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    public ItemStack toStack(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        return Platform.copyWithCount(stack, count);
    }

    public Item getItem() {
        return stack.getItem();
    }

    /**
     * @return The 1.12.2 metadata (subtype or damage) of the item.
     */
    public int getMetadata() {
        return stack.getMetadata();
    }

    @Nullable
    public NBTTagCompound getTag() {
        return stack.getTagCompound();
    }

    @Nullable
    public static AEItemKey fromTag(NBTTagCompound tag) {
        try {
            return DataResults.getOrThrow(CODEC.decode(NbtOps.INSTANCE, tag)).getFirst();
        } catch (Exception e) {
            AELog.debug("Tried to load an invalid item key from NBT: %s", tag, e);
            return null;
        }
    }

    @Override
    public NBTTagCompound toTag() {
        return (NBTTagCompound) DataResults.getOrThrow(CODEC.encodeStart(NbtOps.INSTANCE, this));
    }

    @Override
    public Object getPrimaryKey() {
        return primaryKey;
    }

    /**
     * @see ItemStack#getMaxDamage()
     */
    @Override
    public int getFuzzySearchValue() {
        return this.damage;
    }

    /**
     * @see ItemStack#getItemDamage()
     */
    @Override
    public int getFuzzySearchMaxValue() {
        return stack.isItemStackDamageable() ? stack.getMaxDamage() : 0;
    }

    @Override
    public ResourceLocation getId() {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    @Override
    public ItemStack wrapForDisplayOrFilter() {
        return toStack();
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, World level, BlockPos pos) {
        while (amount > 0) {
            if (drops.size() > 1000) {
                AELog.warn("Tried dropping an excessive amount of items, ignoring %s %ss", amount, stack.getItem());
                break;
            }

            var taken = Math.min(amount, getMaxStackSize());
            amount -= taken;
            drops.add(toStack((int) taken));
        }
    }

    @Override
    protected ITextComponent computeDisplayName() {
        return Platform.getHoverName(getReadOnlyStack());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isTagged(TagKey<?> tag) {
        // This will just return false for incorrectly cast tags
        return ((TagKey<Item>) tag).contains(stack);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<T> type) {
        return type.get(stack);
    }

    @Override
    public boolean hasComponents() {
        return stack.hasTagCompound() || getCapsTag().isPresent();
    }

    /**
     * @return True if the item represented by this key is damaged.
     */
    public boolean isDamaged() {
        return damage > 0;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        ByteBufCodecs.ITEM_STACK.encode(data, stack);
    }

    public static AEItemKey fromPacket(RegistryFriendlyByteBuf data) {
        var stack = ByteBufCodecs.ITEM_STACK.decode(data);
        return new AEItemKey(stack);
    }

    private Optional<NBTTagCompound> getCapsTag() {
        return Platform.getCapsTag(stack);
    }

    @Override
    public String toString() {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String idString = id != BuiltInRegistries.ITEM.getDefaultKey() ? id.toString()
                : stack.getItem().getClass().getName() + "(unregistered)";
        if (stack.getMetadata() != 0) {
            idString += "@" + stack.getMetadata();
        }
        return !stack.hasTagCompound() ? idString : idString + " (with nbt)";
    }
}
