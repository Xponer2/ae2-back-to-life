package appeng.helpers;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import appeng.api.components.ItemContainerContents;
import appeng.api.ids.AEComponents;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.items.contents.StackDependentSupplier;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;

public class WirelessCraftingTerminalMenuHost<T extends WirelessCraftingTerminalItem>
        extends WirelessTerminalMenuHost<T> implements ISegmentedInventory {
    private final SupplierInternalInventory<InternalInventory> craftingGrid;

    public WirelessCraftingTerminalMenuHost(T item, EntityPlayer player, ItemMenuHostLocator locator,
            BiConsumer<EntityPlayer, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
        this.craftingGrid = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(
                        this::getItemStack,
                        stack -> createCraftingInv(player, stack)));
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(CraftingTerminalPart.INV_CRAFTING)) {
            return craftingGrid;
        } else {
            return null;
        }
    }

    private static InternalInventory createCraftingInv(EntityPlayer player, ItemStack stack) {
        var craftingGrid = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                AEComponents.CRAFTING_INV.set(stack, inv.toItemContainerContents());
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        }, 9);
        craftingGrid
                .fromItemContainerContents(AEComponents.CRAFTING_INV.getOrDefault(stack, ItemContainerContents.EMPTY));
        return craftingGrid;
    }
}
