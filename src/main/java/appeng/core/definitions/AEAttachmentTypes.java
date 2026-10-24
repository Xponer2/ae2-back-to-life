package appeng.core.definitions;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.minecraft.entity.Entity;

/**
 * Replacement for NeoForge data attachments: transient per-entity data (not saved).
 */
public final class AEAttachmentTypes {
    private AEAttachmentTypes() {
    }

    public static final Attachment<Boolean> HOLDING_CTRL = new Attachment<>(() -> false);

    public static final class Attachment<T> {
        private final Supplier<T> defaultValue;
        private final Map<Entity, T> values = new WeakHashMap<>();

        Attachment(Supplier<T> defaultValue) {
            this.defaultValue = defaultValue;
        }

        public synchronized T get(Entity entity) {
            var value = values.get(entity);
            return value != null ? value : defaultValue.get();
        }

        public synchronized void set(Entity entity, T value) {
            values.put(entity, value);
        }
    }
}
