package appeng.integration.modules.itemlists;

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Ints;

import net.minecraft.item.ItemStack;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Rect2i;
import appeng.menu.slot.FakeSlot;

public final class DropTargets {

    private DropTargets() {
    }

    public static List<DropTarget> getTargets(AEBaseScreen<?> aeScreen) {
        List<DropTarget> targets = new ArrayList<>();

        for (var slot : aeScreen.getMenu().inventorySlots) {
            if (slot.isEnabled() && slot instanceof FakeSlot fakeSlot) {
                var area = new Rect2i(aeScreen.getGuiLeft() + slot.xPos, aeScreen.getGuiTop() + slot.yPos, 16, 16);
                targets.add(new FakeSlotDropTarget(area, fakeSlot));
            }
        }

        return targets;
    }

    private static final class FakeSlotDropTarget implements DropTarget {
        private final Rect2i area;
        private final FakeSlot slot;

        private FakeSlotDropTarget(Rect2i area, FakeSlot slot) {
            this.area = area;
            this.slot = slot;
        }

        @Override
        public Rect2i area() {
            return area;
        }

        @Override
        public boolean canDrop(GenericStack stack) {
            // Use the standard inventory function to test if the dragged stack would in theory be accepted
            return slot.canSetFilterTo(wrapFilterAsItem(stack));
        }

        @Override
        public boolean drop(GenericStack stack) {
            var itemStack = wrapFilterAsItem(stack);
            if (slot.canSetFilterTo(itemStack)) {
                slot.setFilterTo(itemStack);
                return true;
            }
            return false;
        }

        // Fake slots use GenericStacks wrapped in ItemStack for non-items internally
        private static ItemStack wrapFilterAsItem(GenericStack genericStack) {
            if (genericStack.what() instanceof AEItemKey itemKey) {
                return itemKey.toStack(Ints.saturatedCast(Math.max(1, genericStack.amount())));
            } else {
                return GenericStack.wrapInItemStack(genericStack.what(), Math.max(1, genericStack.amount()));
            }
        }
    }
}
