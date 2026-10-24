package appeng.core.network;

import net.minecraft.util.ResourceLocation;

import appeng.core.AppEng;

/**
 * Replacement for the 1.20.5+ {@code CustomPacketPayload}: every AE2 packet has a {@link Type} (its id) and a
 * {@code STREAM_CODEC}. Packets are registered in {@link InitNetwork} and sent with {@link PacketDistributor}.
 */
public interface CustomAppEngPayload {
    Type<? extends CustomAppEngPayload> type();

    static <T extends CustomAppEngPayload> Type<T> createType(String name) {
        return new Type<>(AppEng.makeId(name));
    }

    final class Type<T extends CustomAppEngPayload> {
        private final ResourceLocation id;

        public Type(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Type<?> other && id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "Type[" + id + "]";
        }
    }
}
