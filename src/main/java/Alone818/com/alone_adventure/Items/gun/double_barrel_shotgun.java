package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class double_barrel_shotgun extends GunItem{
    public double_barrel_shotgun()  {
        super(new Item.Properties().rarity(Rarity.RARE), STATS);
    }
    public static final GunStats STATS = GunStats.builder()
            .damage(3.0F)                    // 子弹伤害：5
            .bulletSpeed(4.0F)                // 子弹速度：7 格/tick
            .bulletCount(12)                   // 子弹数量：1
            .ammo( ModItems.SHOTGUN_SHELL,
                    ModItems.SHOTGUN_SHELL_DUM,
                    ModItems.SHOTGUN_SHELL_EXPLOSIVE,
                    ModItems.SHOTGUN_SHELL_POISON)     // 弹药：短子弹
            .magazineSize(2)                  // 弹夹容量：6
            .horizontalOffset(7F)           // 横向偏移：±0.8°（小散布）
            .verticalOffset(7F)            // 纵向偏移：±0.6°
            .fireMode(GunStats.FireMode.MANUAL) // 开火模式：手动（逐发）
            .fireRateTicks(30)                // 射速：1.25 秒/发（较慢）
            .recoil(12.0F)                     // 后坐力：6.0°（大）
            .reloadTicks(25)                  // 装填时间：0.8 秒（逐发）
            .reloadType(GunStats.ReloadType.PER_BULLET) // 装填方式：逐发
            .penetration(0)                   // 穿透能力：1
            .ricochet(2)                      // 反弹能力：1
            .knockback(0.2F)                  // 击退能力：0.8
            .effectiveRange(12.0F)            // 有效射程：30 格
            .modSlots(2)                      // 改装槽位：2
            .handedness(GunStats.Handedness.TWO_HANDED) // 持枪方式：单手
            .aimZoom(1.1F)                    // 瞄准倍率：2.5×
            .build();
}

