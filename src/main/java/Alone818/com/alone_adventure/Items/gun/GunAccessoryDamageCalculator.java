package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.Curios.gun.GrandOpening;
import Alone818.com.alone_adventure.Curios.gun.MagazinePressure;
import Alone818.com.alone_adventure.Curios.gun.OneWithGun;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * ============================================================
 * 枪械饰品伤害倍率统一计算器
 * ============================================================
 *
 * 所有会影响“最终子弹伤害倍率”的枪械饰品，
 * 都建议从这里统一接入。
 *
 * GunItem 不需要知道具体有哪些饰品。
 *
 * 计算方式：
 *
 * 最终饰品倍率
 * =
 * 弹夹掌控倍率
 * × 饰品A倍率
 * × 饰品B倍率
 * × ...
 *
 * 默认倍率为 1.0F。
 */
public final class GunAccessoryDamageCalculator {

    private GunAccessoryDamageCalculator() {
        // 工具类禁止实例化
    }

    /**
     * ========================================================
     * 获取所有枪械饰品提供的最终伤害倍率
     * ========================================================
     *
     * @param player    当前开枪玩家
     * @param gunStack  当前枪械
     * @param stats     当前枪械最终属性
     *
     * @return 所有相关饰品叠加后的伤害倍率
     */
    public static float getDamageMultiplier(
            Player player,
            ItemStack gunStack,
            GunStats stats
    ) {

        // 默认不改变伤害
        float multiplier = 1.0F;

        // ====================================================
        // 弹夹掌控
        // ====================================================
        //
        // 根据当前弹匣剩余子弹数量改变伤害。
        //
        // 满弹夹：
        //     2.00x
        //
        // 空弹夹：
        //     0.60x
        //
        // 如果没有佩戴弹夹掌控：
        //     1.00x
        //
        multiplier *= MagazinePressure.getDamageMultiplier(
                player,
                gunStack,
                stats
        );

        // ====================================================
        // 人枪合一
        // ====================================================
        //
        // 基础 70%，
        // 每次开枪命中 +10%（上限 150%），
        // 空枪 -20%（最低回落到 70%）。
        //
        multiplier *= OneWithGun.getDamageMultiplier(
                player
        );

        // ====================================================
        // 盛大开场
        // ====================================================
        //
        // 触发（命中 100% ~ 95% 血量生物）后
        // 20 秒内伤害基础值 -35%。
        //
        multiplier *= GrandOpening.getDamageMultiplier(
                player
        );

        // ====================================================
        // 以后其他饰品可以继续写在这里
        // ====================================================
        //
        // 例如：
        //
        // multiplier *= SomeAccessory.getDamageMultiplier(
        //         player,
        //         gunStack,
        //         stats
        // );

        return multiplier;
    }
}