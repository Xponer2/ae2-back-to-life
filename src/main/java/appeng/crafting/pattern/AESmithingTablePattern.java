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
import java.util.List;
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
import appeng.recipes.vanilla.SmithingRecipe;
import appeng.recipes.vanilla.SmithingRecipeInput;

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
 * Encodes patterns for the {@link net.minecraft.world.level.block.SmithingTableBlock}.
 */
public class AESmithingTablePattern implements IPatternDetails, IMolecularAssemblerSupportedPattern {
    // The slot indices in the 3x3 crafting grid that we insert our item into (in the MAC)
    private static final int TEMPLATE_CRAFTING_GRID_SLOT = 3;
    private static final int BASE_CRAFTING_GRID_SLOT = 4;
    private static final int ADDITION_CRAFTING_GRID_SLOT = 5;

    private final AEItemKey definition;
    public final boolean canSubstitute;
    private final ResourceLocation recipeId;
    private final SmithingRecipe recipe;
    private final ItemStack output;
    private final AEItemKey template;
    private final AEItemKey base;
    private final AEItemKey addition;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;

    public AESmithingTablePattern(AEItemKey definition, World level) {
        this.definition = definition;

        var encodedPattern = AEComponents.ENCODED_SMITHING_TABLE_PATTERN.get(definition);
        if (encodedPattern == null) {
            throw new IllegalArgumentException("Given item does not encode a smithing table pattern: " + definition);
        } else if (encodedPattern.containsMissingContent()) {
            throw new IllegalArgumentException("Pattern references missing content");
        }

        this.template = Objects.requireNonNull(AEItemKey.of(encodedPattern.template()), "template");
        this.base = Objects.requireNonNull(AEItemKey.of(encodedPattern.base()), "base");
        this.addition = Objects.requireNonNull(AEItemKey.of(encodedPattern.addition()), "addition");
        this.canSubstitute = encodedPattern.canSubstitute();

        // Find recipe
        this.recipeId = encodedPattern.recipeId();
        this.recipe = RecipeManager.of(level).byKey(recipeId).map(holder -> (SmithingRecipe) holder.value())
                .orElse(null);
        if (recipe == null) {
            throw new IllegalStateException("Smithing pattern references unknown recipe " + recipeId);
        }

        // Build frame and find output
        var testFrame = new SmithingRecipeInput(
                template.toStack(),
                base.toStack(),
                addition.toStack());

        if (!this.recipe.matches(testFrame, level)) {
            throw new IllegalStateException("The recipe " + recipeId + " no longer matches the encoded input.");
        }

        this.output = this.recipe.assemble(testFrame);
        if (this.output.isEmpty()) {
            throw new IllegalStateException("The recipe " + recipeId + " produced an empty item stack result.");
        }

        // Find ingredients
        // Only smithing transform recipes exist in this version (no armor trims)
        Ingredient templateIngredient = this.recipe.getTemplate();
        Ingredient baseIngredient = this.recipe.getBase();
        Ingredient additionIngredient = this.recipe.getAddition();

        this.inputs = new IInput[] {
                new Input(template, templateIngredient, TEMPLATE_CRAFTING_GRID_SLOT),
                new Input(base, baseIngredient, BASE_CRAFTING_GRID_SLOT),
                new Input(addition, additionIngredient, ADDITION_CRAFTING_GRID_SLOT)
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
                && ((AESmithingTablePattern) obj).definition.equals(definition);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    public AEItemKey getTemplate() {
        return template;
    }

    public AEItemKey getBase() {
        return base;
    }

    public AEItemKey getAddition() {
        return addition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    public boolean canSubstitute() {
        return canSubstitute;
    }

    @Override
    public ItemStack assemble(CraftingInput container, World level) {
        // Note that the crafting input is always "compressed" and removes any margins,
        // so the slots we used to insert into the Molecular Assembler will change.
        // noinspection SizeReplaceableByIsEmpty
        var testFrame = new SmithingRecipeInput(
                container.size() >= 1 ? container.getItem(0) : ItemStack.EMPTY,
                container.size() >= 2 ? container.getItem(1) : ItemStack.EMPTY,
                container.size() >= 3 ? container.getItem(2) : ItemStack.EMPTY);

        if (recipe.matches(testFrame, level)) {
            return recipe.assemble(testFrame);
        }
        return ItemStack.EMPTY;
    }

    public boolean isItemValid(int gridSlot, AEItemKey key, World level) {
        if (key == null) {
            return false;
        }

        if (!canSubstitute) {
            if (gridSlot == TEMPLATE_CRAFTING_GRID_SLOT) {
                return template.equals(key);
            } else if (gridSlot == BASE_CRAFTING_GRID_SLOT) {
                return base.equals(key);
            } else if (gridSlot == ADDITION_CRAFTING_GRID_SLOT) {
                return addition.equals(key);
            } else {
                return false;
            }
        }

        // Fill frame and check result
        var testInput = switch (gridSlot) {
            case TEMPLATE_CRAFTING_GRID_SLOT -> new SmithingRecipeInput(
                    key.toStack(),
                    base.toStack(),
                    addition.toStack());
            case BASE_CRAFTING_GRID_SLOT -> new SmithingRecipeInput(
                    template.toStack(),
                    key.toStack(),
                    addition.toStack());
            case ADDITION_CRAFTING_GRID_SLOT -> new SmithingRecipeInput(
                    template.toStack(),
                    base.toStack(),
                    key.toStack());
            default -> null;
        };
        if (testInput == null) {
            return false;
        }

        return recipe.matches(testInput, level)
                && ItemStack.areItemStacksEqual(output, recipe.assemble(testInput));
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return slot == TEMPLATE_CRAFTING_GRID_SLOT || slot == BASE_CRAFTING_GRID_SLOT
                || slot == ADDITION_CRAFTING_GRID_SLOT;
    }

    @Override
    public void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor gridAccessor) {
        var entry = table[0].getFirstEntry();
        if (entry != null && entry.getKey() instanceof AEItemKey itemKey) {
            gridAccessor.set(TEMPLATE_CRAFTING_GRID_SLOT, itemKey.toStack());
            table[0].remove(entry.getKey(), 1);
        }

        entry = table[1].getFirstEntry();
        if (entry != null && entry.getKey() instanceof AEItemKey itemKey) {
            gridAccessor.set(BASE_CRAFTING_GRID_SLOT, itemKey.toStack());
            table[1].remove(entry.getKey(), 1);
        }

        entry = table[2].getFirstEntry();
        if (entry != null && entry.getKey() instanceof AEItemKey itemKey) {
            gridAccessor.set(ADDITION_CRAFTING_GRID_SLOT, itemKey.toStack());
            table[2].remove(entry.getKey(), 1);
        }
    }

    public static void encode(ItemStack stack, RecipeHolder<SmithingRecipe> recipe, AEItemKey template, AEItemKey base,
            AEItemKey addition,
            AEItemKey output, boolean allowSubstitutes) {
        Preconditions.checkNotNull(recipe, "recipe");
        Preconditions.checkNotNull(recipe, "template");
        Preconditions.checkNotNull(base, "base");
        Preconditions.checkNotNull(addition, "addition");
        Preconditions.checkNotNull(output, "output");

        AEComponents.ENCODED_SMITHING_TABLE_PATTERN.set(stack, new EncodedSmithingTablePattern(
                template.toStack(),
                base.toStack(),
                addition.toStack(),
                output.toStack(),
                allowSubstitutes,
                recipe.id()));
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

        var encodedPattern = AEComponents.ENCODED_SMITHING_TABLE_PATTERN.get(stack);
        if (encodedPattern != null) {
            tooltip.addInput(GenericStack.fromItemStack(encodedPattern.template()));
            tooltip.addInput(GenericStack.fromItemStack(encodedPattern.base()));
            tooltip.addInput(GenericStack.fromItemStack(encodedPattern.addition()));
            tooltip.addOutput(GenericStack.fromItemStack(encodedPattern.resultItem()));
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

    private class Input implements IInput {
        private final GenericStack[] possibleInputs;
        private final int gridSlot;

        private Input(AEItemKey what, Ingredient recipeIngredient, int gridSlot) {
            this.gridSlot = gridSlot;

            if (!canSubstitute) {
                this.possibleInputs = new GenericStack[] { new GenericStack(what, 1) };
            } else {
                ItemStack[] matchingStacks = recipeIngredient.getMatchingStacks();
                this.possibleInputs = new GenericStack[matchingStacks.length + 1];
                // Ensure that the stack chosen by the user gets precedence.
                this.possibleInputs[0] = new GenericStack(what, 1);
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
                return AESmithingTablePattern.this.isItemValid(gridSlot, itemKey, level);
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
