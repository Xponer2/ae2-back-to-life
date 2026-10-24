package appeng.recipes.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.AERecipeTypes;
import appeng.util.AECodecs;
import appeng.util.BuiltInRegistries;

/**
 * Used to handle upgrading and removal of upgrades for crafting units (in-world).
 */
public class CraftingUnitTransformRecipe extends CustomRecipe {
    public static final MapCodec<CraftingUnitTransformRecipe> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder
            .group(
                    AECodecs.BLOCK.fieldOf("upgraded_block")
                            .forGetter(CraftingUnitTransformRecipe::getUpgradedBlock),
                    AECodecs.ITEM.fieldOf("upgrade_item")
                            .forGetter(CraftingUnitTransformRecipe::getUpgradeItem))
            .apply(builder, CraftingUnitTransformRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingUnitTransformRecipe> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.registry(BuiltInRegistries.BLOCK.key()),
                    CraftingUnitTransformRecipe::getUpgradedBlock,
                    ByteBufCodecs.registry(BuiltInRegistries.ITEM.key()),
                    CraftingUnitTransformRecipe::getUpgradeItem,
                    CraftingUnitTransformRecipe::new);

    private final Block upgradedBlock;
    private final Item upgradeItem;

    public CraftingUnitTransformRecipe(Block upgradedBlock, Item upgradeItem) {
        super(CraftingBookCategory.MISC);
        this.upgradedBlock = upgradedBlock;
        this.upgradeItem = upgradeItem;
    }

    public Block getUpgradedBlock() {
        return this.upgradedBlock;
    }

    public Item getUpgradeItem() {
        return upgradeItem;
    }

    /**
     * Gets the upgrade that would be returned from removing an upgrade from the given crafting block.
     *
     * @return Empty stack if no upgrade removal is possible.
     */
    public static ItemStack getRemovedUpgrade(World level, Block upgradedBlock) {
        var recipeManager = level.getRecipeManager();

        for (var holder : recipeManager.byType(AERecipeTypes.CRAFTING_UNIT_TRANSFORM)) {
            if (holder.value().upgradedBlock == upgradedBlock) {
                return holder.value().upgradeItem.getDefaultInstance();
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Search for the resulting upgraded block when upgrading a crafting unit with the given upgrade item.
     */
    public static Block getUpgradedBlock(World level, ItemStack upgradeItem) {
        for (var holder : level.getRecipeManager().byType(AERecipeTypes.CRAFTING_UNIT_TRANSFORM)) {
            if (upgradeItem.is(holder.value().getUpgradeItem())) {
                return holder.value().upgradedBlock;
            }
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput input, World level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
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
        return CraftingUnitTransformRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return AERecipeTypes.CRAFTING_UNIT_TRANSFORM;
    }
}
