package appeng.block.misc;

import appeng.util.ItemTooltipContext;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.MysteriousCubeBlockEntity;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.server.services.compass.ServerCompassService;

public class MysteriousCubeBlock extends AEBaseEntityBlock<MysteriousCubeBlockEntity> {
    // Not a redstone conductor to prevent using it as a facade.
    public static final BlockProperties PROPERTIES = metalProps().strength(10, 1000).isRedstoneConductor(Blocks::never);

    public MysteriousCubeBlock() {
        super(PROPERTIES);
    }

    @Override
    public void onPlace(IBlockState state, World level, BlockPos pos, IBlockState oldState, boolean isMoving) {
        if (level instanceof WorldServer serverLevel) {
            ServerCompassService.notifyBlockChange(serverLevel, pos);
        }
    }

    @Override
    public void onRemove(IBlockState state, World level, BlockPos pos, IBlockState newState, boolean isMoving) {
        if (newState.getBlock() == state.getBlock()) {
            return; // Just a block state change
        }

        super.onRemove(state, level, pos, newState, isMoving);

        if (level instanceof WorldServer serverLevel) {
            ServerCompassService.notifyBlockChange(serverLevel, pos);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        tooltip.add(Tooltips.of(GuiText.MysteriousQuote, Tooltips.QUOTE_TEXT));
    }
}
