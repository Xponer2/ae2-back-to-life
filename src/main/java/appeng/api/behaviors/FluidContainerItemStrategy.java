package appeng.api.behaviors;

import com.google.common.primitives.Ints;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import appeng.api.capabilities.Capabilities;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.util.GenericContainerHelper;
import appeng.util.Platform;
import appeng.util.fluid.FluidSoundHelper;

class FluidContainerItemStrategy
        implements ContainerItemStrategy<AEFluidKey, FluidContainerItemStrategy.Context> {
    @Override
    public @Nullable GenericStack getContainedStack(ItemStack stack) {
        return GenericContainerHelper.getContainedFluidStack(stack);
    }

    @Override
    public @Nullable Context findCarriedContext(EntityPlayer player, Container menu) {
        if (Capabilities.FluidHandler.ITEM.getCapability(player.inventory.getItemStack()) != null) {
            return new CarriedContext(player, menu);
        }
        return null;
    }

    @Override
    public @Nullable Context findPlayerSlotContext(EntityPlayer player, int slot) {
        if (Capabilities.FluidHandler.ITEM.getCapability(player.inventory.getStackInSlot(slot)) != null) {
            return new PlayerInvContext(player, slot);
        }

        return null;
    }

    @Override
    public long extract(Context context, AEFluidKey what, long amount, Actionable mode) {
        var stack = context.getStack();
        var copy = Platform.copyWithCount(stack, 1);
        var fluidHandler = Capabilities.FluidHandler.ITEM.getCapability(copy);
        if (fluidHandler == null) {
            return 0;
        }

        int extracted = fluidHandler.drain(what.toStack(Ints.saturatedCast(amount)), mode.getFluidAction()).amount;
        if (mode == Actionable.MODULATE) {
            stack.shrink(1);
            context.addOverflow(fluidHandler.getContainer());
        }
        return extracted;
    }

    @Override
    public long insert(Context context, AEFluidKey what, long amount, Actionable mode) {
        var stack = context.getStack();
        var copy = Platform.copyWithCount(stack, 1);
        var fluidHandler = Capabilities.FluidHandler.ITEM.getCapability(copy);
        if (fluidHandler == null) {
            return 0;
        }

        int filled = fluidHandler.fill(what.toStack(Ints.saturatedCast(amount)), mode.getFluidAction());
        if (mode == Actionable.MODULATE) {
            stack.shrink(1);
            context.addOverflow(fluidHandler.getContainer());
        }
        return filled;
    }

    @Override
    public void playFillSound(EntityPlayer player, AEFluidKey what) {
        FluidSoundHelper.playFillSound(player, what);
    }

    @Override
    public void playEmptySound(EntityPlayer player, AEFluidKey what) {
        FluidSoundHelper.playEmptySound(player, what);
    }

    @Override
    public @Nullable GenericStack getExtractableContent(Context context) {
        return getContainedStack(context.getStack());
    }

    interface Context {
        ItemStack getStack();

        void setStack(ItemStack stack);

        void addOverflow(ItemStack stack);
    }

    private final static class CarriedContext implements Context {
        private final EntityPlayer player;
        private final Container menu;

        public CarriedContext(EntityPlayer player, Container menu) {
            this.player = player;
            this.menu = menu;
        }

        public EntityPlayer player() {
            return player;
        }

        public Container menu() {
            return menu;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CarriedContext)) return false;
            CarriedContext that = (CarriedContext) o;
            return java.util.Objects.equals(player, that.player)
                    && java.util.Objects.equals(menu, that.menu);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(player, menu);
        }

        @Override
        public String toString() {
            return "CarriedContext[" + "player=" + player + ", " + "menu=" + menu + "]";
        }

        @Override
        public ItemStack getStack() {
            return player.inventory.getItemStack();
        }

        @Override
        public void setStack(ItemStack stack) {
            player.inventory.setItemStack(stack);
        }

        public void addOverflow(ItemStack stack) {
            if (player.inventory.getItemStack().isEmpty()) {
                player.inventory.setItemStack(stack);
            } else {
                Platform.placeItemBackInInventory(player, stack);
            }
        }
    }

    private final static class PlayerInvContext implements Context {
        private final EntityPlayer player;
        private final int slot;

        public PlayerInvContext(EntityPlayer player, int slot) {
            this.player = player;
            this.slot = slot;
        }

        public EntityPlayer player() {
            return player;
        }

        public int slot() {
            return slot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlayerInvContext)) return false;
            PlayerInvContext that = (PlayerInvContext) o;
            return java.util.Objects.equals(player, that.player)
                    && slot == that.slot;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(player, slot);
        }

        @Override
        public String toString() {
            return "PlayerInvContext[" + "player=" + player + ", " + "slot=" + slot + "]";
        }

        @Override
        public ItemStack getStack() {
            return player.inventory.getStackInSlot(slot);
        }

        @Override
        public void setStack(ItemStack stack) {
            player.inventory.setInventorySlotContents(slot, stack);
        }

        public void addOverflow(ItemStack stack) {
            Platform.placeItemBackInInventory(player, stack);
        }
    }
}
