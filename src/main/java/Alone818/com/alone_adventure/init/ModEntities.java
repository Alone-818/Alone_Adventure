package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.Items.InkBladeProjectile;
import Alone818.com.alone_adventure.Items.RegimentBannerEntity;
import Alone818.com.alone_adventure.Items.ShockDeviceProjectile;
import Alone818.com.alone_adventure.entity.AmmoBoxEntity;
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

    // 投掷电击器放电装置弹体
    public static final RegistryObject<EntityType<ShockDeviceProjectile>> SHOCK_DEVICE =
            ENTITIES.register("shock_device_projectile",
                    () -> EntityType.Builder.<ShockDeviceProjectile>of(
                                    ShockDeviceProjectile::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("shock_device_projectile"));

    // 连队团旗的旗子实体（静态、不可交互，仅承载光环与渲染）
    public static final RegistryObject<EntityType<RegimentBannerEntity>> REGIMENT_BANNER =
            ENTITIES.register("regiment_banner",
                    () -> EntityType.Builder.<RegimentBannerEntity>of(
                                    RegimentBannerEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.8F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .fireImmune()
                            .build("regiment_banner"));

    // 通用枪械的子弹实体（高速直线弹道，可穿透/衰减）
    public static final RegistryObject<EntityType<BulletProjectile>> BULLET =
            ENTITIES.register("bullet",
                    () -> EntityType.Builder.<BulletProjectile>of(
                                    BulletProjectile::new, MobCategory.MISC)
                            .sized(0.15F, 0.15F)
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build("bullet"));
    public static final RegistryObject<EntityType<AmmoBoxEntity>> AMMO_BOX =
            ENTITIES.register(
                    "ammo_box",
                    () -> EntityType.Builder
                            .of(
                                    AmmoBoxEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.25F,
                                    0.25F
                            )
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(
                                    Alone_adventure.MODID
                                            + ":ammo_box"
                            )
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
