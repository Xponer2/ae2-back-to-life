package appeng.api.implementations.blockentities;

import appeng.util.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;

import appeng.api.capabilities.Capabilities;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.core.localization.GuiText;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;

/**
 * Provides both a key for grouping pattern providers, and displaying the group in the pattern access terminal.
 * <p/>
 * The group can be the crafting container itself, for example if it is given a custom name by the player. It might
 * default to the crafting machine it is adjacent otherwise.
 *
 * @param icon    The icon to display when referring to the grouped containers. This stack will NEVER be modified. It
 *                will be used to show an icon, as well as compared to group similar containers. It may be
 *                {@link ItemStack#EMPTY}
 * @param name    The name to use to refer to the group.
 * @param tooltip Additional tooltip lines to describe the group (i.e. installed upgrades).
 */
public final class PatternContainerGroup {
    private final AEItemKey icon;
    private final ITextComponent name;
    private final List<ITextComponent> tooltip;

    public PatternContainerGroup(@Nullable AEItemKey icon, ITextComponent name, List<ITextComponent> tooltip) {
        this.icon = icon;
        this.name = name;
        this.tooltip = tooltip;
    }

    @Nullable public AEItemKey icon() {
        return icon;
    }

    public ITextComponent name() {
        return name;
    }

    public List<ITextComponent> tooltip() {
        return tooltip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatternContainerGroup)) return false;
        PatternContainerGroup that = (PatternContainerGroup) o;
        return java.util.Objects.equals(icon, that.icon)
                && java.util.Objects.equals(name, that.name)
                && java.util.Objects.equals(tooltip, that.tooltip);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(icon, name, tooltip);
    }

    @Override
    public String toString() {
        return "PatternContainerGroup[" + "icon=" + icon + ", " + "name=" + name + ", " + "tooltip=" + tooltip + "]";
    }

    private static final PatternContainerGroup NOTHING = new PatternContainerGroup(AEItemKey.of(Items.AIR),
            GuiText.Nothing.text(), Collections.emptyList());

    public static PatternContainerGroup nothing() {
        return NOTHING;
    }

    public void writeToPacket(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(icon != null);
        if (icon != null) {
            icon.writeToPacket(buffer);
        }
        ByteBufCodecs.COMPONENT.encode(buffer, name);
        buffer.writeVarInt(tooltip.size());
        for (var component : tooltip) {
            ByteBufCodecs.COMPONENT.encode(buffer, component);
        }
    }

    public static PatternContainerGroup readFromPacket(RegistryFriendlyByteBuf buffer) {
        var icon = buffer.readBoolean() ? AEItemKey.fromPacket(buffer) : null;
        var name = ByteBufCodecs.COMPONENT.decode(buffer);
        var lineCount = buffer.readVarInt();
        var lines = new ArrayList<ITextComponent>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(ByteBufCodecs.COMPONENT.decode(buffer));
        }
        return new PatternContainerGroup(icon, name, lines);
    }

    @Nullable
    public static PatternContainerGroup fromMachine(World level, BlockPos pos, EnumFacing side) {
        // Check for first-class support
        var craftingMachine = ICraftingMachine.of(level, pos, side);
        if (craftingMachine != null) {
            return craftingMachine.getCraftingMachineInfo();
        }

        // Anything else requires a block entity
        var target = level.getTileEntity(pos);
        if (target == null) {
            return null;
        }

        // Heuristic: If it doesn't allow item or fluid transfers, ignore it
        var itemHandler = Capabilities.ItemHandler.BLOCK.getCapability(level,  pos, Platform.getBlockState(target), target,
                side);
        if (itemHandler == null || itemHandler.getSlots() <= 0) {
            var fluidHandler = Capabilities.FluidHandler.BLOCK.getCapability(level,  pos, Platform.getBlockState(target), target,
                    side);
            if (fluidHandler == null || fluidHandler.getTanks() == 0) {
                return null;
            }
        }

        AEItemKey icon;
        ITextComponent name;
        List<ITextComponent> tooltip = Collections.emptyList();

        // For scenarios like pattern providers against cable bus
        if (target instanceof IPartHost partHost) {
            var part = partHost.getPart(side);
            if (part == null) {
                // No part at side -> ignore, we don't interface with the cable
                return null;
            }

            icon = AEItemKey.of(part.getPartItem());
            if (part instanceof IWorldNameable nameable) {
                name = nameable.getDisplayName();
            } else {
                name = icon.getDisplayName();
            }
        } else {
            // Try to wrestle an item from the adjacent block entity
            // TODO On Forge we can use pick block here
            var targetBlock = Platform.getBlockState(target).getBlock();
            var targetItem = new ItemStack(targetBlock);
            icon = AEItemKey.of(targetItem);

            if (target instanceof IWorldNameable nameable && nameable.hasCustomName()) {
                name = nameable.getDisplayName();
            } else {
                if (targetItem.isEmpty()) {
                    // If the object isn't named, and we didn't get an item from it,
                    // we can't show either icon nor name!
                    return null;
                }
                name = Platform.getHoverName(targetItem);
            }
        }

        return new PatternContainerGroup(icon, name, tooltip);
    }
}
