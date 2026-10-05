package Alone818.com.alone_adventure.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

/**
 * 派系定义。
 *
 * 派系通过 FactionManager 注册，
 * 不走 Forge 注册表：
 * 以后新增派系只需要在 ModFactions 里加一行注册代码。
 */
public class Faction {

    /**
     * 派系关系。
     */
    public enum Relation {

        /**
         * 同盟：互相不攻击，一方被攻击时另一方会支援。
         */
        ALLY,

        /**
         * 中立：不会主动锁定对方，被攻击时可以反击。
         */
        NEUTRAL,

        /**
         * 敌对：见面就打。
         */
        HOSTILE
    }

    /** 派系 id，例如 "empire" */
    private final String id;

    /** 显示名翻译键，例如 "faction.alone_adventure.empire" */
    private final String nameKey;

    /** 派系主题色，用于界面显示 */
    private final int color;

    /**
     * 是否主动索敌玩家。
     *
     * true：见到玩家就打（恶魔、亡灵）
     * false：对玩家中立，被打才反击（帝国、部落）
     */
    private final boolean hostileToPlayers;

    /**
     * 突袭血条颜色（仇恨-突袭系统）。
     *
     * 默认红色，在 ModFactions 注册时
     * 按派系指定（帝国黄 / 恶魔红 / 亡灵淡蓝 / 部落绿）。
     */
    private BossEvent.BossBarColor raidBarColor =
            BossEvent.BossBarColor.RED;

    /**
     * 只允许通过 FactionManager.register 创建。
     */
    Faction(
            String id,
            String nameKey,
            int color,
            boolean hostileToPlayers
    ) {
        this.id = id;
        this.nameKey = nameKey;
        this.color = color;
        this.hostileToPlayers = hostileToPlayers;
    }

    /**
     * 是否主动索敌玩家。
     */
    public boolean isHostileToPlayers() {
        return hostileToPlayers;
    }

    /**
     * 派系 id，例如 "empire"。
     */
    public String getId() {
        return id;
    }

    /**
     * 显示名翻译键。
     */
    public String getNameKey() {
        return nameKey;
    }

    /**
     * 派系主题色。
     */
    public int getColor() {
        return color;
    }

    /**
     * 派系显示名。
     */
    public Component getDisplayName() {
        return Component.translatable(nameKey);
    }

    /**
     * 设置突袭血条颜色（链式），默认红色。
     */
    public Faction setRaidBarColor(BossEvent.BossBarColor color) {
        this.raidBarColor = color;
        return this;
    }

    /**
     * 突袭血条颜色。
     */
    public BossEvent.BossBarColor getRaidBarColor() {
        return raidBarColor;
    }
}
