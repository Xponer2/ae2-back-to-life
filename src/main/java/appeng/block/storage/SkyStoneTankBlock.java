package appeng.block.storage;

import appeng.block.BlockProperties;
import appeng.util.ItemTooltipContext;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.storage.SkyStoneTankBlockEntity;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.util.ItemInteractionResult;

public class SkyStoneTankBlock extends AEBaseEntityBlock<SkyStoneTankBlockEntity> {

    public SkyStoneTankBlock(BlockProperties props) {
        super(props);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        if (super.useItemOn(heldItem, state, level, pos, player, hand, hit).result() == EnumActionResult.PASS) {
            if (level.getTileEntity(pos) instanceof SkyStoneTankBlockEntity tank && tank.onPlayerUse(player, hand)) {
                return ItemInteractionResult.SUCCESS;
            }

        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Tooltips.of(GuiText.TankBucketCapacity, SkyStoneTankBlockEntity.BUCKET_CAPACITY));
    }
}
