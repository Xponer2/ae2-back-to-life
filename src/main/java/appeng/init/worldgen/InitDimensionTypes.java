package appeng.init.worldgen;

import appeng.spatial.SpatialStorageDimensionIds;

public final class InitDimensionTypes {

    private InitDimensionTypes() {
    }

    /**
     * Registers the spatial storage dimension (pre-init, after the config was loaded).
     */
    public static void init() {
        SpatialStorageDimensionIds.register();
    }
}
