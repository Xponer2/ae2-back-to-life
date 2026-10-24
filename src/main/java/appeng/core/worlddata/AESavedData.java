package appeng.core.worlddata;

import java.util.function.Function;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Base class for all AE2 saved data. Keeps the 1.20+ API ({@link #save(NBTTagCompound)}, {@link #setDirty()}) on top of
 * 1.12.2's {@link WorldSavedData}. Subclasses need a public constructor taking the data name (required by
 * {@link MapStorage}) and load their state in {@link #load(NBTTagCompound)}.
 */
public abstract class AESavedData extends WorldSavedData {
    protected AESavedData(String name) {
        super(name);
    }

    public abstract NBTTagCompound save(NBTTagCompound tag);

    public abstract void load(NBTTagCompound tag);

    @Override
    public final void readFromNBT(NBTTagCompound nbt) {
        load(nbt);
    }

    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return save(compound);
    }

    public void setDirty() {
        markDirty();
    }

    /**
     * Gets or creates saved data stored with the overworld (global map storage, like modern overworld data storage).
     */
    @SuppressWarnings("unchecked")
    public static <T extends AESavedData> T getOrCreate(World level, Class<T> clazz, String name,
            Function<String, T> factory) {
        MapStorage storage = level.getMapStorage();
        if (storage == null) {
            throw new IllegalStateException("World has no map storage: " + level);
        }
        var data = (T) storage.getOrLoadData(clazz, name);
        if (data == null) {
            data = factory.apply(name);
            storage.setData(name, data);
        }
        return data;
    }

    /**
     * Gets or creates saved data that is stored per dimension.
     */
    @SuppressWarnings("unchecked")
    public static <T extends AESavedData> T getOrCreatePerWorld(World level, Class<T> clazz, String name,
            Function<String, T> factory) {
        MapStorage storage = level.getPerWorldStorage();
        var data = (T) storage.getOrLoadData(clazz, name);
        if (data == null) {
            data = factory.apply(name);
            storage.setData(name, data);
        }
        return data;
    }
}
