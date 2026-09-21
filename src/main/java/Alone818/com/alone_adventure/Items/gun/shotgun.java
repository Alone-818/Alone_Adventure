package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.item.Rarity;

/**
 * 霰弹枪 - 单手面杀伤模板
 *
 * 属性一览：
 * 子弹伤害 2（×6 弹丸）／ 子弹速度 5.0 ／ 弹丸数 6 ／ 弹夹 6（弹夹装填 2.5 秒）
 * 弹药：霰弹
 * 开火模式 手动（泵动，0.75 秒/发）／ 后坐力 4.5° ／ 偏移 ±4.0°/±3.0°
 * 穿透 0 ／ 反弹 0 ／ 击退 1.2 ／ 有效射程 12 格 ／ 改装槽位 2
 * 持枪方式 单手 ／ 瞄准倍率 1.2×
 */
public class shotgun extends GunItem {

    public shotgun() {
        super(new Properties().rarity(Rarity.UNCOMMON), STATS);
    }

    public static final GunStats STATS = GunStats.builder()
            .damage(2.0F)                     // 子弹伤害：2（每弹丸）
            .bulletSpeed(5.0F)                // 子弹速度：5 格/tick
            .bulletCount(6)                   // 子弹数量：6 弹丸
            .ammo(ModItems.SHOTGUN_SHELL)      // 弹药：霰弹
            .magazineSize(6)                  // 弹夹容量：6
            .horizontalOffset(4.0F)           // 横向偏移：±4.0°（大散布）
            .verticalOffset(3.0F)             // 纵向偏移：±3.0°
            .fireMode(GunStats.FireMode.MANUAL) // 开火模式：手动（泵动）
            .fireRateTicks(15)                // 射速：0.75 秒/发
            .recoil(4.5F)                     // 后坐力：4.5°
            .reloadTicks(50)                  // 装填时间：2.5 秒
            .reloadType(GunStats.ReloadType.MAGAZINE) // 装填方式：弹夹
            .penetration(0)                   // 穿透能力：0
            .ricochet(0)                      // 反弹能力：0
            .knockback(1.2F)                  // 击退能力：1.2
            .effectiveRange(12.0F)            // 有效射程：12 格（近距离）
            .modSlots(2)                      // 改装槽位：2
            .handedness(GunStats.Handedness.ONE_HANDED) // 持枪方式：单手
            .aimZoom(1.2F)                    // 瞄准倍率：1.2×
            .build();
}
