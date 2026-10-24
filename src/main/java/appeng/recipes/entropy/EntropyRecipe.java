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

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.Fluid;

import appeng.api.util.Registries;
import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.items.tools.powered.EntropyManipulatorItem;
import appeng.recipes.AERecipeTypes;
import appeng.util.AECodecs;
import appeng.util.BuiltInRegistries;

/**
 * A special recipe used for the {@link EntropyManipulatorItem}.
 */
public class EntropyRecipe implements IRecipe<RecipeInput> {

    public static final MapCodec<EntropyRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            EntropyMode.CODEC.fieldOf("mode").forGetter(EntropyRecipe::getMode),
            Input.CODEC.fieldOf("input").forGetter(EntropyRecipe::getInput),
            Output.CODEC.fieldOf("output").forGetter(EntropyRecipe::getOutput)).apply(builder, EntropyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntropyRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.enumCodec(EntropyMode.class),
            EntropyRecipe::getMode,
            Input.STREAM_CODEC,
            EntropyRecipe::getInput,
            Output.STREAM_CODEC,
            EntropyRecipe::getOutput,
            EntropyRecipe::new);

    @Deprecated(forRemoval = true, since = "1.21.1")
    public static final ResourceLocation TYPE_ID = AppEng.makeId("entropy");

    @Deprecated(forRemoval = true, since = "1.21.1")
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

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    public EntropyMode getMode() {
        return this.mode;
    }

    @Nullable
    public IBlockState getOutputBlockState(IBlockState originalBlockState) {
        return output.block().map(blockOutput -> blockOutput.apply(originalBlockState)).orElse(null);
    }

    @Nullable
    public FluidState getOutputFluidState(FluidState originalFluidState) {
        return output.fluid().map(fluidOutput -> fluidOutput.apply(originalFluidState)).orElse(null);
    }

    public List<ItemStack> getDrops() {
        return this.output.drops();
    }

    public boolean matches(EntropyMode mode, IBlockState blockState, FluidState fluidState) {
        if (this.getMode() != mode) {
            return false;
        }

        return input.matches(blockState, fluidState);
    }

    /**
     * Copies a property from one stateholder to another (if that stateholder also has that property).
     */
    private static <T extends Comparable<T>, SH extends StateHolder<?, SH>> SH copyProperty(SH from, SH to,
            IProperty<T> property) {
        if (to.hasProperty(property)) {
            return to.setValue(property, from.getValue(property));
        } else {
            return to;
        }
    }

    public Input getInput() {
        return input;
    }

    public Output getOutput() {
        return output;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Input)) return false;
            Input that = (Input) o;
            return java.util.Objects.equals(block, that.block)
                    && java.util.Objects.equals(fluid, that.fluid);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(block, fluid);
        }

        @Override
        public String toString() {
            return "Input[" + "block=" + block + ", " + "fluid=" + fluid + "]";
        }

        public static Codec<Input> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockInput.CODEC.optionalFieldOf("block").forGetter(Input::block),
                FluidInput.CODEC.optionalFieldOf("fluid").forGetter(Input::fluid)).apply(builder, Input::new));

        public static StreamCodec<RegistryFriendlyByteBuf, Input> STREAM_CODEC = StreamCodec.composite(
                BlockInput.STREAM_CODEC.apply(ByteBufCodecs::optional),
                Input::block,
                FluidInput.STREAM_CODEC.apply(ByteBufCodecs::optional),
                Input::fluid,
                Input::new);

        public boolean matches(IBlockState blockState, FluidState fluidState) {
            if (block.isPresent()) {
                var inputBlock = block.get().block();

                if (blockState.getBlock() != inputBlock) {
                    return false;
                }
                var stateDefinition = inputBlock.getBlockState();
                if (!PropertyUtils.doPropertiesMatch(stateDefinition, blockState, block.get().properties())) {
                    return false;
                }
            }

            if (fluid.isPresent()) {
                var inputFluid = fluid.get().fluid();
                if (fluidState.getType() != inputFluid) {
                    return false;
                }
                var stateDefinition = inputFluid.getBlockState();
                if (!PropertyUtils.doPropertiesMatch(stateDefinition, fluidState, fluid.get().properties())) {
                    return false;
                }
            }

            return true;
        }
    }

    public final static class BlockInput {
        private final Block block;
        private final Map<String, PropertyValueMatcher> properties;

        public BlockInput(Block block, Map<String, PropertyValueMatcher> properties) {
            this.block = block;
            this.properties = properties;
        }

        public Block block() {
            return block;
        }

        public Map<String, PropertyValueMatcher> properties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockInput)) return false;
            BlockInput that = (BlockInput) o;
            return java.util.Objects.equals(block, that.block)
                    && java.util.Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(block, properties);
        }

        @Override
        public String toString() {
            return "BlockInput[" + "block=" + block + ", " + "properties=" + properties + "]";
        }

        public static Codec<BlockInput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                AECodecs.BLOCK.fieldOf("id").forGetter(BlockInput::block),
                PropertyValueMatcher.MAP_CODEC.optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(BlockInput::properties))
                .apply(builder, BlockInput::new));

        public static StreamCodec<RegistryFriendlyByteBuf, BlockInput> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BLOCK,
                BlockInput::block,
                ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, ByteBufCodecs.STRING_UTF8,
                        PropertyValueMatcher.STREAM_CODEC),
                BlockInput::properties,
                BlockInput::new);
    }

    public final static class FluidInput {
        private final Fluid fluid;
        private final Map<String, PropertyValueMatcher> properties;

        public FluidInput(Fluid fluid, Map<String, PropertyValueMatcher> properties) {
            this.fluid = fluid;
            this.properties = properties;
        }

        public Fluid fluid() {
            return fluid;
        }

        public Map<String, PropertyValueMatcher> properties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FluidInput)) return false;
            FluidInput that = (FluidInput) o;
            return java.util.Objects.equals(fluid, that.fluid)
                    && java.util.Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(fluid, properties);
        }

        @Override
        public String toString() {
            return "FluidInput[" + "fluid=" + fluid + ", " + "properties=" + properties + "]";
        }

        public static Codec<FluidInput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                AECodecs.FLUID.fieldOf("id").forGetter(FluidInput::fluid),
                PropertyValueMatcher.MAP_CODEC.optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(FluidInput::properties))
                .apply(builder, FluidInput::new));

        public static StreamCodec<RegistryFriendlyByteBuf, FluidInput> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLUID,
                FluidInput::fluid,
                ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, ByteBufCodecs.STRING_UTF8,
                        PropertyValueMatcher.STREAM_CODEC),
                FluidInput::properties,
                FluidInput::new);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Output)) return false;
            Output that = (Output) o;
            return java.util.Objects.equals(block, that.block)
                    && java.util.Objects.equals(fluid, that.fluid)
                    && java.util.Objects.equals(drops, that.drops);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(block, fluid, drops);
        }

        @Override
        public String toString() {
            return "Output[" + "block=" + block + ", " + "fluid=" + fluid + ", " + "drops=" + drops + "]";
        }

        public static Codec<Output> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockOutput.CODEC.optionalFieldOf("block").forGetter(Output::block),
                FluidOutput.CODEC.optionalFieldOf("fluid").forGetter(Output::fluid),
                AECodecs.ITEM_STACK.listOf().optionalFieldOf("drops", Collections.emptyList()).forGetter(Output::drops))
                .apply(builder, Output::new));

        public static StreamCodec<RegistryFriendlyByteBuf, Output> STREAM_CODEC = StreamCodec.composite(
                BlockOutput.STREAM_CODEC.apply(ByteBufCodecs::optional),
                Output::block,
                FluidOutput.STREAM_CODEC.apply(ByteBufCodecs::optional),
                Output::fluid,
                ByteBufCodecs.ITEM_STACK_LIST,
                Output::drops,
                Output::new);
    }

    public final static class BlockOutput {
        private final Block block;
        private final boolean keepProperties;
        private final Map<String, String> properties;

        public BlockOutput(Block block, boolean keepProperties, Map<String, String> properties) {
            this.block = block;
            this.keepProperties = keepProperties;
            this.properties = properties;
        }

        public Block block() {
            return block;
        }

        public boolean keepProperties() {
            return keepProperties;
        }

        public Map<String, String> properties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockOutput)) return false;
            BlockOutput that = (BlockOutput) o;
            return java.util.Objects.equals(block, that.block)
                    && keepProperties == that.keepProperties
                    && java.util.Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(block, keepProperties, properties);
        }

        @Override
        public String toString() {
            return "BlockOutput[" + "block=" + block + ", " + "keepProperties=" + keepProperties + ", " + "properties=" + properties + "]";
        }

        public static Codec<BlockOutput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                AECodecs.BLOCK.fieldOf("id").forGetter(BlockOutput::block),
                Codec.BOOL.optionalFieldOf("", false).forGetter(BlockOutput::keepProperties),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(BlockOutput::properties))
                .apply(builder, BlockOutput::new));

        public static StreamCodec<RegistryFriendlyByteBuf, BlockOutput> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BLOCK,
                BlockOutput::block,
                ByteBufCodecs.BOOL,
                BlockOutput::keepProperties,
                ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, ByteBufCodecs.STRING_UTF8,
                        ByteBufCodecs.STRING_UTF8),
                BlockOutput::properties,
                BlockOutput::new);

        public IBlockState apply(IBlockState originalBlockState) {
            IBlockState state = block.getDefaultState();

            if (keepProperties) {
                for (IProperty<?> property : originalBlockState.getProperties()) {
                    state = copyProperty(originalBlockState, state, property);
                }
            }

            var stateDefinition = originalBlockState.getBlock().getBlockState();
            state = PropertyUtils.applyProperties(stateDefinition, state, properties);

            return state;
        }
    }

    public final static class FluidOutput {
        private final Fluid fluid;
        private final boolean keepProperties;
        private final Map<String, String> properties;

        public FluidOutput(Fluid fluid, boolean keepProperties, Map<String, String> properties) {
            this.fluid = fluid;
            this.keepProperties = keepProperties;
            this.properties = properties;
        }

        public Fluid fluid() {
            return fluid;
        }

        public boolean keepProperties() {
            return keepProperties;
        }

        public Map<String, String> properties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FluidOutput)) return false;
            FluidOutput that = (FluidOutput) o;
            return java.util.Objects.equals(fluid, that.fluid)
                    && keepProperties == that.keepProperties
                    && java.util.Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(fluid, keepProperties, properties);
        }

        @Override
        public String toString() {
            return "FluidOutput[" + "fluid=" + fluid + ", " + "keepProperties=" + keepProperties + ", " + "properties=" + properties + "]";
        }

        public static Codec<FluidOutput> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                AECodecs.FLUID.fieldOf("id").forGetter(FluidOutput::fluid),
                Codec.BOOL.optionalFieldOf("", false).forGetter(FluidOutput::keepProperties),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("properties", Collections.emptyMap())
                        .forGetter(FluidOutput::properties))
                .apply(builder, FluidOutput::new));

        public static StreamCodec<RegistryFriendlyByteBuf, FluidOutput> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLUID,
                FluidOutput::fluid,
                ByteBufCodecs.BOOL,
                FluidOutput::keepProperties,
                ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, ByteBufCodecs.STRING_UTF8,
                        ByteBufCodecs.STRING_UTF8),
                FluidOutput::properties,
                FluidOutput::new);

        public FluidState apply(FluidState originalFluidState) {
            FluidState state = fluid.defaultFluidState();

            if (keepProperties) {
                for (IProperty<?> property : originalFluidState.getProperties()) {
                    state = copyProperty(originalFluidState, state, property);
                }
            }

            var stateDefinition = state.getType().getBlockState();
            state = PropertyUtils.applyProperties(stateDefinition, state, properties);

            return state;
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, FluidOutput output) {
            buffer.writeById(BuiltInRegistries.FLUID::getId, output.fluid);
            buffer.writeBoolean(output.keepProperties);
            buffer.writeMap(output.properties, RegistryFriendlyByteBuf::writeUtf, (fbb, value) -> fbb.writeUtf(value));
        }

        public static FluidOutput fromNetwork(RegistryFriendlyByteBuf buffer) {
            var fluid = buffer.readById(BuiltInRegistries.FLUID::byId);
            var keepProperties = buffer.readBoolean();
            var properties = buffer.readMap(RegistryFriendlyByteBuf::readUtf, fbb -> fbb.readUtf());
            return new FluidOutput(fluid, keepProperties, properties);
        }
    }

}
