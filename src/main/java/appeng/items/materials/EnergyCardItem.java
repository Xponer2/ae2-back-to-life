package appeng.items.materials;

import appeng.util.ItemProperties;

import com.google.common.base.Preconditions;

public class EnergyCardItem extends UpgradeCardItem {

    private final int energyMultiplier;

    public EnergyCardItem(ItemProperties properties, int energyMultiplier) {
        super(properties);

        Preconditions.checkArgument(energyMultiplier > 0, "energyMultiplier must be > 0");
        this.energyMultiplier = energyMultiplier;
    }

    public int getEnergyMultiplier() {
        return this.energyMultiplier;
    }
}
