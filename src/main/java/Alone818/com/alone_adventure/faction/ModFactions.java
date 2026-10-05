package Alone818.com.alone_adventure.faction;

import net.minecraft.world.BossEvent;

/**
 * 当前全部派系。
 *
 * 以后加新派系只需要四步：
 *
 * 1. 在这里加一行 FactionManager.register
 *    （第三个参数：是否主动索敌玩家）
 * 2. 在语言文件加显示名条目
 *    （faction.alone_adventure.<id>）
 * 3. 需要自定义关系时调 FactionManager.setRelation，
 *    不调则默认和其他派系互相敌对
 * 4. 需要生物时在 ModFactionEntities 接入
 */
public class ModFactions {

    /** 帝国：对玩家中立，主题金色 #FFD700，突袭血条黄色 */
    public static final Faction EMPIRE =
            FactionManager.register(
                            "empire",
                            0xFFD700,
                            false
                    )
                    .setRaidBarColor(
                            BossEvent.BossBarColor.YELLOW
                    );

    /** 亡灵：对玩家中立，主题淡蓝 #ADD8E6，突袭血条淡蓝 */
    public static final Faction UNDEAD =
            FactionManager.register(
                            "undead",
                            0xADD8E6,
                            false
                    )
                    .setRaidBarColor(
                            BossEvent.BossBarColor.BLUE
                    );

    /** 恶魔：对玩家中立，主题绯红 #DC143C，突袭血条红色 */
    public static final Faction DEMON =
            FactionManager.register(
                            "demon",
                            0xDC143C,
                            false
                    )
                    .setRaidBarColor(
                            BossEvent.BossBarColor.RED
                    );

    /** 部落：对玩家中立，主题青绿 #32CD32，突袭血条绿色 */
    public static final Faction TRIBE =
            FactionManager.register(
                            "tribe",
                            0x32CD32,
                            false
                    )
                    .setRaidBarColor(
                            BossEvent.BossBarColor.GREEN
                    );

    /**
     * 全部派系，按注册顺序。
     *
     * 派系系列生物（实体、属性、刷怪蛋、渲染器）
     * 都按这个列表批量展开。
     */
    public static Faction[] all() {

        return new Faction[]{
                EMPIRE,
                UNDEAD,
                DEMON,
                TRIBE
        };
    }

    /**
     * 派系生物归属注册。
     *
     * 由主 mod 构造函数调用，
     * 保证在世界加载前生效。
     */
    public static void register() {

        /*
         * 全部派系都不接入原版生物：
         * 派系系统只对模组自己的派系生物生效，
         * 原版生物保持原版行为。
         *
         * 以后接入派系生物的两条路：
         *
         * 1. 实体类实现 IFactionMob 接口（推荐）
         * 2. 在这里用 registerMember 按实体类型登记
         */
    }
}
