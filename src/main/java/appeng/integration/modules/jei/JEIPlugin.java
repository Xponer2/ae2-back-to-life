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

package appeng.integration.modules.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

import appeng.api.config.Actionable;
import appeng.api.config.CondenserOutput;
import appeng.api.features.P2PTunnelAttunementInternal;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.InscriberScreen;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.FacadeCreativeTab;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.ItemDefinition;
import appeng.core.localization.GuiText;
import appeng.core.localization.ItemModText;
import appeng.core.tags.TagManager;
import appeng.integration.modules.itemlists.ItemPredicates;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.recipes.AERecipeTypes;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.WirelessCraftingTermMenu;
import appeng.recipes.RecipeManager;
import appeng.recipes.game.SmithingCraftingRecipe;
import appeng.recipes.game.StorageCellUpgradeRecipe;
import appeng.recipes.quartzcutting.QuartzCuttingRecipe;
import appeng.recipes.vanilla.VanillaRecipes;

/**
 * 1.12.2 only: AE2 for 1.21 has plugins for REI and EMI; this JEI 4.16 plugin shows the same categories (recipes are
 * read from the AE2 recipe manager, which is filled from the recipe JSON at startup on both sides).
 */
@mezz.jei.api.JEIPlugin
public class JEIPlugin implements IModPlugin {

    static final ResourceLocation TEXTURE = AppEng.makeId("textures/guis/jei.png");

    private IIngredientRegistry ingredientRegistry;

    @Override
    public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
        // Every facade is its own entry
        subtypeRegistry.useNbtForSubtypes(AEItems.FACADE.asItem());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        var guiHelper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(
                new CondenserCategory(guiHelper),
                new InscriberRecipeCategory(guiHelper),
                new ChargerCategory(guiHelper),
                new AttunementCategory(guiHelper),
                new TransformCategory(guiHelper),
                new EntropyCategory(guiHelper),
                new StonecuttingCategory(guiHelper));
    }

    @Override
    public void register(IModRegistry registry) {
        var guiHelper = registry.getJeiHelpers().getGuiHelper();
        var recipes = RecipeManager.get();

        // Inscriber
        registry.addRecipes(recipes.byType(AERecipeTypes.INSCRIBER).stream()
                .map(holder -> new InscriberRecipeWrapper(holder.value()))
                .collect(Collectors.toList()), InscriberRecipeCategory.UID);
        registry.addRecipeCatalyst(AEBlocks.INSCRIBER.stack(), InscriberRecipeCategory.UID);
        registry.addRecipeClickArea(InscriberScreen.class, 82, 39, 26, 16, InscriberRecipeCategory.UID);

        // Charger
        registry.addRecipes(recipes.byType(AERecipeTypes.CHARGER).stream()
                .map(holder -> new ChargerRecipeWrapper(holder.value()))
                .collect(Collectors.toList()), ChargerCategory.UID);
        registry.addRecipeCatalyst(AEBlocks.CHARGER.stack(), ChargerCategory.UID);
        registry.addRecipeCatalyst(AEBlocks.CRANK.stack(), ChargerCategory.UID);

        // In-world transformations (throwing items into a fluid, explosions)
        registry.addRecipes(recipes.byType(AERecipeTypes.TRANSFORM).stream()
                .map(holder -> new TransformRecipeWrapper(holder.value()))
                .collect(Collectors.toList()), TransformCategory.UID);

        // Entropy manipulator
        registry.addRecipes(recipes.byType(AERecipeTypes.ENTROPY).stream()
                .map(holder -> new EntropyRecipeWrapper(guiHelper, holder.value()))
                .collect(Collectors.toList()), EntropyCategory.UID);
        registry.addRecipeCatalyst(chargeFully(AEItems.ENTROPY_MANIPULATOR.stack()), EntropyCategory.UID);

        // Stonecutting (only usable through stonecutting patterns in 1.12.2)
        registry.addRecipes(VanillaRecipes.getAllStonecutting().stream()
                .map(StonecuttingRecipeWrapper::new)
                .collect(Collectors.toList()), StonecuttingCategory.UID);
        registry.addRecipeCatalyst(AEParts.PATTERN_ENCODING_TERMINAL.stack(), StonecuttingCategory.UID);
        registry.addRecipeCatalyst(AEBlocks.MOLECULAR_ASSEMBLER.stack(), StonecuttingCategory.UID);

        // Condenser
        registry.addRecipes(Arrays.asList(
                new CondenserOutputWrapper(guiHelper, CondenserOutput.MATTER_BALLS),
                new CondenserOutputWrapper(guiHelper, CondenserOutput.SINGULARITY)), CondenserCategory.UID);
        registry.addRecipeCatalyst(AEBlocks.CONDENSER.stack(), CondenserCategory.UID);

        // The crafting terminals craft like a crafting table
        registry.addRecipeCatalyst(AEParts.CRAFTING_TERMINAL.stack(), VanillaRecipeCategoryUid.CRAFTING);
        registry.addRecipeCatalyst(AEItems.WIRELESS_CRAFTING_TERMINAL.stack(), VanillaRecipeCategoryUid.CRAFTING);

        registerDescriptions(registry);
        registerAttunements(registry);
        registerStorageCellUpgrades(registry);

        // AE2's own crafting recipe classes in the recipe registry
        registry.handleRecipes(QuartzCuttingRecipe.class, CustomCraftingRecipeWrapper::new,
                VanillaRecipeCategoryUid.CRAFTING);
        registry.handleRecipes(SmithingCraftingRecipe.class, CustomCraftingRecipeWrapper::new,
                VanillaRecipeCategoryUid.CRAFTING);

        if (AEConfig.instance().isEnableFacadeRecipesInRecipeViewer()) {
            registry.addRecipeRegistryPlugin(
                    new FacadeRegistryPlugin(AEItems.FACADE.asItem(), AEParts.CABLE_ANCHOR.stack()));
        }

        // Allow recipe transfer from JEI to the crafting terminals and the pattern encoding terminal
        var transfers = registry.getRecipeTransferRegistry();
        var transferHelper = registry.getJeiHelpers().recipeTransferHandlerHelper();
        transfers.addRecipeTransferHandler(
                new CraftingTermTransferHandler<>(CraftingTermMenu.class, transferHelper),
                VanillaRecipeCategoryUid.CRAFTING);
        transfers.addRecipeTransferHandler(
                new CraftingTermTransferHandler<>(WirelessCraftingTermMenu.class, transferHelper),
                VanillaRecipeCategoryUid.CRAFTING);
        transfers.addUniversalRecipeTransferHandler(new PatternEncodingTransferHandler());

        // Drag and drop into filter slots, exclusion zones and the stack under the mouse
        registry.addGhostIngredientHandler(AEBaseScreen.class, new GhostIngredientHandler());
        registry.addAdvancedGuiHandlers(new AdvancedGuiHandler());

        hideItems(registry);
        this.ingredientRegistry = registry.getIngredientRegistry();
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        // FacadeItem has no sub items of its own, the facades of all blocks come from the facade creative tab
        if (ingredientRegistry != null && AEConfig.instance().isEnableFacadesInRecipeViewer()) {
            var facades = FacadeCreativeTab.getDisplayItems();
            if (!facades.isEmpty()) {
                ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, facades);
            }
        }
    }

    private static void registerAttunements(IModRegistry registry) {
        var stackHelper = registry.getJeiHelpers().getStackHelper();
        var allItems = registry.getIngredientRegistry().getAllIngredients(VanillaTypes.ITEM);
        var wrappers = new ArrayList<AttunementWrapper>();

        for (var entry : P2PTunnelAttunementInternal.getApiTunnels()) {
            var inputs = allItems.stream().filter(entry.stackPredicate()).collect(Collectors.toList());
            if (inputs.isEmpty()) {
                continue;
            }
            wrappers.add(new AttunementWrapper(inputs, new ItemStack(entry.tunnelType()),
                    Arrays.asList(ItemModText.P2P_API_ATTUNEMENT.text(), entry.description())));
        }

        for (var entry : P2PTunnelAttunementInternal.getTagTunnels().entrySet()) {
            List<ItemStack> inputs = new ArrayList<>();
            for (var stack : TagManager.getItems(entry.getKey().location())) {
                if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) {
                    inputs.addAll(stackHelper.getSubtypes(stack));
                } else {
                    inputs.add(stack);
                }
            }
            if (inputs.isEmpty()) {
                continue;
            }
            wrappers.add(new AttunementWrapper(inputs, new ItemStack(entry.getValue()),
                    Collections.singletonList(ItemModText.P2P_TAG_ATTUNEMENT.text())));
        }

        registry.addRecipes(wrappers, AttunementCategory.UID);
    }

    /**
     * The upgrade recipes are special crafting recipes JEI can't show by itself. They are looked up in the crafting
     * recipes of Forge and in the recipe manager of AE2.
     */
    private static void registerStorageCellUpgrades(IModRegistry registry) {
        var upgrades = new ArrayList<StorageCellUpgradeRecipe>();
        for (var recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            if ((Object) recipe instanceof StorageCellUpgradeRecipe upgrade) {
                upgrades.add(upgrade);
            }
        }
        for (var holder : RecipeManager.get().getRecipes()) {
            if (holder.value() instanceof StorageCellUpgradeRecipe upgrade && !upgrades.contains(upgrade)) {
                upgrades.add(upgrade);
            }
        }
        registry.addRecipes(upgrades.stream().map(StorageCellUpgradeWrapper::new).collect(Collectors.toList()),
                VanillaRecipeCategoryUid.CRAFTING);
    }

    private static ItemStack chargeFully(ItemStack stack) {
        if (stack.getItem() instanceof AEBasePoweredItem poweredItem) {
            poweredItem.injectAEPower(stack, poweredItem.getAEMaxPower(stack), Actionable.MODULATE);
        }
        return stack;
    }

    private static void hideItems(IModRegistry registry) {
        var blacklist = registry.getJeiHelpers().getIngredientBlacklist();
        for (var stack : registry.getIngredientRegistry().getAllIngredients(VanillaTypes.ITEM)) {
            if (ItemPredicates.shouldBeHidden(stack)) {
                blacklist.addIngredientToBlacklist(stack);
            }
        }
    }

    private void registerDescriptions(IModRegistry registry) {
        addDescription(registry, AEItems.CERTUS_QUARTZ_CRYSTAL, GuiText.CertusQuartzObtain.getTranslationKey());

        if (AEConfig.instance().isSpawnPressesInMeteoritesEnabled()) {
            for (var press : Arrays.asList(AEItems.LOGIC_PROCESSOR_PRESS, AEItems.CALCULATION_PROCESSOR_PRESS,
                    AEItems.ENGINEERING_PROCESSOR_PRESS, AEItems.SILICON_PRESS)) {
                addDescription(registry, press, GuiText.inWorldCraftingPresses.getTranslationKey());
            }
        }

        addDescription(registry, AEBlocks.CRANK.item(), ItemModText.CRANK_DESCRIPTION.getTranslationKey());
    }

    private static void addDescription(IModRegistry registry, ItemDefinition<?> itemDefinition,
            String... messageKeys) {
        // JEI translates the keys itself
        registry.addIngredientInfo(itemDefinition.stack(), VanillaTypes.ITEM, messageKeys);
    }
}
