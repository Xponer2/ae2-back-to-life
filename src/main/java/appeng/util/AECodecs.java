package appeng.util;

import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import appeng.api.ids.AEComponents;
import appeng.core.definitions.AEItems;
import appeng.core.tags.Flattening;

/**
 * AE2's own codecs plus replacements for codecs that modern Minecraft defines on vanilla classes
 * ({@code ItemStack.CODEC} etc).
 */
public final class AECodecs {
    private static final Logger LOG = LogManager.getLogger(AECodecs.class);

    private AECodecs() {
    }

    public static final Codec<ResourceLocation> RESOURCE_LOCATION = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(new ResourceLocation(s));
        } catch (Exception e) {
            return DataResult.error("Not a valid resource location: " + s);
        }
    }, ResourceLocation::toString);

    public static final Codec<Item> ITEM = RESOURCE_LOCATION.comapFlatMap(id -> {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || (item == Items.AIR && !"air".equals(id.getPath()))) {
            var flat = Flattening.lookup(id);
            if (flat != null) {
                item = ForgeRegistries.ITEMS.getValue(flat.id());
            }
        }
        if (item == null || (item == Items.AIR && !"air".equals(id.getPath()))) {
            return DataResult.error("Unknown item: " + id);
        }
        return DataResult.success(item);
    }, Item::getRegistryName);

    public static final Codec<Block> BLOCK = RESOURCE_LOCATION.comapFlatMap(id -> {
        if (!ForgeRegistries.BLOCKS.containsKey(id)) {
            var flat = Flattening.lookup(id);
            if (flat != null && ForgeRegistries.BLOCKS.containsKey(flat.id())) {
                return DataResult.success(ForgeRegistries.BLOCKS.getValue(flat.id()));
            }
            return DataResult.error("Unknown block: " + id);
        }
        return DataResult.success(ForgeRegistries.BLOCKS.getValue(id));
    }, Block::getRegistryName);

    public static final Codec<Fluid> FLUID = RESOURCE_LOCATION.comapFlatMap(id -> {
        // Forge 1.12.2 fluids are only identified by name, modern ids are namespace:path
        Fluid fluid = FluidRegistry.getFluid(id.getPath());
        if (fluid == null) {
            fluid = FluidRegistry.getFluid(id.toString());
        }
        if (fluid == null) {
            return DataResult.error("Unknown fluid: " + id);
        }
        return DataResult.success(fluid);
    }, f -> new ResourceLocation(Platform.getFluidId(f)));

    public static final Codec<NBTTagCompound> COMPOUND_TAG = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        NBTBase tag = dynamic.convert(NbtOps.INSTANCE).getValue();
        if (tag instanceof NBTTagCompound c) {
            return DataResult.success(c.copy());
        }
        return DataResult.error("Not a compound tag: " + tag);
    }, tag -> new Dynamic<>(NbtOps.INSTANCE, tag.copy()));

    /**
     * Modern ItemStack.CODEC layout: {id, count} plus 1.12.2 metadata ("damage"), NBT ("tag") and capabilities.
     */
    public static final Codec<ItemStack> ITEM_STACK = RecordCodecBuilder.create(i -> i.group(
            ITEM.fieldOf("id").forGetter(ItemStack::getItem),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStack::getCount),
            Codec.INT.optionalFieldOf("damage", 0).forGetter(ItemStack::getMetadata),
            COMPOUND_TAG.optionalFieldOf("tag").forGetter(s -> Optional.ofNullable(s.getTagCompound())),
            COMPOUND_TAG.optionalFieldOf("ForgeCaps").forGetter(AECodecs::getCaps))
            .apply(i, (item, count, damage, tag, caps) -> {
                var stack = new ItemStack(item, count, damage, caps.orElse(null));
                tag.ifPresent(t -> stack.setTagCompound(t.copy()));
                return stack;
            }));

    /**
     * Modern Minecraft encodes an empty item stack as an empty map.
     */
    public static final Codec<ItemStack> OPTIONAL_ITEM_STACK = new Codec<ItemStack>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            var map = ops.getMap(input);
            if (map.result().isPresent() && map.result().get().get("id") == null) {
                return DataResult.success(Pair.of(ItemStack.EMPTY, input));
            }
            return ITEM_STACK.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(ItemStack input, DynamicOps<T> ops, T prefix) {
            if (input.isEmpty()) {
                return DataResult.success(ops.emptyMap());
            }
            return ITEM_STACK.encode(input, ops, prefix);
        }
    };

    public static final Codec<ItemStack> SINGLE_ITEM_STACK = ITEM_STACK;

    public static final Codec<FluidStack> FLUID_STACK = RecordCodecBuilder.create(i -> i.group(
            FLUID.fieldOf("id").forGetter(FluidStack::getFluid),
            Codec.INT.fieldOf("amount").forGetter(s -> s.amount),
            COMPOUND_TAG.optionalFieldOf("tag").forGetter(s -> Optional.ofNullable(s.tag)))
            .apply(i, (fluid, amount, tag) -> new FluidStack(fluid, amount, tag.orElse(null))));

    public static final Codec<ITextComponent> COMPONENT = Codec.STRING.xmap(
            ITextComponent.Serializer::jsonToComponent,
            ITextComponent.Serializer::componentToJson);

    public static final Codec<BlockPos> BLOCK_POS = Codec.INT_STREAM.comapFlatMap(stream -> {
        int[] a = stream.toArray();
        if (a.length != 3) {
            return DataResult.error("Block pos needs 3 ints");
        }
        return DataResult.success(new BlockPos(a[0], a[1], a[2]));
    }, pos -> java.util.stream.IntStream.of(pos.getX(), pos.getY(), pos.getZ()));

    public static final Codec<EnumFacing> DIRECTION = Codec.STRING.comapFlatMap(s -> {
        EnumFacing dir = EnumFacing.byName(s);
        return dir != null ? DataResult.success(dir) : DataResult.error("Unknown direction " + s);
    }, EnumFacing::getName);

    /**
     * Block states are stored as {Name, Meta}.
     */
    @SuppressWarnings("deprecation")
    public static final Codec<IBlockState> BLOCK_STATE = RecordCodecBuilder.create(i -> i.group(
            BLOCK.fieldOf("Name").forGetter(IBlockState::getBlock),
            Codec.INT.optionalFieldOf("Meta", 0).forGetter(s -> s.getBlock().getMetaFromState(s)))
            .apply(i, Block::getStateFromMeta));

    private static Optional<NBTTagCompound> getCaps(ItemStack stack) {
        var full = stack.writeToNBT(new NBTTagCompound());
        return full.hasKey("ForgeCaps") ? Optional.of(full.getCompoundTag("ForgeCaps")) : Optional.empty();
    }

    private static final Codec.ResultFunction<ItemStack> MISSING_CONTENT_ITEMSTACK_RESULT = new Codec.ResultFunction<ItemStack>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<ItemStack, T>> a) {
            if (DataResults.isError(a)) {
                var missingContent = AEItems.MISSING_CONTENT.stack();
                var convert = Dynamic.convert(ops, NbtOps.INSTANCE, input);
                if (convert instanceof NBTTagCompound compoundTag) {
                    AEComponents.MISSING_CONTENT_ITEMSTACK_DATA.set(missingContent, compoundTag.copy());
                }
                LOG.error("Failed to deserialize ItemStack: {}", DataResults.message(a));
                AEComponents.MISSING_CONTENT_ERROR.set(missingContent, DataResults.message(a));

                return DataResult.success(
                        Pair.of(missingContent, input),
                        Lifecycle.stable());
            }

            // Return unchanged if deserialization succeeded
            return a;
        }

        @Override
        public <T> DataResult<T> coApply(DynamicOps<T> ops, ItemStack input, DataResult<T> t) {
            // When the serialization result failed, we write a missing content item instead
            // this one will NOT be recoverable
            if (DataResults.isError(t)) {
                var missingContent = AEItems.MISSING_CONTENT.stack();
                LOG.error("Failed to serialize ItemStack {}: {}", input, DataResults.message(t));
                AEComponents.MISSING_CONTENT_ERROR.set(missingContent, DataResults.message(t));

                return SINGLE_ITEM_STACK.encodeStart(ops, missingContent).setLifecycle(t.lifecycle());
            }

            // When the input is a MISSING_CONTENT item and has the original data attached,
            // we write that back.
            if (AEItems.MISSING_CONTENT.is(input)) {
                var originalData = AEComponents.MISSING_CONTENT_ITEMSTACK_DATA.get(input);
                if (originalData != null) {
                    return DataResult.success(Dynamic.convert(NbtOps.INSTANCE, ops, originalData),
                            t.lifecycle());
                }
            }

            return t;
        }
    };

    public static final Codec<ItemStack> FAULT_TOLERANT_SIMPLE_ITEM_CODEC = SINGLE_ITEM_STACK
            .mapResult(MISSING_CONTENT_ITEMSTACK_RESULT);

    public static final Codec<ItemStack> FAULT_TOLERANT_ITEMSTACK_CODEC = ITEM_STACK
            .mapResult(MISSING_CONTENT_ITEMSTACK_RESULT);

    public static final Codec<ItemStack> FAULT_TOLERANT_OPTIONAL_ITEMSTACK_CODEC = OPTIONAL_ITEM_STACK
            .mapResult(MISSING_CONTENT_ITEMSTACK_RESULT);
}
