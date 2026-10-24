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

package appeng.menu.slot;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;

import appeng.api.inventories.InternalInventory;
import appeng.util.CraftingInput;
import appeng.util.RecipeHolder;
import appeng.util.inv.AppEngInternalInventory;

public class AppEngCraftingSlot extends AppEngSlot {

    /**
     * The craft matrix inventory linked to this result slot.
     */
    private final InternalInventory craftingGrid;

    /**
     * The player that is using the GUI where this slot resides.
     */
    private final EntityPlayer player;

    /**
     * The number of items that have been crafted so far. Gets passed to ItemStack.onCrafting before being reset.
     */
    private int amountCrafted;

    @Nullable
    private RecipeHolder<?> recipeUsed;

    public AppEngCraftingSlot(EntityPlayer player, InternalInventory craftingGrid) {
        super(new AppEngInternalInventory(1), 0);
        this.player = player;
        this.craftingGrid = craftingGrid;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /**
     * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood. Typically increases an
     * internal count then calls onCrafting(item).
     */
    @Override
    protected final void onCrafting(ItemStack stack, int amount) {
        onQuickCraft(stack, amount);
    }

    @Override
    protected final void onCrafting(ItemStack stack) {
        checkTakeAchievements(stack);
    }

    protected void onQuickCraft(ItemStack stack, int amount) {
        this.amountCrafted += amount;
        this.checkTakeAchievements(stack);
    }

    /**
     * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood. This replicates Vanilla
     * {@link net.minecraft.world.inventory.ResultSlot}.
     */
    protected void checkTakeAchievements(ItemStack stack) {
        var craftContainer = craftingGrid.toContainer();

        if (amountCrafted > 0) {
            stack.onCrafting(this.player.world, this.player, this.amountCrafted);
            FMLCommonHandler.instance().firePlayerCraftingEvent(this.player, stack, craftContainer);
        }

        // 1.21 RecipeCraftingHolder.awardUsedRecipes
        if (recipeUsed != null && recipeUsed.value() instanceof IRecipe recipe && !recipe.isDynamic()) {
            this.player.unlockRecipes(Lists.newArrayList(recipe));
        }

        this.amountCrafted = 0;
    }

    @Override
    public ItemStack onTake(EntityPlayer player, ItemStack stack) {
        this.amountCrafted += stack.getCount();
        this.checkTakeAchievements(stack);

        var items = NonNullList.withSize(this.craftingGrid.size(), ItemStack.EMPTY);
        for (int i = 0; i < this.craftingGrid.size(); i++) {
            items.set(i, this.craftingGrid.getStackInSlot(i));
        }
        var positioned = CraftingInput.ofPositioned(3, 3, items);

        ForgeHooks.setCraftingPlayer(player);
        var remainingItems = this.getRemainingItems(positioned.input(), player.world);
        ForgeHooks.setCraftingPlayer(null);

        for (var y = 0; y < 3; y++) {
            for (var x = 0; x < 3; x++) {
                var slotIdx = y * 3 + x;
                var remainderIdx = (y - positioned.top()) * 3 + (x - positioned.left());

                // Consumes the item from the grid
                this.craftingGrid.extractItem(slotIdx, 1, false);

                if (remainderIdx >= 0 && remainderIdx < remainingItems.size()) {
                    var remainingInSlot = remainingItems.get(remainderIdx);
                    if (!remainingInSlot.isEmpty()) {
                        if (this.craftingGrid.getStackInSlot(slotIdx).isEmpty()) {
                            this.craftingGrid.setItemDirect(slotIdx, remainingInSlot);
                        } else if (!this.player.inventory.addItemStackToInventory(remainingInSlot)) {
                            this.player.dropItem(remainingInSlot, false);
                        }
                    }
                }
            }
        }
        return stack;
    }

    /**
     * Overrides what is being shown as the crafting output, but doesn't notify parent menu.
     */
    public void setDisplayedCraftingOutput(ItemStack stack) {
        getInventory().setItemDirect(0, stack);
    }

    /**
     * Decrease the size of the stack in slot (first int arg) by the amount of the second int arg. Returns the new
     * stack.
     */
    @Override
    public ItemStack remove(int par1) {
        if (this.hasItem()) {
            this.amountCrafted += Math.min(par1, this.getItem().getCount());
        }

        return super.remove(par1);
    }

    // TODO: This is really hacky and NEEDS to be solved with a full menu/gui
    // refactoring.
    protected NonNullList<ItemStack> getRemainingItems(CraftingInput ic, World level) {
        var recipe = ic.findRecipe(level);
        return recipe != null ? ic.getRemainingItems(recipe.value()) : NonNullList.withSize(9, ItemStack.EMPTY);
    }

    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        this.recipeUsed = recipe;
    }

    @Nullable
    public RecipeHolder<?> getRecipeUsed() {
        return recipeUsed;
    }
}
