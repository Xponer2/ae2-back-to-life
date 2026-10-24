package appeng.api.upgrades;

import appeng.util.Platform;

import appeng.util.ItemProperties;

import appeng.util.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.nio.sctp.Association;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import appeng.api.util.ItemLike;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.items.materials.EnergyCardItem;
import appeng.items.materials.UpgradeCardItem;

/**
 * Manages available upgrades for AE machines, parts and items.
 */
public final class Upgrades {
    // Key is the upgrade cards item
    private static final Map<Item, List<Association>> ASSOCIATIONS = new IdentityHashMap<>();
    // Keeps track of which upgrades an item supports
    private static final Map<IUpgradeableItem, Set<Item>> SUPPORTED_ITEM_UPGRADES = new IdentityHashMap<>();
    // Key is the upgrade cards item
    private static final Map<Item, List<ITextComponent>> UPGRADE_CARD_TOOLTIP_LINES = new IdentityHashMap<>();

    private Upgrades() {
    }

    /**
     * Same as {@link #add(ItemLike, ItemLike, int, String)}, but without a tooltip group.
     */
    public static synchronized void add(ItemLike upgradeCard, ItemLike upgradableObject, int maxSupported) {
        add(upgradeCard, upgradableObject, maxSupported, null);
    }

    /**
     * Associates an upgrade card item with an upgradable object and registers the maximum number of this upgrade that
     * the upgradable object supports.
     *
     * @param tooltipGroup If not null, the upgradable object will be shown using this translation key in the tooltip of
     *                     the upgrade card, grouped together with all other upgradable objects using the same
     *                     translation key.
     */
    public static synchronized void add(ItemLike upgradeCard, ItemLike upgradableObject, int maxSupported,
            @Nullable String tooltipGroup) {
        Item item = upgradableObject.asItem();
        Block block;
        if (item instanceof ItemBlock blockItem) {
            block = blockItem.getBlock();
        } else {
            block = null;
        }

        var upgrade = upgradeCard.asItem();

        // Collect all supported upgrades for upgradable items
        if (item instanceof IUpgradeableItem upgradeableItem) {
            var upgrades = SUPPORTED_ITEM_UPGRADES.get(upgradeableItem);
            if (upgrades == null) {
                SUPPORTED_ITEM_UPGRADES.put(upgradeableItem, ImmutableSet.of(upgrade));
            } else {
                var newSet = new HashSet<>(upgrades);
                newSet.add(upgrade);
                SUPPORTED_ITEM_UPGRADES.put(upgradeableItem, ImmutableSet.copyOf(newSet));
            }
        }

        var translatedTooltipGroup = tooltipGroup != null ? new TextComponentTranslation(tooltipGroup) : null;

        var association = new Association(upgrade, item, block, maxSupported, translatedTooltipGroup);
        ASSOCIATIONS.computeIfAbsent(association.upgradeCard(), ignored -> new ArrayList<>())
                .add(association);
        // Clear tooltip cache
        UPGRADE_CARD_TOOLTIP_LINES.remove(association.upgradeCard());
    }

    /**
     * Returns how many of the given item can be installed in the given upgradable item. Returns 0 if none are allowed.
     */
    public static synchronized int getMaxInstallable(ItemLike card, ItemLike upgradableItem) {
        var associations = ASSOCIATIONS.get(card.asItem());
        if (associations == null) {
            return 0;
        }

        for (var association : associations) {
            if (association.upgradableItem() == upgradableItem.asItem()) {
                return association.maxCount();
            }
        }

        return 0;
    }

    /**
     * Returns a map from an upgradable item to the upgrades it supports.
     */
    public static synchronized Map<IUpgradeableItem, Set<Item>> getUpgradableItems() {
        return ImmutableMap.copyOf(SUPPORTED_ITEM_UPGRADES);
    }

    /**
     * Returns a cumulative energy multiplier based on the amount of "energy cards" fitted onto a tool. Returns 0 if no
     * such cards exist within the tool's upgrade inventory.
     */
    public static int getEnergyCardMultiplier(IUpgradeInventory upgrades) {
        int multiplier = 0;
        for (var card : upgrades) {
            if (card.getItem() instanceof EnergyCardItem ec) {
                multiplier += ec.getEnergyMultiplier();
            }
        }
        return multiplier;
    }

    /**
     * Creates a new upgrade item which can be used to receive automated tooltips and allow custom upgrades to be added
     * to AE2's toolbelt in the network tool.
     */
    public static Item createUpgradeCardItem(ItemProperties p) {
        return new UpgradeCardItem(p);
    }

    /**
     * Checks if the given item is an upgrade card for *any* other object.
     */
    public static boolean isUpgradeCardItem(ItemLike card) {
        return card.asItem() instanceof UpgradeCardItem;
    }

    /**
     * Checks if the given item stack uses an item created using {@link #createUpgradeCardItem}.
     *
     * @see #isUpgradeCardItem(ItemLike)
     */
    public static boolean isUpgradeCardItem(ItemStack stack) {
        return stack.getItem() instanceof UpgradeCardItem;
    }

    /**
     * Gets a list of lines describing where an upgrade card can be used.
     */
    public static synchronized List<ITextComponent> getTooltipLinesForCard(ItemLike card) {
        return UPGRADE_CARD_TOOLTIP_LINES.computeIfAbsent(card.asItem(), Upgrades::createTooltipLinesForCard);
    }

    /**
     * Gets a list of tooltip lines describing which upgrades and how many of each are supported by a given upgradable
     * item. Returns an empty list if there are no upgrades available for this item.
     */
    public static synchronized List<ITextComponent> getTooltipLinesForMachine(ItemLike upgradableItemLike) {
        var upgradableItem = upgradableItemLike.asItem();

        var result = new ArrayList<ITextComponent>();

        for (var cardAssociations : ASSOCIATIONS.values()) {
            for (var association : cardAssociations) {
                if (association.upgradableItem() == upgradableItem) {
                    result.add(TextUtil.withStyle(GuiText.CompatibleUpgrade
                            .text(Platform.getItemDescription(association.upgradeCard()), association.maxCount()), TextFormatting.GRAY));
                    break;
                }
            }
        }

        return result;
    }

    private final static class Association {
        private final Item upgradeCard;
        private final Item upgradableItem;
        private final Block upgradableBlock;
        private final int maxCount;
        private final ITextComponent tooltipGroup;

        public Association(Item upgradeCard, Item upgradableItem, @Nullable Block upgradableBlock, int maxCount, @Nullable ITextComponent tooltipGroup) {
            this.upgradeCard = upgradeCard;
            this.upgradableItem = upgradableItem;
            this.upgradableBlock = upgradableBlock;
            this.maxCount = maxCount;
            this.tooltipGroup = tooltipGroup;
        }

        public Item upgradeCard() {
            return upgradeCard;
        }

        public Item upgradableItem() {
            return upgradableItem;
        }

        @Nullable public Block upgradableBlock() {
            return upgradableBlock;
        }

        public int maxCount() {
            return maxCount;
        }

        @Nullable public ITextComponent tooltipGroup() {
            return tooltipGroup;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Association)) return false;
            Association that = (Association) o;
            return java.util.Objects.equals(upgradeCard, that.upgradeCard)
                    && java.util.Objects.equals(upgradableItem, that.upgradableItem)
                    && java.util.Objects.equals(upgradableBlock, that.upgradableBlock)
                    && maxCount == that.maxCount
                    && java.util.Objects.equals(tooltipGroup, that.tooltipGroup);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(upgradeCard, upgradableItem, upgradableBlock, maxCount, tooltipGroup);
        }

        @Override
        public String toString() {
            return "Association[" + "upgradeCard=" + upgradeCard + ", " + "upgradableItem=" + upgradableItem + ", " + "upgradableBlock=" + upgradableBlock + ", " + "maxCount=" + maxCount + ", " + "tooltipGroup=" + tooltipGroup + "]";
        }

    }

    private static List<ITextComponent> createTooltipLinesForCard(Item card) {
        var associations = new ArrayList<>(ASSOCIATIONS.getOrDefault(card, Collections.emptyList()));
        associations.sort(Comparator.comparingInt(o -> o.maxCount));
        var supportedTooltipLines = new ArrayList<ITextComponent>(associations.size());

        // Use a separate set because the final text will include numbers
        Set<ITextComponent> namesAdded = new HashSet<>();

        for (int i = 0; i < associations.size(); i++) {
            var association = associations.get(i);
            ITextComponent name = Platform.getItemDescription(association.upgradableItem());

            // If the group was already added by a previous item, skip this
            if (association.tooltipGroup() != null && namesAdded.contains(association.tooltipGroup())) {
                continue;
            }

            // If any of the following items would be of the same group, use the group name
            // instead
            if (association.tooltipGroup() != null) {
                for (int j = i + 1; j < associations.size(); j++) {
                    var otherGroup = associations.get(j).tooltipGroup();
                    if (association.tooltipGroup().equals(otherGroup)) {
                        name = association.tooltipGroup();
                        break;
                    }
                }
            }

            if (namesAdded.add(name)) {
                // append the supported count only if its > 1
                ITextComponent base = (TextUtil.withStyle(name.createCopy(), Tooltips.NORMAL_TOOLTIP_TEXT));
                ITextComponent main = base;

                if (association.maxCount() > 1) {
                    main = Tooltips.of(base, Tooltips.of(" ("),
                            Tooltips.ofUnformattedNumber(association.maxCount()), Tooltips.of(")"));

                }
                supportedTooltipLines.add(main);
            }
        }

        return supportedTooltipLines;
    }

}
