package appeng.items.storage;

import java.util.function.Supplier;

import net.minecraft.item.Item;

import appeng.api.ids.AEItemIds;
import appeng.util.BuiltInRegistries;

public final class StorageTier {
    private final int index;
    private final String namePrefix;
    private final int bytes;
    private final double idleDrain;
    private final Supplier<Item> componentSupplier;

    public StorageTier(int index, String namePrefix, int bytes, double idleDrain, Supplier<Item> componentSupplier) {
        this.index = index;
        this.namePrefix = namePrefix;
        this.bytes = bytes;
        this.idleDrain = idleDrain;
        this.componentSupplier = componentSupplier;
    }

    public int index() {
        return index;
    }

    public String namePrefix() {
        return namePrefix;
    }

    public int bytes() {
        return bytes;
    }

    public double idleDrain() {
        return idleDrain;
    }

    public Supplier<Item> componentSupplier() {
        return componentSupplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StorageTier)) return false;
        StorageTier that = (StorageTier) o;
        return index == that.index
                && java.util.Objects.equals(namePrefix, that.namePrefix)
                && bytes == that.bytes
                && Double.compare(idleDrain, that.idleDrain) == 0
                && java.util.Objects.equals(componentSupplier, that.componentSupplier);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(index, namePrefix, bytes, idleDrain, componentSupplier);
    }

    @Override
    public String toString() {
        return "StorageTier[" + "index=" + index + ", " + "namePrefix=" + namePrefix + ", " + "bytes=" + bytes + ", " + "idleDrain=" + idleDrain + ", " + "componentSupplier=" + componentSupplier + "]";
    }

    public static final StorageTier SIZE_1K = new StorageTier(1, "1k", 1024, 0.5,
            () -> BuiltInRegistries.ITEM.get(AEItemIds.CELL_COMPONENT_1K));
    public static final StorageTier SIZE_4K = new StorageTier(2, "4k", 4096, 1.0,
            () -> BuiltInRegistries.ITEM.get(AEItemIds.CELL_COMPONENT_4K));
    public static final StorageTier SIZE_16K = new StorageTier(3, "16k", 16384, 1.5,
            () -> BuiltInRegistries.ITEM.get(AEItemIds.CELL_COMPONENT_16K));
    public static final StorageTier SIZE_64K = new StorageTier(4, "64k", 65536, 2.0,
            () -> BuiltInRegistries.ITEM.get(AEItemIds.CELL_COMPONENT_64K));
    public static final StorageTier SIZE_256K = new StorageTier(5, "256k", 262144, 2.5,
            () -> BuiltInRegistries.ITEM.get(AEItemIds.CELL_COMPONENT_256K));
}
