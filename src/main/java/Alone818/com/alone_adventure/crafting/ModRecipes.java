package Alone818.com.alone_adventure.crafting;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 自定义合成配方序列化器注册
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Alone_adventure.MODID);

    public static final RegistryObject<RecipeSerializer<InjectionRecipe>> INJECTION =
            SERIALIZERS.register("injection_recipe",
                    () -> new InjectionRecipe.Serializer() {
                    });

    public static final RegistryObject<RecipeSerializer<InjectionMixRecipe>> INJECTION_MIX =
            SERIALIZERS.register("injection_mix_recipe",
                    () -> new InjectionMixRecipe.Serializer() {
                    });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}