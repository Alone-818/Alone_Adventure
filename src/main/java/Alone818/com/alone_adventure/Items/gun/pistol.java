package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.item.Rarity;

/**
 * 手枪 - 单手速射模板
 *
 * 属性一览：
 * 子弹伤害 4 ／ 子弹速度 6.0 ／ 弹丸数 1 ／ 弹夹 12（弹夹装填 1.5 秒）
 * 弹药：短子弹
 * 开火模式 半自动 ／ 射速 0.3 秒/发 ／ 后坐力 1.5° ／ 偏移 ±1.2°/±1.0°
 * 穿透 0 ／ 反弹 1 ／ 击退 0.5 ／ 有效射程 25 格 ／ 改装槽位 1
 * 持枪方式 单手（可双持）／ 瞄准倍率 1.5×
 *
 * 子弹命中方块可跳弹 1 次（按命中面镜面反射，保留 70% 速度）。
 */
public class pistol extends GunItem {

    public pistol() {
        super(new Properties().rarity(Rarity.COMMON), STATS);
    }

    public static final GunStats STATS = GunStats.builder()
            .damage(4.0F)                     // 子弹伤害：4
            .bulletSpeed(6.0F)                // 子弹速度：6 格/tick
            .bulletCount(1)                   // 子弹数量：1
            .ammo(ModItems.SHORT_BULLET)       // 弹药：短子弹
            .magazineSize(12)                 // 弹夹容量：12
            .horizontalOffset(1.2F)           // 横向偏移：±1.2°
            .verticalOffset(1.0F)            // 纵向偏移：±1.0°
            .fireMode(GunStats.FireMode.SEMI_AUTO) // 开火模式：半自动
            .fireRateTicks(6)                 // 射速：0.3 秒/发
            .recoil(1.5F)                     // 后坐力：1.5°
            .reloadTicks(30)                  // 装填时间：1.5 秒
            .reloadType(GunStats.ReloadType.MAGAZINE) // 装填方式：弹夹
            .penetration(0)                   // 穿透能力：0
            .ricochet(1)                      // 反弹能力：1（跳弹一次）
            .knockback(0.5F)                  // 击退能力：0.5
            .effectiveRange(25.0F)            // 有效射程：25 格
            .modSlots(1)                      // 改装槽位：1
            .handedness(GunStats.Handedness.ONE_HANDED) // 持枪方式：单手
            .aimZoom(1.5F)                    // 瞄准倍率：1.5×
            .build();
}
