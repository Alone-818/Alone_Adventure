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

    /**
     * =========================================================
     * 单发强化改件
     * =========================================================
     *
     * 效果：
     *
     * 1. 弹匣容量强制变为 1
     *
     * 2. 装填速度 +100%
     *    即装填时间变为原来的 50%
     *
     * 3. 根据减少的弹匣容量增加伤害
     *
     *    每减少 1 发：
     *    伤害 +30%
     *
     *    公式：
     *
     *    原弹容量 = N
     *
     *    减少数量 = N - 1
     *
     *    最终伤害倍率 =
     *
     *    1.0 + (N - 1) × 0.30
     *
     *    例如：
     *
     *    6 发 -> 1 发
     *    减少 5 发
     *
     *    伤害：
     *    1 + 5 × 0.30
     *    = 2.5
     *
     *    即伤害 +150%
     *
     *    10 发 -> 1 发
     *    减少 9 发
     *
     *    伤害：
     *    1 + 9 × 0.30
     *    = 3.7
     *
     *    即伤害 +270%
     */
    public static final RegistryObject<Item> SINGLE_SHOT_AMPLIFIER =
            ITEMS.register(
                    "single_shot_amplifier",
                    () -> new GunUpgradeItem(
                            new Item.Properties(),

                            base -> {

                                // 原始弹匣容量
                                int originalMagazineSize =
                                        Math.max(
                                                1,
                                                base.magazineSize()
                                        );

                                // 被削减的弹容量
                                int reducedMagazine =
                                        Math.max(
                                                0,
                                                originalMagazineSize - 1
                                        );

                                // 每减少 1 发容量，伤害 +20%
                                float damageMultiplier =
                                        1.0F
                                                + reducedMagazine * 0.15F;

                                int newReloadTicks =
                                        Math.max(
                                                1,
                                                Math.round(
                                                        base.reloadTicks()
                                                                * 2.0F
                                                )
                                        );

                                return GunStats.builder(base)

                                        // 弹匣固定 1 发
                                        .magazineSize(1)

                                        // 装填速度 +100%
                                        .reloadTicks(newReloadTicks)

                                        // 根据减少的容量增加伤害
                                        .damage(
                                                base.damage()
                                                        * damageMultiplier
                                        )

                                        .build();
                            },

                            false
                    )
            );

    // =========================================================
    // 注册
    // =========================================================

    public static void register(
            net.minecraftforge.eventbus.api.IEventBus eventBus
    ) {
        ITEMS.register(eventBus);
    }
}