package appeng.api.parts;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.Event;

import appeng.api.capabilities.BlockCapability;
import appeng.api.capabilities.ICapabilityProvider;
import appeng.core.definitions.BlockEntityType;

public class RegisterPartCapabilitiesEvent extends Event {

    final Set<BlockEntityType<? extends IPartHost>> hostTypes = new HashSet<>();

    final Map<BlockCapability<?, ?>, Function<?, EnumFacing>> contextMappers = new HashMap<>();

    final Map<BlockCapability<?, ?>, BlockCapabilityRegistration<?, ?>> capabilityRegistrations = new HashMap<>();

    final static class BlockCapabilityRegistration<T, C> {
        private final BlockCapability<T, C> capability;
        private final Function<C, EnumFacing> contextToSide;
        private final Map<Class<? extends IPart>, ICapabilityProvider<?, C, T>> parts;

        public BlockCapabilityRegistration(BlockCapability<T, C> capability, Function<C, EnumFacing> contextToSide, Map<Class<? extends IPart>, ICapabilityProvider<?, C, T>> parts) {
            this.capability = capability;
            this.contextToSide = contextToSide;
            this.parts = parts;
        }

        public BlockCapability<T, C> capability() {
            return capability;
        }

        public Function<C, EnumFacing> contextToSide() {
            return contextToSide;
        }

        public Map<Class<? extends IPart>, ICapabilityProvider<?, C, T>> parts() {
            return parts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockCapabilityRegistration)) return false;
            BlockCapabilityRegistration<?, ?> that = (BlockCapabilityRegistration<?, ?>) o;
            return java.util.Objects.equals(capability, that.capability)
                    && java.util.Objects.equals(contextToSide, that.contextToSide)
                    && java.util.Objects.equals(parts, that.parts);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(capability, contextToSide, parts);
        }

        @Override
        public String toString() {
            return "BlockCapabilityRegistration[" + "capability=" + capability + ", " + "contextToSide=" + contextToSide + ", " + "parts=" + parts + "]";
        }

        public BlockCapabilityRegistration(BlockCapability<T, C> capability, Function<C, EnumFacing> contextToSide) {
            this(capability, contextToSide, new HashMap<>());
        }

        <P extends IPart> void add(Class<P> partClass, ICapabilityProvider<P, C, T> provider) {
            if (parts.putIfAbsent(partClass, provider) != null) {
                throw new IllegalStateException("Cannot register an additional capability provider for part "
                        + partClass + " since there already is one for capability " + capability);
            }
        }

        public ICapabilityProvider<IPartHost, C, T> buildProvider() {
            return (partHost, context) -> {
                // Get side from context
                var side = contextToSide.apply(context);
                var part = partHost.getPart(side);
                if (part != null) {
                    return handlePart(part, context);
                }
                return null;
            };
        }

        @SuppressWarnings("unchecked")
        private <P extends IPart> T handlePart(P part, C context) {
            var partProvider = (ICapabilityProvider<P, C, T>) parts.get(part.getClass());
            if (partProvider != null) {
                return partProvider.getCapability(part, context);
            }
            return null;
        }
    }

    /**
     * When using capabilities with a context other than {@link EnumFacing}, you need to register a mapping function for
     * AE2 to get the side from the context. It cannot determine which part on a part host should handle the capability
     * otherwise.
     */
    public <T, C> void registerContext(BlockCapability<T, C> capability, Function<C, EnumFacing> directionGetter) {
        contextMappers.put(capability, directionGetter);
    }

    /**
     * Expose a capability for a part class.
     * <p>
     * When looking for an API instance, providers are queried starting from the class of the part, and then moving up
     * to its superclass, and so on, until a provider returning a nonnull API is found.
     * <p>
     * If the context of the lookup is not {@link EnumFacing}, you need to register a mapping function for your custom
     * context! That must be done before this function is called. Currently, the query will fail silently, but IT WILL
     * throw an exception in the future!
     */
    @SuppressWarnings("unchecked")
    public <T, C, P extends IPart> void register(BlockCapability<T, C> capability,
            ICapabilityProvider<P, C, T> provider,
            Class<P> partClass) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(partClass, "partClass");
        Objects.requireNonNull(provider, "provider");

        if (partClass.isInterface() || Modifier.isAbstract(partClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "Capabilities can only be registered for concrete part classes: " + partClass.getCanonicalName());
        }

        var mapper = (Function<C, EnumFacing>) contextMappers.getOrDefault(capability, c -> (EnumFacing) c);

        var registrations = (BlockCapabilityRegistration<T, C>) capabilityRegistrations
                .computeIfAbsent(capability, ignored -> new BlockCapabilityRegistration<>(capability, mapper));
        registrations.add(partClass, provider);
    }

    /**
     * Adds a new type of block entity that will participate in forwarding API lookups to its attached parts.
     */
    public <T extends TileEntity & IPartHost> void addHostType(BlockEntityType<T> hostType) {
        hostTypes.add(hostType);
    }
}
