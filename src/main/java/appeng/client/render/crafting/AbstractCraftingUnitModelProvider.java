package appeng.client.render.crafting;

import java.util.List;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.crafting.ICraftingUnitType;

/**
 * Provides material and model information for custom crafting CPU blocks for use with {@link CraftingCubeModel}.
 * 
 * @param <T> The "type" associated with the crafting block being rendered.
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractCraftingUnitModelProvider<T extends ICraftingUnitType> {

    protected final T type;

    public AbstractCraftingUnitModelProvider(T type) {
        this.type = type;
    }

    /**
     * @return a list of any materials used for the model to be passed into
     *         {@link CraftingCubeModel#getTextures()}.
     */
    public abstract List<ResourceLocation> getMaterials();

    public abstract IBakedModel getBakedModel(Function<ResourceLocation, TextureAtlasSprite> spriteGetter);
}
