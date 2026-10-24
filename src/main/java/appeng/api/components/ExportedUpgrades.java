package appeng.api.components;

import appeng.util.Platform;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.item.ItemStack;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.util.AECodecs;

public final class ExportedUpgrades {
    private final List<ItemStack> upgrades;

    public ExportedUpgrades(List<ItemStack> upgrades) {
        this.upgrades = upgrades;
    }

    public List<ItemStack> upgrades() {
        return upgrades;
    }

    @Override
    public String toString() {
        return "ExportedUpgrades[" + "upgrades=" + upgrades + "]";
    }

    // Defined using xmap since we previously used a List directly.
    // TODO 1.21.1 Use a normal record codec
    public static Codec<ExportedUpgrades> CODEC = AECodecs.ITEM_STACK.listOf().xmap(ExportedUpgrades::new,
            ExportedUpgrades::upgrades);

    public static StreamCodec<RegistryFriendlyByteBuf, ExportedUpgrades> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.ITEM_STACK_LIST, ExportedUpgrades::upgrades,
            ExportedUpgrades::new);

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof ExportedUpgrades that))
            return false;
        return Platform.listMatches(upgrades, that.upgrades);
    }

    @Override
    public int hashCode() {
        return Platform.hashStackList(upgrades);
    }
}
