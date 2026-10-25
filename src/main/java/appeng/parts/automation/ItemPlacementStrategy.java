package appeng.parts.automation;

import java.util.Random;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IPlantable;

import appeng.api.behaviors.PlacementStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AEConfig;
import appeng.util.Platform;

public class ItemPlacementStrategy implements PlacementStrategy {
    private static final Random RANDOM_OFFSET = new Random();
    private final WorldServer level;
    private final BlockPos pos;
    private final EnumFacing side;
    private final TileEntity host;
    @Nullable
    private final UUID ownerUuid;
    private boolean blocked = false;

    public ItemPlacementStrategy(WorldServer level, BlockPos pos, EnumFacing side, TileEntity host,
            @Nullable UUID owningPlayerId) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.host = host;
        this.ownerUuid = owningPlayerId;
    }

    public void clearBlocked() {
        this.blocked = !isReplaceable();
    }

    private boolean isReplaceable() {
        return level.getBlockState(pos).getBlock().isReplaceable(level, pos);
    }

    public final long placeInWorld(AEKey what, long amount, Actionable type, boolean placeAsEntity) {
        if (this.blocked || !(what instanceof AEItemKey itemKey) || amount <= 0) {
            return 0;
        }

        var i = itemKey.getItem();

        var maxStorage = (int) Math.min(amount, itemKey.getMaxStackSize());
        var is = itemKey.toStack(maxStorage);
        var worked = false;

        var side = this.side.getOpposite();

        final var placePos = pos;

        if (isReplaceable()) {
            if (placeAsEntity) {
                final var sum = this.countEntitesAround(level, placePos);

                // Disable spawning once there is a certain amount of entities in an area.
                if (sum < AEConfig.instance().getFormationPlaneEntityLimit()) {
                    worked = true;

                    if (type == Actionable.MODULATE) {
                        is.setCount(maxStorage);
                        spawnItemEntity(level, host, side, is);
                    }
                }
            } else {
                final var player = Platform.getFakePlayer(level, ownerUuid);
                Platform.configurePlayer(player, side, host);

                maxStorage = is.getCount();
                worked = true;
                if (type == Actionable.MODULATE) {
                    // The side the plane is attached to will be considered the look direction
                    // in terms of placing an item
                    var lookDirection = side;

                    // Items are used by the player holding them
                    player.setHeldItem(EnumHand.MAIN_HAND, is);
                    try {
                        // Clicking the (replaceable) target block places blocks into it
                        is.onItemUse(player, level, placePos, EnumHand.MAIN_HAND, lookDirection.getOpposite(),
                                0.5f, 0.5f, 0.5f);
                        if (is.getCount() == maxStorage && i instanceof IPlantable) {
                            // Seeds are used on the soil below the target block
                            is.onItemUse(player, level, placePos.down(), EnumHand.MAIN_HAND, EnumFacing.UP,
                                    0.5f, 1.0f, 0.5f);
                        }
                    } finally {
                        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                    }
                    maxStorage = Math.max(0, maxStorage - is.getCount());

                } else {
                    maxStorage = 1;
                }

                // Seems to work without... Safe keeping
                // player.setHeldItem(hand, ItemStack.EMPTY);
            }
        }

        this.blocked = !isReplaceable();

        if (worked) {
            return maxStorage;
        }

        return 0;
    }

    private static void spawnItemEntity(World level, TileEntity te, EnumFacing side, ItemStack is) {
        // The center of the block the plane is located in
        final var centerX = te.getPos().getX() + .5;
        final double centerY = te.getPos().getY();
        final var centerZ = te.getPos().getZ() + .5;

        // Create an EntityItem already at the position of the plane.
        // We don't know the final position, but need it for its size.
        Entity entity = new EntityItem(level, centerX, centerY, centerZ, is.copy());

        // When spawning downwards, we have to take into account that it spawns it at
        // their "feet" and not center like x or z. So we move it up to be flush with
        // the plane
        final double additionalYOffset = side.getYOffset() == -1 ? 1 - entity.height : 0;

        // Calculate the maximum spawn area so an entity hitbox will always be inside
        // the block.
        final double spawnAreaHeight = Math.max(0, 1 - entity.height);
        final double spawnAreaWidth = Math.max(0, 1 - entity.width);

        // Calculate the offsets to spawn it into the adjacent block, taking the sign
        // into account.
        // Spawn it 0.8 blocks away from the center pos when facing in this direction
        // Every other direction will select a position in a .5 block area around the
        // block center.
        final var offsetX = side.getXOffset() == 0 //
                ? RANDOM_OFFSET.nextFloat() * spawnAreaWidth - spawnAreaWidth / 2
                : side.getXOffset() * (.525 + entity.width / 2);
        final var offsetY = side.getYOffset() == 0 //
                ? RANDOM_OFFSET.nextFloat() * spawnAreaHeight
                : side.getYOffset() + additionalYOffset;
        final var offsetZ = side.getZOffset() == 0 //
                ? RANDOM_OFFSET.nextFloat() * spawnAreaWidth - spawnAreaWidth / 2
                : side.getZOffset() * (.525 + entity.width / 2);

        final var absoluteX = centerX + offsetX;
        final var absoluteY = centerY + offsetY;
        final var absoluteZ = centerZ + offsetZ;

        // Set to correct position and slow the motion down a bit
        entity.setPosition(absoluteX, absoluteY, absoluteZ);
        entity.motionX = side.getXOffset() * .1;
        entity.motionY = side.getYOffset() * 0.1;
        entity.motionZ = side.getZOffset() * 0.1;

        // NOTE: Vanilla generally ignores the return-value of this method when spawning items into the world
        // Forge will return false when the embedded event is canceled, but the event canceller is responsible
        // for cleaning up the entity in that case, so we should always assume our spawning was successful,
        // and consume items...
        level.spawnEntity(entity);
    }

    private int countEntitesAround(World level, BlockPos pos) {
        final var t = new AxisAlignedBB(pos).grow(8);
        final var list = level.getEntitiesWithinAABB(Entity.class, t);

        return list.size();
    }
}
