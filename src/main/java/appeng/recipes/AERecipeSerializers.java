package appeng.recipes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import appeng.core.AppEng;
import appeng.recipes.entropy.EntropyRecipeSerializer;
import appeng.recipes.game.AddItemUpgradeRecipeSerializer;
import appeng.recipes.game.CraftingUnitTransformRecipeSerializer;
import appeng.recipes.game.FacadeRecipe;
import appeng.recipes.game.RemoveItemUpgradeRecipeSerializer;
import appeng.recipes.game.StorageCellDisassemblyRecipeSerializer;
import appeng.recipes.game.StorageCellUpgradeRecipeSerializer;
import appeng.recipes.handlers.ChargerRecipeSerializer;
import appeng.recipes.handlers.InscriberRecipeSerializer;
import appeng.recipes.mattercannon.MatterCannonAmmoSerializer;
import appeng.recipes.quartzcutting.QuartzCuttingRecipeSerializer;
import appeng.recipes.transform.TransformRecipeSerializer;

public final class AERecipeSerializers {
    private AERecipeSerializers() {
    }

    // Recipe type ids in the recipe files ("type": "ae2:inscriber") -> serializer
    private static final Map<ResourceLocation, RecipeSerializer<?>> SERIALIZERS = new HashMap<>();

    static {
        register("inscriber", InscriberRecipeSerializer.INSTANCE);
        register("facade", FacadeRecipe.SERIALIZER);
        register("entropy", EntropyRecipeSerializer.INSTANCE);
        register("matter_cannon", MatterCannonAmmoSerializer.INSTANCE);
        register("transform", TransformRecipeSerializer.INSTANCE);
        register("charger", ChargerRecipeSerializer.INSTANCE);
        register("storage_cell_upgrade", StorageCellUpgradeRecipeSerializer.INSTANCE);
        register("add_item_upgrade", AddItemUpgradeRecipeSerializer.INSTANCE);
        register("remove_item_upgrade", RemoveItemUpgradeRecipeSerializer.INSTANCE);
        register("quartz_cutting", QuartzCuttingRecipeSerializer.INSTANCE);
        register("crafting_unit_transform", CraftingUnitTransformRecipeSerializer.INSTANCE);
        register("storage_cell_disassembly", StorageCellDisassemblyRecipeSerializer.INSTANCE);
    }

    private static void register(String id, RecipeSerializer<?> serializer) {
        SERIALIZERS.put(AppEng.makeId(id), serializer);
    }

    /**
     * @return The serializer for the recipe type of a recipe file, or null if it isn't an AE2 type.
     */
    @Nullable
    public static RecipeSerializer<?> get(ResourceLocation typeId) {
        return SERIALIZERS.get(typeId);
    }
}
