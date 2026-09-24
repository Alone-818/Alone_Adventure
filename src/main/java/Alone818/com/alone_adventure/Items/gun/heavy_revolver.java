package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.item.Rarity;

/**
 * 重型左轮 - 逐发装填模板
 *
 * 属性一览：
 * 子弹伤害 10 ／ 子弹速度 7.0 ／ 弹丸数 1 ／ 弹夹 6（逐发装填）
 * 弹药：短子弹
 * 开火模式 手动（逐发上膛，较长开火间隔）／ 后坐力 6.0° ／ 偏移 ±0.8°/±0.6°
 * 穿透 1 ／ 反弹 1 ／ 击退 0.8 ／ 有效射程 30 格 ／ 改装槽位 2
 * 持枪方式 双手（仅主手可用）／ 瞄准倍率 2.5×
 */
public class heavy_revolver extends GunItem {

    public heavy_revolver() {
        super(new Properties().rarity(Rarity.RARE), STATS);
    }

    public static final GunStats STATS = GunStats.builder()
            .damage(15.0F)                    // 子弹伤害：5
            .bulletSpeed(7.0F)                // 子弹速度：7 格/tick
            .bulletCount(1)                   // 子弹数量：1
            .ammo( ModItems.SHORT_BULLET,
                    ModItems.SHORT_BULLET_DUM,
                    ModItems.SHORT_BULLET_EXPLOSIVE,
                    ModItems.SHORT_BULLET_COIN,
                    ModItems.SHORT_BULLET_POISON,
                    ModItems.SHORT_BULLET_ARMOR_PIERCING)     // 弹药：短子弹
            .magazineSize(6)                  // 弹夹容量：6
            .horizontalOffset(0.8F)           // 横向偏移：±0.8°（小散布）
            .verticalOffset(0.6F)            // 纵向偏移：±0.6°
            .fireMode(GunStats.FireMode.MANUAL) // 开火模式：手动（逐发）
            .fireRateTicks(25)                // 射速：1.25 秒/发（较慢）
            .recoil(6.0F)                     // 后坐力：6.0°（大）
            .reloadTicks(16)                  // 装填时间：0.8 秒（逐发）
            .reloadType(GunStats.ReloadType.PER_BULLET) // 装填方式：逐发
            .penetration(1)                   // 穿透能力：1
            .ricochet(1)                      // 反弹能力：1
            .knockback(0.8F)                  // 击退能力：0.8
            .effectiveRange(30.0F)            // 有效射程：30 格
            .modSlots(2)                      // 改装槽位：2
            .handedness(GunStats.Handedness.ONE_HANDED) // 持枪方式：单手
            .aimZoom(1.1F)                    // 瞄准倍率：2.5×
            .build();
}
