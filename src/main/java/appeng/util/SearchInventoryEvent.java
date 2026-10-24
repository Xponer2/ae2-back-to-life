package appeng.util;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import appeng.integration.modules.curios.CuriosIntegration;

/**
 * Event fired when AE2 is looking for ItemStacks in a player inventory. By default, AE2 only looks at the 36 usual
 * slots of the player inventory, use this event to make AE2 consider more stacks. AE2 will check after the event if
 * they contain the item it is searching.
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

    static {
        MinecraftForge.EVENT_BUS.addListener((SearchInventoryEvent event) -> {
            event.getStacks().addAll(event.getEntity().getInventory().items);
        });
        MinecraftForge.EVENT_BUS.addListener((SearchInventoryEvent event) -> {
            var cap = event.getEntity().getCapability(CuriosIntegration.ITEM_HANDLER);
            if (cap == null)
                return;
            for (int i = 0; i < cap.getSlots(); i++) {
                event.getStacks().add(cap.getStackInSlot(i));
            }
        });
    }

    public static List<ItemStack> getItems(EntityPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        MinecraftForge.EVENT_BUS.post(new SearchInventoryEvent(player, items));
        return items;
    }
}
