
package Alone818.com.alone_adventure.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 般若汤专属状态。
 *
 * 不使用玩家身上的普通效果来判断般若汤层数，
 * 避免玩家本身拥有力量效果时产生错误。
 */
public class BanruoState {

    private static final String TAG_LEVEL = "AloneAdventure_BanruoLevel";
    private static final String TAG_EXPIRY = "AloneAdventure_BanruoExpiry";
    private static final String TAG_RAMPAGE = "AloneAdventure_BanruoRampage";

    /**
     * 获取般若汤层数。
     *
     * 0 = 没有
     * 1 = 第一口
     * 2 = 第二口
     * 3 = 第三口，爆回
     * 4 = 第四口及以后
     */
    public static int getLevel(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.getInt(TAG_LEVEL);
    }

    /**
     * 设置般若汤层数。
     */
    public static void setLevel(Player player, int level) {
        CompoundTag data = player.getPersistentData();

        int value = Math.max(0, Math.min(level, 4));

        if (value <= 0) {
            data.remove(TAG_LEVEL);
        } else {
            data.putInt(TAG_LEVEL, value);
        }
    }

    /**
     * 获取般若汤效果结束时间。
     *
     * 使用服务器游戏时间。
     */
    public static long getExpiry(Player player) {
        return player.getPersistentData().getLong(TAG_EXPIRY);
    }

    /**
     * 设置般若汤效果结束时间。
     */
    public static void setExpiry(Player player, long expiry) {
        player.getPersistentData().putLong(TAG_EXPIRY, expiry);
    }

    /**
     * 是否处于爆回状态。
     */
    public static boolean isRampage(Player player) {
        return player.getPersistentData().getBoolean(TAG_RAMPAGE);
    }

    /**
     * 设置爆回状态。
     */
    public static void setRampage(Player player, boolean rampage) {
        player.getPersistentData().putBoolean(TAG_RAMPAGE, rampage);
    }

    /**
     * 开始/刷新般若汤状态。
     */
    public static void drink(Player player, long currentGameTime) {

        int level = getLevel(player);

        // 增加一层，最高4层
        level = Math.min(level + 1, 4);

        setLevel(player, level);

        // 每次饮用重新获得60秒持续时间
        setExpiry(
                player,
                currentGameTime + 20L * 60L
        );

        /*
         * 第三口开始爆回。
         *
         * 一旦达到第三口，就进入爆回。
         */
        if (level >= 3) {
            setRampage(player, true);
        }
    }

    /**
     * 检查般若汤效果是否已经结束。
     */
    public static boolean isExpired(Player player, long currentGameTime) {
        long expiry = getExpiry(player);

        return expiry <= 0 || currentGameTime >= expiry;
    }

    /**
     * 清除般若汤状态。
     */
    public static void clear(Player player) {
        CompoundTag data = player.getPersistentData();

        data.remove(TAG_LEVEL);
        data.remove(TAG_EXPIRY);
        data.remove(TAG_RAMPAGE);
    }
}
