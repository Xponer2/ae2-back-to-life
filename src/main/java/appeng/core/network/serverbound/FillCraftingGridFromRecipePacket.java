
package appeng.core.network.serverbound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.helpers.ICraftingGridMenu;
import appeng.items.storage.ViewCellItem;
import appeng.me.storage.NullInventory;
import appeng.util.CraftingRecipeUtil;
import appeng.util.prioritylist.IPartitionList;

/**
 * This packet will attempt to fill a crafting grid with real items based on a crafting recipe for the purposes of
 * crafting that item.
 *
 * @param recipeId            The Recipe ID to be crafted. This is optional since some recipes in JEI/REI are generated
 *                            on-the-fly and do not really exist.
 * @param ingredientTemplates The client sends us the intended items for the crafting based on the recipe shown to them.
 *                            This can be used in place of the actual ingredients in case the recipe is not resolvable.
 *                            <p/>
 *                            The size of this list *has* to match the size and layout of the crafting matrix used in
 *                            the menu we're trying to fill the matrix in. Usually it'll be 3x3.
 * @param craftMissing        True if missing entries should be queued for autocrafting instead.
 */
public final class FillCraftingGridFromRecipePacket implements ServerboundPacket {
    private final ResourceLocation recipeId;
    private final NonNullList<ItemStack> ingredientTemplates;
    private final boolean craftMissing;

    @Nullable public ResourceLocation recipeId() {
        return recipeId;
    }

    public NonNullList<ItemStack> ingredientTemplates() {
        return ingredientTemplates;
    }

    public boolean craftMissing() {
        return craftMissing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FillCraftingGridFromRecipePacket)) return false;
        FillCraftingGridFromRecipePacket that = (FillCraftingGridFromRecipePacket) o;
        return java.util.Objects.equals(recipeId, that.recipeId)
                && java.util.Objects.equals(ingredientTemplates, that.ingredientTemplates)
                && craftMissing == that.craftMissing;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(recipeId, ingredientTemplates, craftMissing);
    }

    @Override
    public String toString() {
        return "FillCraftingGridFromRecipePacket[" + "recipeId=" + recipeId + ", " + "ingredientTemplates=" + ingredientTemplates + ", " + "craftMissing=" + craftMissing + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, FillCraftingGridFromRecipePacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    FillCraftingGridFromRecipePacket::write,
                    FillCraftingGridFromRecipePacket::decode);

    public static final Type<FillCraftingGridFromRecipePacket> TYPE = CustomAppEngPayload
            .createType("fill_crafting_grid_from_recipe");

    @Override
    public Type<FillCraftingGridFromRecipePacket> type() {
        return TYPE;
    }

    public FillCraftingGridFromRecipePacket(@Nullable ResourceLocation recipeId,
            NonNullList<ItemStack> ingredientTemplates,
            boolean craftMissing) {
        this.recipeId = recipeId;
        this.ingredientTemplates = NonNullList.withSize(ingredientTemplates.size(), ItemStack.EMPTY);
        for (int i = 0; i < ingredientTemplates.size(); i++) {
            this.ingredientTemplates.set(i, ingredientTemplates.get(i).copy());
        }
        this.craftMissing = craftMissing;
    }

    public static FillCraftingGridFromRecipePacket decode(RegistryFriendlyByteBuf stream) {
        ResourceLocation recipeId = null;
        if (stream.readBoolean()) {
            recipeId = stream.readResourceLocation();
        }

        var ingredientTemplates = NonNullList.withSize(stream.readInt(), ItemStack.EMPTY);
        for (int i = 0; i < ingredientTemplates.size(); i++) {
            ingredientTemplates.set(i, ByteBufCodecs.OPTIONAL_ITEM_STACK.decode(stream));
        }
        var craftMissing = stream.readBoolean();

        return new FillCraftingGridFromRecipePacket(recipeId, ingredientTemplates, craftMissing);
    }

    public void write(RegistryFriendlyByteBuf data) {
        if (recipeId != null) {
            data.writeBoolean(true);
            data.writeResourceLocation(recipeId);
        } else {
            data.writeBoolean(false);
        }
        data.writeInt(ingredientTemplates.size());
        for (var stack : ingredientTemplates) {
            ByteBufCodecs.OPTIONAL_ITEM_STACK.encode(data, stack);
        }
        data.writeBoolean(craftMissing);
    }

    /**
     * Serverside handler for this packet.
     */
    @Override
    public void handleOnServer(EntityPlayerMP player) {
        // Setup and verification
        var menu = player.openContainer;
        if (!(menu instanceof ICraftingGridMenu cct)) {
            // Server might have closed the menu before the client-packet is processed. This is not an error.
            return;
        }

        var energy = cct.getEnergySource();
        @Nullable
        ICraftingService craftingService;
        @Nullable
        IStorageService storageService;
        MEStorage networkStorage;
        KeyCounter cachedStorage;

        @Nullable
        var node = cct.getGridNode();
        if (node != null && cct.getLinkStatus().connected()) {
            craftingService = node.getGrid().getCraftingService();
            storageService = node.getGrid().getStorageService();
            networkStorage = storageService.getInventory();
            cachedStorage = storageService.getCachedInventory();
        } else {
            craftingService = null;
            storageService = null;
            networkStorage = NullInventory.of();
            cachedStorage = new KeyCounter();
        }

        var craftMatrix = cct.getCraftingMatrix();

        // We'll try to use the best possible ingredients based on what's available in the network

        var filter = ViewCellItem.createItemFilter(cct.getViewCells());
        var ingredients = getDesiredIngredients(player);

        // Prepare to autocraft some stuff
        var toAutoCraft = new LinkedHashMap<AEItemKey, IntList>();
        boolean touchedGridStorage = false;

        // Take out all items that do not match the ingredient of their current slot. We hold on to them
        // instead of returning them to storage right away, since they may match the ingredient of another slot.
        // Returning them first and then extracting them again would fail, since the cached inventory we use to find
        // the ingredients is only updated once per tick.
        var displacedItems = new ArrayList<DisplacedItem>();
        for (var x = 0; x < craftMatrix.size(); x++) {
            var currentItem = craftMatrix.getStackInSlot(x);
            if (!currentItem.isEmpty() && !ingredients.get(x).test(currentItem)) {
                displacedItems.add(new DisplacedItem(x, currentItem.copy()));
                craftMatrix.setItemDirect(x, ItemStack.EMPTY);
            }
        }

        // Handle each slot
        for (var x = 0; x < craftMatrix.size(); x++) {
            var currentItem = craftMatrix.getStackInSlot(x);
            var ingredient = ingredients.get(x);

            if (!currentItem.isEmpty() || ingredient.getMatchingStacks().length == 0) {
                // Grid already has an item that matches the ingredient, or the slot should stay empty
                continue;
            }

            // Prefer items that were previously in the grid, but in the wrong slot
            currentItem = takeIngredientFromDisplacedItems(displacedItems, ingredient);

            // Try to find the best item for this slot. Sort by the amount available in the last tick,
            // then try to extract from most to least available item until 1 can be extracted.
            if (currentItem.isEmpty()) {
                var request = findBestMatchingItemStack(ingredient, filter, cachedStorage);
                for (var what : request) {
                    var extracted = StorageHelper.poweredExtraction(energy, networkStorage, what, 1,
                            cct.getActionSource());
                    if (extracted > 0) {
                        touchedGridStorage = true;
                        currentItem = what.toStack(Ints.saturatedCast(extracted));
                        break;
                    }
                }
            }

            // If still nothing, try taking it from the player inventory
            if (currentItem.isEmpty()) {
                currentItem = takeIngredientFromPlayer(cct, player, ingredient);
            }

            craftMatrix.setItemDirect(x, currentItem);

            // If we couldn't find the item, schedule its autocrafting
            if (currentItem.isEmpty() && craftMissing && craftingService != null) {
                int slot = x;
                findCraftableKey(ingredient, craftingService).ifPresent(key -> {
                    toAutoCraft.computeIfAbsent(key, k -> new IntArrayList()).add(slot);
                });
            }
        }

        // Put away the displaced items that were not used in any other slot
        for (var displaced : displacedItems) {
            var displacedItem = displaced.stack();
            if (displacedItem.isEmpty()) {
                continue;
            }

            var in = AEItemKey.of(displacedItem);
            var inserted = StorageHelper.poweredInsert(energy, networkStorage, in, displacedItem.getCount(),
                    cct.getActionSource());
            if (inserted > 0) {
                touchedGridStorage = true;
                displacedItem.shrink((int) inserted);
            }

            // If more is remaining, try moving it to the player inventory
            if (!displacedItem.isEmpty() && !player.inventory.addItemStackToInventory(displacedItem)) {
                // Leave it in the grid if its original slot is still free, drop it otherwise
                if (craftMatrix.getStackInSlot(displaced.slot()).isEmpty()) {
                    craftMatrix.setItemDirect(displaced.slot(), displacedItem);
                } else {
                    player.dropItem(displacedItem, false);
                }
            }
        }

        menu.onCraftMatrixChanged(craftMatrix.toContainer());

        if (!toAutoCraft.isEmpty()) {
            // Invalidate the grid storage cache if we modified it. The crafting plan will use
            // the outdated cached inventory otherwise.
            if (touchedGridStorage) {
                storageService.invalidateCache();
            }

            // This must be the last call since it changes the menu!
            var stacks = toAutoCraft.entrySet().stream()
                    .map(e -> new ICraftingGridMenu.AutoCraftEntry(e.getKey(), e.getValue())).collect(Collectors.toList());
            cct.startAutoCrafting(stacks);
        }
    }

    /**
     * An item that was removed from the crafting grid because it didn't match the ingredient of its slot.
     */
    private final static class DisplacedItem {
        private final int slot;
        private final ItemStack stack;

        public DisplacedItem(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }

        public int slot() {
            return slot;
        }

        public ItemStack stack() {
            return stack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DisplacedItem)) return false;
            DisplacedItem that = (DisplacedItem) o;
            return slot == that.slot
                    && java.util.Objects.equals(stack, that.stack);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(slot, stack);
        }

        @Override
        public String toString() {
            return "DisplacedItem[" + "slot=" + slot + ", " + "stack=" + stack + "]";
        }

    }

    private static ItemStack takeIngredientFromDisplacedItems(List<DisplacedItem> displacedItems,
            Ingredient ingredient) {
        for (var displaced : displacedItems) {
            var stack = displaced.stack();
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return stack.splitStack(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack takeIngredientFromPlayer(ICraftingGridMenu cct, EntityPlayerMP player, Ingredient ingredient) {
        var playerInv = player.inventory;
        for (int i = 0; i < playerInv.mainInventory.size(); i++) {
            // Do not take ingredients out of locked slots
            if (cct.isPlayerInventorySlotLocked(i)) {
                continue;
            }

            var item = playerInv.getStackInSlot(i);
            if (ingredient.test(item)) {
                var result = item.splitStack(1);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private NonNullList<Ingredient> getDesiredIngredients(EntityPlayer player) {
        // Try to retrieve the real recipe on the server-side
        if (this.recipeId != null) {
            var recipe = CraftingManager.getRecipe(this.recipeId);
            if (recipe != null) {
                return CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
            }
        }

        // If the recipe is unavailable for any reason, use the templates provided by the client
        var ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        Preconditions.checkArgument(ingredients.size() == this.ingredientTemplates.size(),
                "Got %d ingredient templates from client, expected %d",
                ingredientTemplates.size(), ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) {
            var template = ingredientTemplates.get(i);
            if (!template.isEmpty()) {
                ingredients.set(i, Ingredient.fromStacks(template));
            }
        }

        return ingredients;
    }

    /**
     * From a stream of AE item stacks, pick the one with the highest available amount in the network. Returns null if
     * the stream is empty.
     * <p/>
     * We normalize the stored amount vs. the amount needed for the recipe. While this is irrelevant for crafting
     * recipes, it can be relevant for processing patterns where an ingredient doesn't always have amount=1.
     *
     * <pre>
     * Example:
     * Recipe asks for 16xItem A or 1xItem B.
     * Storage has 16xItem A and 15xItem B.
     * Item B should have priority because the recipe could be crafted 15x, while with item A it could only be crafted
     * once.
     * </pre>
     */
    private List<AEItemKey> findBestMatchingItemStack(Ingredient ingredient, IPartitionList filter,
            KeyCounter storage) {
        return Arrays.stream(ingredient.getMatchingStacks())//
                .map(AEItemKey::of) //
                .filter(r -> r != null && (filter == null || filter.isListed(r)))
                .flatMap(s -> storage.findFuzzy(s, FuzzyMode.IGNORE_ALL).stream())//
                // While FuzzyMode.IGNORE_ALL will retrieve all stacks of the same Item which matches
                // standard Vanilla Ingredient matching, there are NBT-matching Ingredient subclasses on Forge,
                // and Mods might actually have mixed into Ingredient
                .filter(e -> ((AEItemKey) e.getKey()).matches(ingredient))
                // Sort in descending order of availability
                .sorted((a, b) -> Long.compare(b.getLongValue(), a.getLongValue()))//
                .map(e -> (AEItemKey) e.getKey())//
                .collect(Collectors.toList());
    }

    private Optional<AEItemKey> findCraftableKey(Ingredient ingredient, ICraftingService craftingService) {
        return Arrays.stream(ingredient.getMatchingStacks())//
                .map(AEItemKey::of)//
                .map(s -> (AEItemKey) craftingService.getFuzzyCraftable(s,
                        key -> ((AEItemKey) key).matches(ingredient)))//
                .filter(Objects::nonNull)//
                .findAny();//
    }
}
