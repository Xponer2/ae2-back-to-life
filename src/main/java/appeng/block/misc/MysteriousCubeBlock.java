package appeng.block.misc;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockProperties;
import appeng.blockentity.misc.MysteriousCubeBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.datagen.providers.tags.ConventionTags;
import appeng.server.services.compass.ServerCompassService;
import appeng.util.ItemTooltipContext;

public class MysteriousCubeBlock extends AEBaseEntityBlock<MysteriousCubeBlockEntity> {
    // Not a redstone conductor to prevent using it as a facade.
    public static final BlockProperties PROPERTIES = metalProps().strength(10, 1000).isRedstoneConductor(null);

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

    /**
     * The 1.21 loot table: all inscriber presses (item tag {@code ae2:inscriber_presses}) and the guide; with silk touch
     * the cube itself (see {@link #canSilkHarvest}).
     */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
            int fortune) {
        for (var id : ConventionTags.INSCRIBER_PRESSES.ids()) {
            var item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) {
                drops.add(new ItemStack(item));
            }
        }
        drops.add(AEItems.TABLET.stack());
    }

    /**
     * Blocks with a block entity can't be silk harvested by default.
     */
    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        tooltip.add(Tooltips.of(GuiText.MysteriousQuote, Tooltips.QUOTE_TEXT));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
