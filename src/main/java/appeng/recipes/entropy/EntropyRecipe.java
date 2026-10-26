/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.recipes.entropy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.core.tags.Flattening;
import appeng.recipes.AERecipe;
import appeng.recipes.AERecipeTypes;
import appeng.recipes.RecipeCodecs;
import appeng.recipes.RecipeInput;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.RecipeType;
import appeng.util.AECodecs;

/**
 * A recipe of the entropy manipulator: heating or cooling a block or fluid in the world.
 * <p/>
 * Fluids are blocks in this version: a fluid input matches the blocks of that fluid, {@code minecraft:water} its
 * source blocks and {@code minecraft:flowing_water} the flowing ones (like the fluid ids of newer versions). Modern
 * vanilla block ids are translated with {@link Flattening}, including their metadata (stone bricks are a variant of
 * {@code minecraft:stonebrick}).
 */
public class EntropyRecipe implements AERecipe<RecipeInput> {

    public static final MapCodec<EntropyRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            EntropyMode.CODEC.fieldOf("mode").forGetter(EntropyRecipe::getMode),
            Input.CODEC.fieldOf("input").forGetter(EntropyRecipe::getInput),
            Output.CODEC.fieldOf("output").forGetter(EntropyRecipe::getOutput)).apply(builder, EntropyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntropyRecipe> STREAM_CODEC = RecipeCodecs
            .viaJson(CODEC.codec());

    @Deprecated
    public static final ResourceLocation TYPE_ID = AppEng.makeId("entropy");

    @Deprecated
    public static final RecipeType<EntropyRecipe> TYPE = AERecipeTypes.ENTROPY;

    private final EntropyMode mode;
    private final Input input;
    private final Output output;

    EntropyRecipe(EntropyMode mode, Input input, Output output) {
        this.mode = mode;
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean matches(RecipeInput inv, World level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput inv) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EntropyRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public EntropyMode getMode() {
        return this.mode;
    }

    /**
     * @return The block that replaces the given one, or null if the recipe outputs a fluid or nothing.
     */
    @Nullable
    public IBlockState getOutputBlockState(IBlockState originalBlockState) {
        return output.block().map(blockOutput -> blockOutput.apply(originalBlockState)).orElse(null);
    }

    /**
     * @return The fluid block that replaces the original block, or null if the recipe outputs no fluid.
     */
    @Nullable
    public IBlockState getOutputFluidState() {
        return output.fluid().map(FluidOutput::toBlockState).orElse(null);
    }

    public List<ItemStack> getDrops() {
        return this.output.drops();
    }

    public boolean matches(EntropyMode mode, IBlockState blockState) {
        if (this.getMode() != mode) {
            return false;
        }

        return input.matches(blockState);
    }

    public Input getInput() {
        return input;
    }

    public Output getOutput() {
        return output;
    }

    /**
     * @return True for source blocks of fluids (and for blocks that aren't fluids).
     */
    static boolean isSource(IBlockState state) {
        var block = state.getBlock();
        if (block instanceof BlockLiquid) {
            return state.getValue(BlockLiquid.LEVEL) == 0;
        }
        if (block instanceof BlockFluidBase) {
            return state.getValue(BlockFluidBase.LEVEL) == 0;
        }
        return true;
    }

    /**
     * Copies a property from one state to another (if that state also has that property).
     */
    private static <T extends Comparable<T>> IBlockState copyProperty(IBlockState from, IBlockState to,
            IProperty<T> property) {
        if (to.getPropertyKeys().contains(property)) {
            return to.withProperty(property, from.getValue(property));
        } else {
            return to;
        }
    }

    /**
     * A block id of a recipe file: the block and the metadata of translated vanilla ids (-1 = any).
     */
    public static final class BlockRef {
        static final Codec<BlockRef> CODEC = AECodecs.RESOURCE_LOCATION.comapFlatMap(id -> {
            var flat = Flattening.lookup(id);
            if (flat != null && ForgeRegistries.BLOCKS.containsKey(flat.id())) {
                return DataResult.success(new BlockRef(ForgeRegistries.BLOCKS.getValue(flat.id()), flat.meta()));
            }
            if (ForgeRegistries.BLOCKS.containsKey(id)) {
                return DataResult.success(new BlockRef(ForgeRegistries.BLOCKS.getValue(id), -1));
            }
            return DataResult.error("Unknown block: " + id);
        }, ref -> ref.block.getRegistryName());

        private final Block block;
        private final int meta;

        BlockRef(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }

        public Block block() {
            return block;
        }

        boolean matches(IBlockState state) {
            return state.getBlock() == block && (meta < 0 || block.getMetaFromState(state) == meta);
        }

        @SuppressWarnings("deprecation")
        IBlockState defaultState() {
            return meta >= 0 ? block.getStateFromMeta(meta) : block.getDefaultState();
        }
    }

    /**
     * A fluid id of a recipe file; {@code flowing_*} means the flowing blocks of the fluid.
     */
    public static final class FluidRef {
        static final Codec<FluidRef> CODEC = AECodecs.RESOURCE_LOCATION.comapFlatMap(id -> {
            var name = id.getPath();
            var flowing = name.startsWith("flowing_");
            if (flowing) {
                name = name.substring("flowing_".length());
            }
            var fluid = FluidRegistry.getFluid(name);
            if (fluid == null) {
                return DataResult.error("Unknown fluid: " + id);
            }
            return DataResult.success(new FluidRef(fluid, flowing));
        }, ref -> new ResourceLocation((ref.flowing ? "flowing_" : "") + ref.fluid.getName()));

        private final Fluid fluid;
        private final boolean flowing;

        FluidRef(Fluid fluid, boolean flowing) {
            this.fluid = fluid;
            this.flowing = flowing;
        }

        public Fluid fluid() {
            return fluid;
        }

        public boolean flowing() {
            return flowing;
        }

        boolean matches(IBlockState state) {
            var stateFluid = FluidRegistry.lookupFluidForBlock(state.getBlock());
            return stateFluid != null && stateFluid.getName().equals(fluid.getName()) && isSource(state) != flowing;
        }

        @Nullable
        IBlockState toBlockState() {
            var block = fluid.getBlock();
            if (block == null) {
                return null;
            }
            if (!flowing) {
                return block.getDefaultState();
            }
            // A nearly full flowing block
            if (block instanceof BlockLiquid) {
                return BlockLiquid.getFlowingBlock(block.getDefaultState().getMaterial()).getDefaultState()
                        .withProperty(BlockLiquid.LEVEL, 1);
            }
            if (block instanceof BlockFluidBase) {
                return block.getDefaultState().withProperty(BlockFluidBase.LEVEL, 1);
            }
            return block.getDefaultState();
        }
    }

    public final static class Input {
        private final Optional<BlockInput> block;
        private final Optional<FluidInput> fluid;

        public Input(Optional<BlockInput> block, Optional<FluidInput> fluid) {
            this.block = block;
            this.fluid = fluid;
        }

        public Optional<BlockInput> block() {
            return block;
        }

        public Optional<FluidInput> fluid() {
            return fluid;
        }

        public static Codec<Input> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockInput.CODEC.optionalFieldOf("block").forGetter(Input::block),
                FluidInput.CODEC.optionalFieldOf("fluid").forGetter(Input::fluid)).apply(builder, Input::new));

        public boolean matches(IBlockState blockState) {
            if (block.isPresent()) {
                var input = block.get();
                if (!input.ref().matches(blockState)) {
                    return false;
                }
                var stateDefinition = input.ref().block().getBlockState();
                if (!PropertyUtils.doPropertiesMatch(stateDefinition, blockState, input.properties())) {
                    return false;
                }
            }

            if (fluid.isPresent() && !fluid.get().ref().matches(blockState)) {
                return false;
            }

            return block.isPresent() || fluid.isPresent();
        }
    }

    public final static class BlockInput {
        private final BlockRef ref;
        private final Map<String, PropertyValueMatcher> properties;

        public BlockInput(BlockRef ref, Map<String, PropertyValueMatcher> properties) {
            this.ref = ref;
            this.properties = properties;
        }

        public BlockRef ref() {
            return ref;
        }

        public Block block() {
            return ref.block();
        }

        public Map<String, PropertyValueMatcher> properties() {
            return properties;
        }

        public static Codec<BlockInput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockRef.CODEC.fieldOf("id").forGetter(BlockInput::ref),
                PropertyValueMatcher.MAP_CODEC.optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(BlockInput::properties))
                .apply(builder, BlockInput::new));
    }

    /**
     * Fluids have no properties in this version, properties given in the recipe file are ignored.
     */
    public final static class FluidInput {
        private final FluidRef ref;
        private final Map<String, PropertyValueMatcher> properties;

        public FluidInput(FluidRef ref, Map<String, PropertyValueMatcher> properties) {
            this.ref = ref;
            this.properties = properties;
        }

        public FluidRef ref() {
            return ref;
        }

        public Fluid fluid() {
            return ref.fluid();
        }

        public Map<String, PropertyValueMatcher> properties() {
            return properties;
        }

        public static Codec<FluidInput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                FluidRef.CODEC.fieldOf("id").forGetter(FluidInput::ref),
                PropertyValueMatcher.MAP_CODEC.optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(FluidInput::properties))
                .apply(builder, FluidInput::new));
    }

    public final static class Output {
        private final Optional<BlockOutput> block;
        private final Optional<FluidOutput> fluid;
        private final List<ItemStack> drops;

        public Output(Optional<BlockOutput> block, Optional<FluidOutput> fluid, List<ItemStack> drops) {
            this.block = block;
            this.fluid = fluid;
            this.drops = drops;
        }

        public Optional<BlockOutput> block() {
            return block;
        }

        public Optional<FluidOutput> fluid() {
            return fluid;
        }

        public List<ItemStack> drops() {
            return drops;
        }

        public static Codec<Output> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockOutput.CODEC.optionalFieldOf("block").forGetter(Output::block),
                FluidOutput.CODEC.optionalFieldOf("fluid").forGetter(Output::fluid),
                AECodecs.ITEM_STACK.listOf().optionalFieldOf("drops", Collections.emptyList()).forGetter(Output::drops))
                .apply(builder, Output::new));
    }

    public final static class BlockOutput {
        private final BlockRef ref;
        private final boolean keepProperties;
        private final Map<String, String> properties;

        public BlockOutput(BlockRef ref, boolean keepProperties, Map<String, String> properties) {
            this.ref = ref;
            this.keepProperties = keepProperties;
            this.properties = properties;
        }

        public BlockRef ref() {
            return ref;
        }

        public Block block() {
            return ref.block();
        }

        public boolean keepProperties() {
            return keepProperties;
        }

        public Map<String, String> properties() {
            return properties;
        }

        public static Codec<BlockOutput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockRef.CODEC.fieldOf("id").forGetter(BlockOutput::ref),
                Codec.BOOL.optionalFieldOf("keepProperties", false).forGetter(BlockOutput::keepProperties),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(BlockOutput::properties))
                .apply(builder, BlockOutput::new));

        public IBlockState apply(IBlockState originalBlockState) {
            IBlockState state = ref.defaultState();

            if (keepProperties) {
                for (IProperty<?> property : originalBlockState.getPropertyKeys()) {
                    state = copyProperty(originalBlockState, state, property);
                }
            }

            return PropertyUtils.applyProperties(state.getBlock().getBlockState(), state, properties);
        }
    }

    /**
     * Fluids have no properties in this version, the source block (or a flowing block) of the fluid is placed.
     */
    public final static class FluidOutput {
        private final FluidRef ref;
        private final boolean keepProperties;
        private final Map<String, String> properties;

        public FluidOutput(FluidRef ref, boolean keepProperties, Map<String, String> properties) {
            this.ref = ref;
            this.keepProperties = keepProperties;
            this.properties = properties;
        }

        public FluidRef ref() {
            return ref;
        }

        public Fluid fluid() {
            return ref.fluid();
        }

        public boolean keepProperties() {
            return keepProperties;
        }

        public Map<String, String> properties() {
            return properties;
        }

        public static Codec<FluidOutput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                FluidRef.CODEC.fieldOf("id").forGetter(FluidOutput::ref),
                Codec.BOOL.optionalFieldOf("keepProperties", false).forGetter(FluidOutput::keepProperties),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(FluidOutput::properties))
                .apply(builder, FluidOutput::new));

        @Nullable
        public IBlockState toBlockState() {
            return ref.toBlockState();
        }
    }
}
