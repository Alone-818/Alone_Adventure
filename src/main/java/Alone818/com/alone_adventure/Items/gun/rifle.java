package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.item.Rarity;

/**
 * 步枪 - 双手长枪模板
 *
 * 属性一览：
 * 子弹伤害 8 ／ 子弹速度 8.0 ／ 弹丸数 1 ／ 弹夹 5（弹夹装填 3 秒）
 * 弹药：长子弹 + 弩箭弹药（G 键切换）
 * 开火模式 手动（栓动，1 秒/发）／ 后坐力 3.0° ／ 偏移 ±0.6°/±0.4°
 * 穿透 2 ／ 反弹 0 ／ 击退 0.4 ／ 有效射程 60 格 ／ 改装槽位 3
 * 持枪方式 双手（仅主手可用）／ 瞄准倍率 3×
 */
public class rifle extends GunItem {

    public rifle() {
        super(new Properties().rarity(Rarity.UNCOMMON), STATS);
    }

    public static final GunStats STATS = GunStats.builder()
            .damage(8.0F)                     // 子弹伤害：8
            .bulletSpeed(8.0F)                // 子弹速度：8 格/tick
            .bulletCount(1)                   // 子弹数量：1
            .ammo(ModItems.LONG_BULLET, ModItems.CROSSBOW_BOLT) // 弹药：长子弹/弩箭弹药（G 切换）
            .magazineSize(5)                  // 弹夹容量：5
            .horizontalOffset(0.6F)           // 横向偏移：±0.6°
            .verticalOffset(0.4F)             // 纵向偏移：±0.4°
            .fireMode(GunStats.FireMode.MANUAL) // 开火模式：手动（栓动）
            .fireRateTicks(20)                // 射速：1 秒/发
            .recoil(3.0F)                     // 后坐力：3.0°
            .reloadTicks(60)                  // 装填时间：3 秒
            .reloadType(GunStats.ReloadType.MAGAZINE) // 装填方式：弹夹
            .penetration(2)                   // 穿透能力：2
            .ricochet(0)                      // 反弹能力：0
            .knockback(0.4F)                  // 击退能力：0.4
            .effectiveRange(60.0F)            // 有效射程：60 格
            .modSlots(3)                      // 改装槽位：3
            .handedness(GunStats.Handedness.TWO_HANDED) // 持枪方式：双手
            .aimZoom(3.0F)                    // 瞄准倍率：3×
            .build();
}
