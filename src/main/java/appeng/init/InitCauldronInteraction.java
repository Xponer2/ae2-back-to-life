package appeng.init;

import net.minecraft.block.BlockCauldron;
import net.minecraft.stats.StatList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.items.tools.MemoryCardItem;
import appeng.items.tools.powered.AbstractPortableCell;

/**
 * Dyed portable cells and memory cards are washed in a cauldron like leather armor. The cauldron of this version only
 * knows leather armor, so this is done when a player uses one of these items on a cauldron with water.
 */
public class InitCauldronInteraction {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(InitCauldronInteraction.class);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof AbstractPortableCell) && !(stack.getItem() instanceof MemoryCardItem)) {
            return;
        }
        // Dyed like leather armor: display.color
        var display = stack.getSubCompound("display");
        if (display == null || !display.hasKey("color")) {
            return;
        }
        var level = event.getWorld();
        var state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BlockCauldron cauldron)) {
            return;
        }
        int water = state.getValue(BlockCauldron.LEVEL);
        if (water <= 0) {
            return;
        }

        if (!level.isRemote) {
            display.removeTag("color");
            if (display.isEmpty()) {
                stack.removeSubCompound("display");
            }
            cauldron.setWaterLevel(level, event.getPos(), state, water - 1);
            event.getEntityPlayer().addStat(StatList.ARMOR_CLEANED);
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
    }
}
