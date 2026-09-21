package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.entity.player.Player;

/**
 * 玩家枪械属性提供者 —— 饰品等系统实现本接口并向
 * {@link GunStatRegistry} 注册，即可为佩戴者提供枪械属性加成。
 *
 * 用法示例（未来饰品）：
 * <pre>{@code
 * GunStatRegistry.register(player -> {
 *     PlayerGunStats stats = new PlayerGunStats();
 *     if (isWearing(player)) stats.damageBonus = 2.0F;   // +2 子弹伤害
 *     return stats;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface IGunStatProvider {

    /**
     * 返回该玩家当前应获得的枪械属性加成。
     * 未佩戴任何相关饰品时返回默认值（加成 0 / 倍率 1）。
     */
    PlayerGunStats getGunStats(Player player);
}
