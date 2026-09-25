package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunStats;
import Alone818.com.alone_adventure.Items.gun.GunUpgradeItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModModificationItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    Alone_adventure.MODID
            );

    // =========================================================
    // 枪械改装件
    // =========================================================

    /**
     * 重型枪管
     *
     * 效果：
     * 伤害 = 原来的 200%
     * 射击间隔 = 原来的 300%
     *
     * 也就是说：
     * - 伤害 ×2
     * - 射击速度降低到原来的 1/3
     */
    public static final RegistryObject<Item> HEAVY_BARREL =
            ITEMS.register(
                    "heavy_barrel",
                    () -> new GunUpgradeItem(
                            new Item.Properties(),

                            base -> GunStats.builder(base)
                                    .damage(base.damage() * 2.0F)
                                    .fireRateTicks(
                                            Math.max(
                                                    1,
                                                    Math.round(
                                                            base.fireRateTicks() * 3.0F
                                                    )
                                            )
                                    )
                                    .build(),

                            false
                    )
            );

    /**
     * 追踪改件
     *
     * 效果：
     * 不修改枪械数值。
     * 发射出的子弹启用追踪行为。
     */
    public static final RegistryObject<Item> TRACKING_UPGRADE =
            ITEMS.register(
                    "tracking_upgrade",
                    () -> new GunUpgradeItem(
                            new Item.Properties(),

                            base -> base,

                            true
                    )
            );

    // =========================================================
    // 注册
    // =========================================================

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}