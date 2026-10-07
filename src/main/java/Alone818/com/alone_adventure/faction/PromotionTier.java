package Alone818.com.alone_adventure.faction;

/**
 * 派系生物的军衔（大等级）。
 *
 * 军衔是实体类型级别的差异：
 * 每个军衔都是独立注册的生物，
 * 有自己的实体 id、刷怪蛋与名字。
 *
 * 生物升到 3 级再击杀一名敌人时，
 * 原地蜕变成下一军衔的实体，等级回到 1 级：
 *
 * 士兵 -> 中士 -> 将军
 * 弓手 -> 弩手 -> 连弩手
 * 法师 -> 大法师 -> 魔导师
 */
public enum PromotionTier {

    /** 基础兵：士兵 / 弓手 / 法师 */
    BASE(1.0F),

    /** 一阶军衔：中士 / 弩手 / 大法师 */
    ADVANCED(1.2F),

    /** 二阶军衔（封顶）：将军 / 连弩手 / 魔导师 */
    ELITE(1.5F);

    /** 军衔基础属性倍率 */
    private final float baseStat;

    PromotionTier(float baseStat) {
        this.baseStat = baseStat;
    }

    /**
     * 军衔基础属性倍率：
     * 基础兵 100% / 一阶 120% / 二阶 150%。
     */
    public float getBaseStat() {
        return baseStat;
    }

    /**
     * 是否已是最高军衔。
     */
    public boolean isMax() {
        return this == ELITE;
    }

    /**
     * 下一军衔，最高军衔返回 null。
     */
    public PromotionTier next() {

        return isMax()
                ? null
                : values()[ordinal() + 1];
    }
}
