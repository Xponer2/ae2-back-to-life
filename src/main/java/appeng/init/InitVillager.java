package appeng.init;

import java.util.Random;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import net.minecraftforge.registries.IForgeRegistry;

import appeng.api.util.ItemLike;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;

/**
 * The fluix researcher villager. Villagers of this version have no work stations (points of interest) and no trade
 * experience: the profession is picked randomly like the ones of the game, and every trade level unlocks the next
 * group of offers.
 */
public class InitVillager {
    private InitVillager() {
    }

    public static final ResourceLocation ID = AppEng.makeId("fluix_researcher");

    /**
     * The skins are made at runtime from the villager textures of the game and the profession overlay of AE2 (see
     * {@code appeng.client.render.VillagerTextures}).
     */
    public static final ResourceLocation SKIN = AppEng.makeId("textures/entity/villager/fluix_researcher.png");
    public static final ResourceLocation ZOMBIE_SKIN = AppEng
            .makeId("textures/entity/zombie_villager/fluix_researcher.png");

    // Uses of the item-for-emerald offers of newer versions
    private static final int ITEMS_FOR_EMERALDS_MAX_USES = 12;

    public static final VillagerProfession PROFESSION = new VillagerProfession(ID.toString(), SKIN.toString(),
            ZOMBIE_SKIN.toString());

    public static void initProfession(IForgeRegistry<VillagerProfession> registry) {
        // The career is named like the profession: the villager is called "entity.Villager.fluix_researcher"
        var career = new VillagerCareer(PROFESSION, ID.getPath());
        initTrades(career);
        registry.register(PROFESSION);
    }

    private static void initTrades(VillagerCareer career) {
        buyItems(career, 1, AEItems.CERTUS_QUARTZ_CRYSTAL, 3, 4, 10);
        buyItems(career, 1, AEItems.METEORITE_COMPASS, 2, 1, 5);

        sellItems(career, 2, AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED, 3, 10, 15);
        sellItems(career, 2, AEItems.SILICON, 5, 8, 13);
        buyItems(career, 2, AEBlocks.SKY_STONE_BLOCK, 5, 8, 20);

        sellItems(career, 3, AEBlocks.QUARTZ_GLASS, 2, 10, 10);
        buyItems(career, 3, AEItems.FLUIX_CRYSTAL, 5, 4, 14);

        sellItems(career, 4, AEItems.MATTER_BALL, 5, 8, 12);
        buyItems(career, 4, AEItems.CALCULATION_PROCESSOR_PRESS, 10, 1, 20);
        buyItems(career, 4, AEItems.ENGINEERING_PROCESSOR_PRESS, 10, 1, 20);
        buyItems(career, 4, AEItems.LOGIC_PROCESSOR_PRESS, 10, 1, 20);
        buyItems(career, 4, AEItems.SILICON_PRESS, 10, 1, 20);

        buyItems(career, 5, () -> Items.SLIME_BALL, 8, 5, 12);
    }

    /**
     * The player sells items to the villager for one emerald. The experience of the trade (xp) has no use in this
     * version, it is kept to show the same offers as newer versions.
     */
    private static void sellItems(VillagerCareer career, int minLevel, ItemLike soldItem, int numberOfItems,
            int maxUses, int xp) {
        career.addTrade(minLevel, new FixedTrade(
                () -> new ItemStack(soldItem.asItem(), numberOfItems),
                () -> new ItemStack(Items.EMERALD),
                maxUses));
    }

    /**
     * The player buys items from the villager for emeralds.
     */
    private static void buyItems(VillagerCareer career, int minLevel, ItemLike boughtItem, int emeraldCost,
            int numberOfItems, int xp) {
        career.addTrade(minLevel, new FixedTrade(
                () -> new ItemStack(Items.EMERALD, emeraldCost),
                () -> new ItemStack(boughtItem.asItem(), numberOfItems),
                ITEMS_FOR_EMERALDS_MAX_USES));
    }

    @FunctionalInterface
    private interface StackFactory {
        ItemStack create();
    }

    /**
     * An offer with fixed amounts. The stacks are created when a villager gets the offer, after all items exist.
     */
    private static final class FixedTrade implements EntityVillager.ITradeList {
        private final StackFactory buy;
        private final StackFactory sell;
        private final int maxUses;

        private FixedTrade(StackFactory buy, StackFactory sell, int maxUses) {
            this.buy = buy;
            this.sell = sell;
            this.maxUses = maxUses;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random) {
            recipeList.add(new MerchantRecipe(buy.create(), ItemStack.EMPTY, sell.create(), 0, maxUses));
        }
    }
}
