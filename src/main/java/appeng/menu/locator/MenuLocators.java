package appeng.menu.locator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.parts.AEBasePart;
import appeng.util.UseOnContext;

/**
 * {@link MenuHostLocator} is used to find the host of a menu on both the server and client-side in a predictable manner
 * and transfer this information from the server to the client when a menu should be opened there.
 * <p/>
 * This class exposes several useful factory methods for obtaining a menu locator, as well as a registry for addons to
 * register their own menu locators.
 */
public final class MenuLocators {
    private static final Map<String, Registration<?>> REGISTRY = new HashMap<>();

    static {
        register(BlockEntityLocator.class, BlockEntityLocator::writeToPacket, BlockEntityLocator::readFromPacket);
        register(PartLocator.class, PartLocator::writeToPacket, PartLocator::readFromPacket);
        register(InventoryItemLocator.class, InventoryItemLocator::writeToPacket, InventoryItemLocator::readFromPacket);
        register(CuriosItemLocator.class, CuriosItemLocator::writeToPacket, CuriosItemLocator::readFromPacket);
    }

    /**
     * Register a new implementation of {@link MenuHostLocator}. This is required to serialize it to/from the network.
     */
    public static synchronized <T extends MenuHostLocator> void register(Class<T> locatorClass,
            BiConsumer<T, RegistryFriendlyByteBuf> packetWriter,
            Function<RegistryFriendlyByteBuf, T> packetReader) {
        String classKey = locatorClass.getName();
        if (REGISTRY.containsKey(classKey)) {
            throw new IllegalStateException("MenuLocator type " + classKey + " is already registered.");
        }
        REGISTRY.put(classKey, new Registration<>(locatorClass, packetWriter, packetReader));
    }

    private synchronized static Registration<?> getRegistration(String classKey) {
        var registration = REGISTRY.get(classKey);
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered menu locator class: " + classKey);
        }
        return registration;
    }

    @SuppressWarnings("unchecked")
    public static <T extends MenuHostLocator> void writeToPacket(RegistryFriendlyByteBuf buf, T locator) {
        var classKey = locator.getClass().getName();
        var registration = (Registration<T>) getRegistration(classKey);
        buf.writeUtf(classKey);
        registration.writeToPacket.accept(locator, buf);
    }

    public static MenuHostLocator readFromPacket(RegistryFriendlyByteBuf buf) {
        var classKey = buf.readUtf();

        var registration = getRegistration(classKey);
        return registration.readFromPacket.apply(buf);
    }

    public static MenuHostLocator forBlockEntity(TileEntity te) {
        if (te.getWorld() == null) {
            throw new IllegalArgumentException("Cannot open a block entity that is not in a level");
        }
        return new BlockEntityLocator(te.getPos());
    }

    public static MenuHostLocator forPart(AEBasePart part) {
        var pos = part.getHost().getLocation();
        return new PartLocator(pos.getPos(), part.getSide());
    }

    public static ItemMenuHostLocator forStack(ItemStack stack) {
        return new StackItemLocator(stack);
    }

    /**
     * Construct a menu locator for an item being used on a block. The item could still open a menu for itself, but it
     * might also open a special menu for the block being right-clicked.
     */
    public static ItemMenuHostLocator forItemUseContext(UseOnContext context) {
        EntityPlayer player = context.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Cannot open a menu without a player");
        }
        int slot = getPlayerInventorySlotFromHand(player, context.getHand());
        return new InventoryItemLocator(
                slot,
                getHitResult(context));
    }

    public static ItemMenuHostLocator forHand(EntityPlayer player, EnumHand hand) {
        int slot = getPlayerInventorySlotFromHand(player, hand);
        return forInventorySlot(slot);
    }

    public static ItemMenuHostLocator forInventorySlot(int inventorySlot) {
        return new InventoryItemLocator(inventorySlot, null);
    }

    public static ItemMenuHostLocator forCurioSlot(int curioSlot) {
        return new CuriosItemLocator(curioSlot, null);
    }

    public static ItemMenuHostLocator forCurioSlot(int curioSlot, UseOnContext context) {
        return new CuriosItemLocator(curioSlot, getHitResult(context));
    }

    private static int getPlayerInventorySlotFromHand(EntityPlayer player, EnumHand hand) {
        ItemStack is = player.getHeldItem(hand);
        if (is.isEmpty()) {
            throw new IllegalArgumentException("Cannot open an item-inventory with empty hands");
        }
        int invSize = player.inventory.getSizeInventory();
        for (int i = 0; i < invSize; i++) {
            if (player.inventory.getStackInSlot(i) == is) {
                return i;
            }
        }
        throw new IllegalArgumentException("Could not find item held in hand " + hand + " in player inventory");
    }

    private final static class Registration<T extends MenuHostLocator> {
        private final Class<T> locatorClass;
        private final BiConsumer<T, RegistryFriendlyByteBuf> writeToPacket;
        private final Function<RegistryFriendlyByteBuf, T> readFromPacket;

        public Registration(Class<T> locatorClass, BiConsumer<T, RegistryFriendlyByteBuf> writeToPacket, Function<RegistryFriendlyByteBuf, T> readFromPacket) {
            this.locatorClass = locatorClass;
            this.writeToPacket = writeToPacket;
            this.readFromPacket = readFromPacket;
        }

        public Class<T> locatorClass() {
            return locatorClass;
        }

        public BiConsumer<T, RegistryFriendlyByteBuf> writeToPacket() {
            return writeToPacket;
        }

        public Function<RegistryFriendlyByteBuf, T> readFromPacket() {
            return readFromPacket;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Registration)) return false;
            Registration<?> that = (Registration<?>) o;
            return java.util.Objects.equals(locatorClass, that.locatorClass)
                    && java.util.Objects.equals(writeToPacket, that.writeToPacket)
                    && java.util.Objects.equals(readFromPacket, that.readFromPacket);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(locatorClass, writeToPacket, readFromPacket);
        }

        @Override
        public String toString() {
            return "Registration[" + "locatorClass=" + locatorClass + ", " + "writeToPacket=" + writeToPacket + ", " + "readFromPacket=" + readFromPacket + "]";
        }

    }

    private static RayTraceResult getHitResult(UseOnContext context) {
        // 1.21 BlockHitResult(location, direction, pos, inside), 1.12.2 has no "inside" flag
        return new RayTraceResult(context.getClickLocation(), context.getHorizontalDirection(),
                context.getClickedPos());
    }
}
