package appeng.api.stacks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;

/**
 * Manages the registry of key types. On 1.12.2 this is a plain list: AE2 and addons register their key types during
 * mod initialization in a deterministic order, so the raw ids match between client and server.
 */
@ApiStatus.Internal
public final class AEKeyTypesInternal {
    private static final Map<ResourceLocation, AEKeyType> BY_ID = new LinkedHashMap<>();
    private static final List<AEKeyType> BY_RAW_ID = new ArrayList<>();
    @Nullable
    private static Set<AEKeyType> allTypes;

    private AEKeyTypesInternal() {
    }

    public static Set<AEKeyType> getAllTypes() {
        if (allTypes == null) {
            allTypes = ImmutableSet.copyOf(BY_ID.values());
        }
        return allTypes;
    }

    public static synchronized void register(AEKeyType keyType) {
        Preconditions.checkState(!BY_ID.containsKey(keyType.getId()), "Key type %s is already registered",
                keyType.getId());
        Preconditions.checkState(BY_RAW_ID.size() < 127, "Too many key types registered");
        BY_ID.put(keyType.getId(), keyType);
        BY_RAW_ID.add(keyType);
        allTypes = null;
    }

    @Nullable
    public static AEKeyType get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static AEKeyType byRawId(int id) {
        return id >= 0 && id < BY_RAW_ID.size() ? BY_RAW_ID.get(id) : null;
    }

    public static int getRawId(AEKeyType type) {
        return BY_RAW_ID.indexOf(type);
    }
}
