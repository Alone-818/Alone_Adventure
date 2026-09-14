package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.InkBladeProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Alone_adventure.MODID);

    // 墨制刀刃飞刃弹体
    public static final RegistryObject<EntityType<InkBladeProjectile>> INK_BLADE =
            ENTITIES.register("ink_blade_projectile",
                    () -> EntityType.Builder.<InkBladeProjectile>of(
                                    InkBladeProjectile::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("ink_blade_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
