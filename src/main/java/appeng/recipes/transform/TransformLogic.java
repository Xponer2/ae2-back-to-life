package appeng.recipes.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.client.EffectType;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.recipes.RecipeManager;

/**
 * In-world transformations: items thrown into a fluid or caught in an explosion. In newer versions a mixin into the
 * item entity started them; here {@link #onItemEntityTick} is called by the items that can start a transformation
 * (the first ingredient of fluid recipes) and explosions are handled in {@link #onExplosion}.
 */
public final class TransformLogic {
    /**
     * How long an item entity has been in a fitting fluid (ticks on the server, effect delay on the client).
     */
    private static final Map<EntityItem, int[]> TRANSFORM_TIME = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean canTransformInFluid(EntityItem entity, Fluid fluid) {
        return getTransformableItems(entity.world, fluid).contains(entity.getItem().getItem());
    }

    public static boolean canTransformInAnyFluid(EntityItem entity) {
        return getTransformableItemsAnyFluid(entity.world).contains(entity.getItem().getItem());
    }

    public static boolean canTransformInExplosion(EntityItem entity) {
        return getTransformableItemsExplosion(entity.world).contains(entity.getItem().getItem());
    }

    /**
     * Called every tick for item entities of AE2 items: transforms them after 3 seconds in a fitting fluid, shows
     * lightning on the client meanwhile.
     */
    public static void onItemEntityTick(EntityItem entity) {
        if (entity.isDead || !canTransformInAnyFluid(entity)) {
            return;
        }

        var box = entity.getEntityBoundingBox();
        var pos = new BlockPos(MathHelper.floor(entity.posX), MathHelper.floor((box.minY + box.maxY) / 2.0D),
                MathHelper.floor(entity.posZ));
        var fluid = FluidRegistry.lookupFluidForBlock(entity.world.getBlockState(pos).getBlock());
        boolean isValidFluid = fluid != null && canTransformInFluid(entity, fluid);

        var time = TRANSFORM_TIME.computeIfAbsent(entity, e -> new int[1]);
        if (entity.world.isRemote) {
            if (isValidFluid && time[0]++ > 30 && AEConfig.instance().isEnableEffects()) {
                // Client side we only render some cool animations.
                AppEng.instance().spawnEffect(EffectType.Lightning, entity.world, entity.posX, entity.posY,
                        entity.posZ, null);
                time[0] = 0;
            }
        } else {
            if (isValidFluid) {
                time[0]++;
                if (time[0] > 60 && !tryTransform(entity, c -> c.isFluid(fluid))) {
                    time[0] = 0;
                }
            } else {
                time[0] = 0;
            }
        }
    }

    /**
     * Items that can be transformed by an explosion are transformed instead of being destroyed by it.
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) {
            return;
        }
        var affected = event.getAffectedEntities();
        for (var entity : new ArrayList<>(affected)) {
            if (entity instanceof EntityItem item && !item.isDead && canTransformInExplosion(item)
                    && tryTransform(item, TransformCircumstance::isExplosion)) {
                affected.remove(entity);
            }
        }
    }

    public static boolean tryTransform(EntityItem entity, Predicate<TransformCircumstance> circumstancePredicate) {
        var level = entity.world;

        var region = new AxisAlignedBB(entity.posX - 1, entity.posY - 1, entity.posZ - 1, entity.posX + 1,
                entity.posY + 1, entity.posZ + 1);
        List<EntityItem> itemEntities = level.getEntitiesWithinAABB(EntityItem.class, region, e -> !e.isDead);

        for (var holder : RecipeManager.of(level).byType(TransformRecipe.TYPE)) {
            var recipe = holder.value();
            if (!circumstancePredicate.test(recipe.circumstance))
                continue;

            if (recipe.ingredients.isEmpty())
                continue;

            List<Ingredient> missingIngredients = Lists.newArrayList(recipe.ingredients);
            Reference2IntMap<EntityItem> consumedItems = new Reference2IntOpenHashMap<>(missingIngredients.size());

            if (recipe.circumstance.isExplosion()) {
                if (missingIngredients.stream().noneMatch(i -> i.apply(entity.getItem())))
                    continue;
            } else {
                if (!missingIngredients.get(0).apply(entity.getItem()))
                    continue;
            }

            for (var itemEntity : itemEntities) {
                var other = itemEntity.getItem();
                if (!other.isEmpty()) {
                    for (var it = missingIngredients.iterator(); it.hasNext();) {
                        Ingredient ing = it.next();
                        var alreadyClaimed = consumedItems.getInt(itemEntity);
                        if (ing.apply(other) && other.getCount() - alreadyClaimed > 0) {
                            consumedItems.put(itemEntity, alreadyClaimed + 1);
                            it.remove();
                        }
                    }
                }
            }

            if (missingIngredients.isEmpty()) {
                var items = new ArrayList<ItemStack>(consumedItems.size());
                for (var e : consumedItems.reference2IntEntrySet()) {
                    var itemEntity = e.getKey();
                    items.add(itemEntity.getItem().splitStack(e.getIntValue()));

                    if (itemEntity.getItem().getCount() <= 0) {
                        itemEntity.setDead();
                    }
                }
                var recipeInput = new TransformRecipeInput(items);
                var craftResult = recipe.assemble(recipeInput);

                var random = level.rand;
                final double x = Math.floor(entity.posX) + .25d + random.nextDouble() * .5;
                final double y = Math.floor(entity.posY) + .25d + random.nextDouble() * .5;
                final double z = Math.floor(entity.posZ) + .25d + random.nextDouble() * .5;
                final double xSpeed = random.nextDouble() * .25 - 0.125;
                final double ySpeed = random.nextDouble() * .25 - 0.125;
                final double zSpeed = random.nextDouble() * .25 - 0.125;

                final EntityItem newEntity = new EntityItem(level, x, y, z, craftResult);

                newEntity.motionX = xSpeed;
                newEntity.motionY = ySpeed;
                newEntity.motionZ = zSpeed;
                level.spawnEntity(newEntity);

                return true;
            }
        }

        return false;
    }

    // not using a Multimap here because we need to cache the empty set
    static Map<Fluid, Set<Item>> fluidCache = Collections.synchronizedMap(new IdentityHashMap<>());
    static Set<Item> explosionCache = null;
    static Set<Item> anyFluidCache = null;

    /**
     * Called when the recipes were (re)loaded.
     */
    public static void clearCache() {
        fluidCache.clear();
        explosionCache = null;
        anyFluidCache = null;
    }

    private static Set<Item> getTransformableItems(World level, Fluid fluid) {
        return fluidCache.computeIfAbsent(fluid, f -> {
            Set<Item> ret = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var holder : RecipeManager.of(level).byType(TransformRecipe.TYPE)) {
                var recipe = holder.value();
                if (!(recipe.circumstance.isFluid(fluid)))
                    continue;
                for (var ingredient : recipe.ingredients) {
                    for (var stack : ingredient.getMatchingStacks()) {
                        ret.add(stack.getItem());
                    }
                    break; // only process first ingredient (they're all required anyway)
                }
            }
            return ret;
        });
    }

    private static Set<Item> getTransformableItemsAnyFluid(World level) {
        Set<Item> ret = anyFluidCache;
        if (ret == null) {
            ret = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var holder : RecipeManager.of(level).byType(TransformRecipe.TYPE)) {
                var recipe = holder.value();
                if (!recipe.circumstance.isFluid())
                    continue;
                for (var ingredient : recipe.ingredients) {
                    for (var stack : ingredient.getMatchingStacks()) {
                        ret.add(stack.getItem());
                    }
                    break; // only process first ingredient (they're all required anyway)
                }
            }
            anyFluidCache = ret;
        }
        return ret;
    }

    private static Set<Item> getTransformableItemsExplosion(World level) {
        Set<Item> ret = explosionCache;
        if (ret == null) {
            ret = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var holder : RecipeManager.of(level).byType(TransformRecipe.TYPE)) {
                var recipe = holder.value();
                if (!recipe.circumstance.isExplosion())
                    continue;
                for (var ingredient : recipe.ingredients) {
                    for (var stack : ingredient.getMatchingStacks()) {
                        ret.add(stack.getItem());
                    }
                    // ingredients that aren't processed may be destroyed in the explosion, so process all of them.
                }
            }
            explosionCache = ret;
        }
        return ret;
    }

    private TransformLogic() {
    }
}
