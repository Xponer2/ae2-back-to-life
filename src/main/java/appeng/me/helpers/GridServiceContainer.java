package appeng.me.helpers;

import java.util.Map;

import appeng.api.networking.IGridServiceProvider;

public final class GridServiceContainer {
    private final Map<Class<?>, IGridServiceProvider> services;
    private final IGridServiceProvider[] serverStartTickServices;
    private final IGridServiceProvider[] levelStartTickServices;
    private final IGridServiceProvider[] levelEndtickServices;
    private final IGridServiceProvider[] serverEndTickServices;

    public GridServiceContainer(Map<Class<?>, IGridServiceProvider> services, IGridServiceProvider[] serverStartTickServices, IGridServiceProvider[] levelStartTickServices, IGridServiceProvider[] levelEndtickServices, IGridServiceProvider[] serverEndTickServices) {
        this.services = services;
        this.serverStartTickServices = serverStartTickServices;
        this.levelStartTickServices = levelStartTickServices;
        this.levelEndtickServices = levelEndtickServices;
        this.serverEndTickServices = serverEndTickServices;
    }

    public Map<Class<?>, IGridServiceProvider> services() {
        return services;
    }

    public IGridServiceProvider[] serverStartTickServices() {
        return serverStartTickServices;
    }

    public IGridServiceProvider[] levelStartTickServices() {
        return levelStartTickServices;
    }

    public IGridServiceProvider[] levelEndtickServices() {
        return levelEndtickServices;
    }

    public IGridServiceProvider[] serverEndTickServices() {
        return serverEndTickServices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridServiceContainer)) return false;
        GridServiceContainer that = (GridServiceContainer) o;
        return java.util.Objects.equals(services, that.services)
                && java.util.Objects.equals(serverStartTickServices, that.serverStartTickServices)
                && java.util.Objects.equals(levelStartTickServices, that.levelStartTickServices)
                && java.util.Objects.equals(levelEndtickServices, that.levelEndtickServices)
                && java.util.Objects.equals(serverEndTickServices, that.serverEndTickServices);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(services, serverStartTickServices, levelStartTickServices, levelEndtickServices, serverEndTickServices);
    }

    @Override
    public String toString() {
        return "GridServiceContainer[" + "services=" + services + ", " + "serverStartTickServices=" + serverStartTickServices + ", " + "levelStartTickServices=" + levelStartTickServices + ", " + "levelEndtickServices=" + levelEndtickServices + ", " + "serverEndTickServices=" + serverEndTickServices + "]";
    }

}
