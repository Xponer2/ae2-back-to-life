package appeng.menu.interfaces;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import appeng.core.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;

import appeng.api.stacks.AEKeyType;
import appeng.api.util.KeyTypeSelection;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.SelectKeyTypePacket;
import appeng.menu.guisync.PacketWritable;

/**
 * Implemented by menus that allow the user to select key types.
 */
public interface KeyTypeSelectionMenu {
    /**
     * Used on the server side to update the key types from {@link SelectKeyTypePacket}.
     */
    KeyTypeSelection getServerKeyTypeSelection();

    /**
     * Used on the client side to read and <b>write</b> the selected key types.
     */
    SyncedKeyTypes getClientKeyTypeSelection();

    /**
     * Update a key type on the client side.
     */
    @ApiStatus.NonExtendable
    default void selectKeyType(AEKeyType keyType, boolean enabled) {
        // Send to server
        ServerboundPacket message = new SelectKeyTypePacket(keyType, enabled);
        PacketDistributor.sendToServer(message);
        // Update client
        getClientKeyTypeSelection().keyTypes().put(keyType, enabled);
    }

    final static class SyncedKeyTypes implements PacketWritable {
        private final Map<AEKeyType, Boolean> keyTypes;

        public SyncedKeyTypes(Map<AEKeyType, Boolean> keyTypes) {
            this.keyTypes = keyTypes;
        }

        public Map<AEKeyType, Boolean> keyTypes() {
            return keyTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SyncedKeyTypes)) return false;
            SyncedKeyTypes that = (SyncedKeyTypes) o;
            return java.util.Objects.equals(keyTypes, that.keyTypes);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(keyTypes);
        }

        @Override
        public String toString() {
            return "SyncedKeyTypes[" + "keyTypes=" + keyTypes + "]";
        }

        public SyncedKeyTypes() {
            this(new LinkedHashMap<>());
        }

        public SyncedKeyTypes(RegistryFriendlyByteBuf buf) {
            this(buf.<AEKeyType, Boolean, Map<AEKeyType, Boolean>>readMap(LinkedHashMap::new,
                    b -> AEKeyType.fromRawId(b.readVarInt()), RegistryFriendlyByteBuf::readBoolean));
        }

        @Override
        public void writeToPacket(RegistryFriendlyByteBuf buf) {
            buf.writeMap(
                    keyTypes,
                    (b, keyType) -> b.writeVarInt(keyType.getRawId()),
                    RegistryFriendlyByteBuf::writeBoolean);
        }

        public List<AEKeyType> enabledSet() {
            return keyTypes.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
        }
    }
}
