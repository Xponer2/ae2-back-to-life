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

package appeng.crafting.pattern;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import appeng.util.CraftingInput;
import appeng.util.RecipeHolder;
import appeng.recipes.vanilla.SingleRecipeInput;
import appeng.recipes.vanilla.StonecutterRecipe;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.core.localization.GuiText;
import appeng.recipes.RecipeManager;

/**
 * Encodes patterns for the {@link net.minecraft.world.level.block.StonecutterBlock}.
 */
public class AEStonecuttingPattern implements IPatternDetails, IMolecularAssemblerSupportedPattern {
    // The slot index in the 3x3 crafting grid that we insert our item into (in the MAC)
    private static final int CRAFTING_GRID_SLOT = 4;

    private final AEItemKey definition;
    public final boolean canSubstitute;
    private final ResourceLocation recipeId;
    private final StonecutterRecipe recipe;
    private final AEItemKey input;
    private final ItemStack output;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;

    /**
     * We cache results of isValid(...) calls for stacks that don't have NBT.
     */
    // Keyed by the primary key: in 1.12.2 variants of an item (wool colors, planks) differ by metadata
    private final Map<Object, Boolean> isValidCache = new IdentityHashMap<>();

    public AEStonecuttingPattern(AEItemKey definition, World level) {
        this.definition = definition;

        var encodedPattern = AEComponents.ENCODED_STONECUTTING_PATTERN.get(definition);
        if (encodedPattern == null) {
            throw new IllegalArgumentException("Given item does not encode a stonecutting pattern: " + definition);
        } else if (encodedPattern.containsMissingContent()) {
            throw new IllegalArgumentException("Pattern references missing content");
        }

        this.input = Objects.requireNonNull(AEItemKey.of(encodedPattern.input()));
        this.canSubstitute = encodedPattern.canSubstitute();

        // Find recipe
        this.recipeId = encodedPattern.recipeId();
        this.recipe = RecipeManager.of(level).byKey(recipeId).map(holder -> (StonecutterRecipe) holder.value())
                .orElse(null);
        if (recipe == null) {
            throw new IllegalStateException("Stonecutting pattern references unknown recipe " + recipeId);
        }

        // Build frame and find output
        var testInput = new SingleRecipeInput(input.toStack());
        if (!this.recipe.matches(testInput, level)) {
            throw new IllegalStateException("The recipe " + recipeId + " no longer matches the encoded input.");
        }

        this.output = this.recipe.assemble(testInput);
        if (this.output.isEmpty()) {
            throw new IllegalStateException("The recipe " + recipeId + " produced an empty item stack result.");
        }

        this.inputs = new IInput[] {
                new Input()
        };
        this.outputs = Collections.singletonList(GenericStack.fromItemStack(this.output));
    }

    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass()
                && ((AEStonecuttingPattern) obj).definition.equals(definition);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    @Override
    public PatternDetailsTooltip getTooltip(World level, ITooltipFlag flags) {
        var tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_CRAFTS);
        tooltip.addInputsAndOutputs(this);
        if (flags.isAdvanced()) {
            tooltip.addProperty(new TextComponentString("Recipe"), new TextComponentString(recipeId.toString()));
        }
        return tooltip;
    }

    public static PatternDetailsTooltip getInvalidTooltip(ItemStack stack, World level, @Nullable Exception cause,
            boolean advancedTooltips) {

        var tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_CRAFTS);
        var encodedPattern = AEComponents.ENCODED_STONECUTTING_PATTERN.get(stack);
        if (encodedPattern != null) {
            if (encodedPattern.canSubstitute()) {
                tooltip.addProperty(GuiText.PatternTooltipSubstitutions.text());
            }
            if (advancedTooltips) {
                tooltip.addProperty(new TextComponentString("Recipe"),
                        new TextComponentString(encodedPattern.recipeId().toString()));
            }
        }
        return tooltip;
    }

    /**
     * Gets the {@link Ingredient} from the actual used recipe. Stonecutting recipes must have exactly one ingredient.
     */
    private Ingredient getRecipeIngredient() {
        return recipe.getIngredients().get(0);
    }

    public boolean isItemValid(AEItemKey key, World level) {
        if (key == null) {
            return false;
        }

        if (!canSubstitute) {
            return input.equals(key);
        }

        var result = getTestResult(key);
        if (result != null) {
            return result;
        }

        // Fill frame and check result
        var testInput = new SingleRecipeInput(key.toStack());

        var newResult = recipe.matches(testInput, level)
                && ItemStack.areItemStacksEqual(output, recipe.assemble(testInput));

        setTestResult(key, newResult);

        return newResult;
    }

    /**
     * Retrieve a previous result of testing whether <code>what</code> is a valid ingredient for <code>slot</code>.
     *
     * @return null if the result is unknown, otherwise indicates whether the key is valid or not.
     */
    @Nullable
    private Boolean getTestResult(AEItemKey what) {
        if (what == null || what.hasComponents()) {
            return null;
        }

        return isValidCache.get(what.getPrimaryKey());
    }

    private void setTestResult(AEItemKey what, boolean result) {
        if (what != null && !what.hasComponents()) {
            isValidCache.put(what.getPrimaryKey(), result);
        }
    }

    public boolean canSubstitute() {
        return canSubstitute;
    }

    @Override
    public ItemStack assemble(CraftingInput container, World level) {
        if (container.size() != 1) {
            return ItemStack.EMPTY;
        }

        // Jiggle the container around
        var testInput = new SingleRecipeInput(container.getItem(0));

        if (recipe.matches(testInput, level)) {
            return recipe.assemble(testInput);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValid(int slot, AEItemKey key, World level) {
        return slot == CRAFTING_GRID_SLOT && isItemValid(key, level);
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return slot == CRAFTING_GRID_SLOT;
    }

    @Override
    public void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor gridAccessor) {
        var entry = table[0].getFirstEntry();
        if (entry != null && entry.getKey() instanceof AEItemKey itemKey) {
            gridAccessor.set(CRAFTING_GRID_SLOT, itemKey.toStack());
            table[0].remove(entry.getKey(), 1);
        }
    }

    public AEItemKey getInput() {
        return input;
    }

    public static void encode(ItemStack stack, RecipeHolder<StonecutterRecipe> recipe, AEItemKey input,
            AEItemKey output,
            boolean allowSubstitution) {
        Preconditions.checkNotNull(recipe, "recipe");
        Preconditions.checkNotNull(input, "input");
        Preconditions.checkNotNull(output, "output");

        AEComponents.ENCODED_STONECUTTING_PATTERN.set(stack, new EncodedStonecuttingPattern(
                input.toStack(),
                output.toStack(),
                allowSubstitution,
                recipe.id()));
    }

    private class Input implements IInput {
        private final GenericStack[] possibleInputs;

        private Input() {
            if (!canSubstitute) {
                this.possibleInputs = new GenericStack[] { new GenericStack(input, 1) };
            } else {
                ItemStack[] matchingStacks = getRecipeIngredient().getMatchingStacks();
                this.possibleInputs = new GenericStack[matchingStacks.length + 1];
                // Ensure that the stack chosen by the user gets precedence.
                this.possibleInputs[0] = new GenericStack(input, 1);
                for (int i = 0; i < matchingStacks.length; ++i) {
                    this.possibleInputs[i + 1] = GenericStack.fromItemStack(matchingStacks[i]);
                }
            }
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possibleInputs;
        }

        @Override
        public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey input, World level) {
            if (input.matches(possibleInputs[0])) {
                return true; // Exact match
            } else if (canSubstitute() && input instanceof AEItemKey itemKey) {
                return AEStonecuttingPattern.this.isItemValid(itemKey, level);
            } else {
                return false;
            }
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
