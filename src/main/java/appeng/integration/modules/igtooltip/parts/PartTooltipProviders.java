package appeng.integration.modules.igtooltip.parts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.IconProvider;
import appeng.api.integrations.igtooltip.providers.NameProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;

public final class PartTooltipProviders {
    private static final Comparator<Registration<?>> COMPARATOR = Comparator.comparingInt(Registration::priority);

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private static final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private static final List<Registration<ServerDataProvider<?>>> serverDataProviders = new ArrayList<>();
    private static final List<Registration<BodyProvider<?>>> bodyProviders = new ArrayList<>();
    private static final List<Registration<NameProvider<?>>> nameProviders = new ArrayList<>();
    private static final List<Registration<IconProvider<?>>> iconProviders = new ArrayList<>();

    private static final Map<Class<?>, CachedProviders<?>> cache = new IdentityHashMap<>();

    private PartTooltipProviders() {
    }

    public static <T> void addServerData(Class<T> baseClass, ServerDataProvider<? super T> provider, int priority) {
        add(serverDataProviders, baseClass, provider, priority);
    }

    public static <T> void addBody(Class<T> baseClass, BodyProvider<? super T> provider, int priority) {
        add(bodyProviders, baseClass, provider, priority);
    }

    public static <T> void addName(Class<T> baseClass, NameProvider<? super T> provider, int priority) {
        add(nameProviders, baseClass, provider, priority);
    }

    public static <T> void addIcon(Class<T> baseClass, IconProvider<? super T> provider, int priority) {
        add(iconProviders, baseClass, provider, priority);
    }

    private static <T> void add(List<Registration<T>> registrations, Class<?> baseClass, T provider, int priority) {
        writeLock.lock();
        try {
            registrations.add(new Registration<>(baseClass, provider, priority));
            registrations.sort(COMPARATOR);
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> CachedProviders<T> getProviders(T object) {
        return getProviders((Class<T>) object.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <U> CachedProviders<U> getProviders(Class<U> objectClass) {
        CachedProviders<U> providers;
        readLock.lock();
        try {
            providers = ((CachedProviders<U>) cache.get(objectClass));
        } finally {
            readLock.unlock();
        }

        // Lazily create the cache
        if (providers == null) {
            writeLock.lock();
            try {
                providers = ((CachedProviders<U>) cache.computeIfAbsent(objectClass,
                        PartTooltipProviders::createProviderLists));
            } finally {
                writeLock.unlock();
            }
        }

        return providers;
    }

    @SuppressWarnings("unchecked")
    private static <U> CachedProviders<U> createProviderLists(Class<U> clazz) {
        var compatibleNameProviders = new ArrayList<NameProvider<? super U>>();
        for (var registration : nameProviders) {
            if (registration.baseClass.isAssignableFrom(clazz)) {
                compatibleNameProviders.add((NameProvider<? super U>) registration.provider());
            }
        }
        var compatibleBodyProviders = new ArrayList<BodyProvider<? super U>>();
        for (var registration : bodyProviders) {
            if (registration.baseClass.isAssignableFrom(clazz)) {
                compatibleBodyProviders.add((BodyProvider<? super U>) registration.provider());
            }
        }
        var compatibleIconProviders = new ArrayList<IconProvider<? super U>>();
        for (var registration : iconProviders) {
            if (registration.baseClass.isAssignableFrom(clazz)) {
                compatibleIconProviders.add((IconProvider<? super U>) registration.provider());
            }
        }
        var compatibleServerDataProviders = new ArrayList<ServerDataProvider<? super U>>();
        for (var registration : serverDataProviders) {
            if (registration.baseClass.isAssignableFrom(clazz)) {
                compatibleServerDataProviders.add((ServerDataProvider<? super U>) registration.provider());
            }
        }

        return new CachedProviders<>(
                compatibleServerDataProviders,
                compatibleBodyProviders,
                compatibleIconProviders,
                compatibleNameProviders);
    }

    private static final class Registration<T> {
        private final Class<?> baseClass;
        private final T provider;
        private final int priority;

        private Registration(Class<?> baseClass, T provider, int priority) {
            this.baseClass = baseClass;
            this.provider = provider;
            this.priority = priority;
        }

        T provider() {
            return provider;
        }

        int priority() {
            return priority;
        }
    }

    public static final class CachedProviders<U> {
        private final List<ServerDataProvider<? super U>> serverDataProviders;
        private final List<BodyProvider<? super U>> bodyProviders;
        private final List<IconProvider<? super U>> iconProviders;
        private final List<NameProvider<? super U>> nameProviders;

        CachedProviders(List<ServerDataProvider<? super U>> serverDataProviders,
                List<BodyProvider<? super U>> bodyProviders,
                List<IconProvider<? super U>> iconProviders,
                List<NameProvider<? super U>> nameProviders) {
            this.serverDataProviders = serverDataProviders;
            this.bodyProviders = bodyProviders;
            this.iconProviders = iconProviders;
            this.nameProviders = nameProviders;
        }

        public List<ServerDataProvider<? super U>> serverDataProviders() {
            return serverDataProviders;
        }

        public List<BodyProvider<? super U>> bodyProviders() {
            return bodyProviders;
        }

        public List<IconProvider<? super U>> iconProviders() {
            return iconProviders;
        }

        public List<NameProvider<? super U>> nameProviders() {
            return nameProviders;
        }
    }
}
