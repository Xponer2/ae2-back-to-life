package appeng.util;

import net.minecraftforge.event.entity.player.PlayerEvent;
import java.util.ArrayList;
import java.util.List;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;


/**
 * Event fired when AE2 is looking for ItemStacks in a player inventory. By default, AE2 only looks at the 36 usual
 * slots of the player inventory, use this event to make AE2 consider more stacks. AE2 will check after the event if
 * they contain the item it is searching.
 */
/**
 * Collects the items a player carries (main inventory; other mods can add theirs, e.g. Baubles). 1.21 also adds the
 * Curios slots, which don't exist on 1.12.2.
 */
public class SearchInventoryEvent extends PlayerEvent {
    private final List<ItemStack> stacks;

    public SearchInventoryEvent(EntityPlayer player, List<ItemStack> stacks) {
        super(player);
        this.stacks = stacks;
    }

    public List<ItemStack> getStacks() {
        return stacks;
    }

    public static List<ItemStack> getItems(EntityPlayer player) {
        List<ItemStack> items = new ArrayList<>(player.inventory.mainInventory);
        MinecraftForge.EVENT_BUS.post(new SearchInventoryEvent(player, items));
        return items;
    }
}
