package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 枪械饰品属性管理 —— 注册、收集并应用佩戴者的枪械属性加成。
 *
 * <p>设计思路：
 * <ul>
 *   <li>饰品实现 {@link IGunStatProvider} 接口，通过 {@link #registerProvider} 注册到此管理类</li>
 *   <li>每类饰品注册一个名称（用于 tooltip 显示），并在 {@link PlayerGunStats} 中存储加成</li>
 *   <li>玩家穿戴时，{@link #collectAll(Player)} 汇总所有饰品的加成</li>
 *   <li>加成支持：<b>数值属性</b>（如伤害、弹速）和<b>倍率属性</b>（如装填速度、射速）</li>
 * </ul>
 *
 * <p>用法示例（饰品）：
 * <pre>{@code
 * // 在饰品类的构造器或注册期调用：
 * GunModItems.registerProvider("hunter_serum", player -> {
 *     PlayerGunStats stats = new PlayerGunStats();
 *     if (isWearing(player)) {
 *         stats.damageBonus = 2.0F;          // +2 子弹伤害
 *         stats.fireRateMultiplier = 1.2F;   // 射速提升 20%
 *     }
 *     return stats;
 * });
 * }</pre>
 */
public class GunModItems {

    /** 已注册的饰品提供者（名称 → 提供者函数） */
    private static final Map<String, IGunStatProvider> PROVIDERS = new HashMap<>();

    private GunModItems() {
    }

    /**
     * 注册一个枪械饰品属性提供者。
     *
     * @param name     饰品名称标识（用于 tooltip 显示，可与实际物品 ID 不同）
     * @param provider 属性提供者函数
     */
    public static void registerProvider(String name, IGunStatProvider provider) {
        PROVIDERS.put(name, provider);
    }

    /**
     * 收集玩家佩戴的所有枪械饰品的属性加成。
     * 若无饰品穿戴，返回默认值（全部 0/1）。
     *
     * @param player 玩家实体
     * @return 累计的枪械属性加成
     */
    public static PlayerGunStats collectAll(Player player) {
        PlayerGunStats total = PlayerGunStats.neutral();
        for (IGunStatProvider provider : PROVIDERS.values()) {
            total.merge(provider.getGunStats(player));
        }
        return total;
    }

    /**
     * 获取所有已注册的饰品名称列表（用于 tooltip 动态生成）。
     *
     * @return 饰品名称列表
     */
    public static Iterable<String> getRegisteredNames() {
        return PROVIDERS.keySet();
    }

    /**
     * 检查指定名称的饰品是否已注册。
     *
     * @param name 饰品名称
     * @return 是否已注册
     */
    public static boolean isRegistered(String name) {
        return PROVIDERS.containsKey(name);
    }
}