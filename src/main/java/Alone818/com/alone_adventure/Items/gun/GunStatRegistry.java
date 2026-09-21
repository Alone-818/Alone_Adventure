package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家枪械属性提供者注册表 —— 收集一名玩家所有来源的枪械加成。
 *
 * 当前未注册任何提供者：{@link #collect} 返回默认值（全部加成为 0），
 * 后续饰品/装备实现 {@link IGunStatProvider} 注册进来即可无缝生效。
 * 建议在模组构造或 commonSetup 阶段注册（只增不删，无需并发保护）。
 */
public final class GunStatRegistry {

    private static final List<IGunStatProvider> PROVIDERS = new ArrayList<>();

    private GunStatRegistry() {
    }

    /** 注册一个玩家枪械属性提供者（可在任何时机调用，仅建议注册期使用） */
    public static void register(IGunStatProvider provider) {
        PROVIDERS.add(provider);
    }

    /** 汇总一名玩家所有来源的枪械加成（无提供者时返回默认值） */
    public static PlayerGunStats collect(Player player) {
        PlayerGunStats total = PlayerGunStats.neutral();
        for (IGunStatProvider provider : PROVIDERS) {
            total.merge(provider.getGunStats(player));
        }
        return total;
    }
}
