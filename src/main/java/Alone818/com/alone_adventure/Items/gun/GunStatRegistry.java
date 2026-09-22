package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家枪械属性提供者注册表 —— 收集一名玩家所有来源的枪械加成。
 *
 * <p>此类保留作为兼容层，实际实现已迁移至 {@link GunModItems}。
 * 建议在模组加载期注册饰品提供者：
 * <pre>{@code
 * GunStatRegistry.registerProvider("my_gun_perk", myProvider);
 * }</pre>
 *
 * <p>注意：GunModItems.collectAll() 是主入口，GunStatRegistry.collect() 已弃用。
 */
@Deprecated
public final class GunStatRegistry {

    private static final List<IGunStatProvider> PROVIDERS = new ArrayList<>();

    private GunStatRegistry() {
    }

    /**
     * 注册一个玩家枪械属性提供者（已弃用，建议使用 GunModItems.registerProvider）
     */
    @Deprecated
    public static void register(IGunStatProvider provider) {
        PROVIDERS.add(provider);
    }

    /**
     * 汇总一名玩家所有来源的枪械加成（已弃用，建议使用 GunModItems.collectAll）
     */
    @Deprecated
    public static PlayerGunStats collect(Player player) {
        PlayerGunStats total = PlayerGunStats.neutral();
        for (IGunStatProvider provider : PROVIDERS) {
            total.merge(provider.getGunStats(player));
        }
        return total;
    }
}