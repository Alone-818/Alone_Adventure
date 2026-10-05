package Alone818.com.alone_adventure.faction;

import net.minecraft.util.RandomSource;

/**
 * 派系生物的职业。
 *
 * 职业决定战斗 AI 与升级路线，
 * 军衔（PromotionTier）决定实体类型与数值档位。
 *
 * 实体 id = 派系id_军衔名，例如：
 *
 * empire_soldier / empire_sergeant / empire_general
 * empire_archer / empire_crossbowman / empire_repeater
 * empire_mage / empire_archmage / empire_magus
 */
public enum MobClass {

    /** 近战职业：士兵 -> 中士 -> 将军 */
    SOLDIER(
            new String[]{"soldier", "sergeant", "general"},
            new int[]{0xFFFFFF, 0xFFD75C, 0xFF7F27}
    ),

    /** 远程职业：弓手 -> 弩手 -> 连弩手 */
    ARCHER(
            new String[]{"archer", "crossbowman", "repeater"},
            new int[]{0x9C9C9C, 0x8FB6D9, 0x4FA4E8}
    ),

    /** 辅助职业：法师 -> 大法师 -> 魔导师 */
    MAGE(
            new String[]{"mage", "archmage", "magus"},
            new int[]{0xAA66FF, 0x8844CC, 0xDD99FF}
    );

    /** 各军衔的实体 id 后缀（按 PromotionTier 顺序） */
    private final String[] suffixes;

    /** 各军衔的刷怪蛋副色（主色为派系色） */
    private final int[] eggColors;

    MobClass(String[] suffixes, int[] eggColors) {
        this.suffixes = suffixes;
        this.eggColors = eggColors;
    }

    /**
     * 实体 id 后缀，
     * 例如 SOLDIER + ELITE -> "general"。
     */
    public String getSuffix(PromotionTier tier) {
        return suffixes[tier.ordinal()];
    }

    /**
     * 刷怪蛋副色，按军衔渐变。
     */
    public int getEggColor(PromotionTier tier) {
        return eggColors[tier.ordinal()];
    }

    /**
     * 按标准职业权重随机抽取：
     * 士兵 60% / 弓手 25% / 法师 15%。
     *
     * 突袭波次与巡逻战团共用这一份权重，
     * 保证两种生成方式的队伍结构一致。
     */
    public static MobClass random(RandomSource random) {

        float roll =
                random.nextFloat();

        if (roll < 0.60F) {
            return SOLDIER;
        }

        if (roll < 0.85F) {
            return ARCHER;
        }

        return MAGE;
    }
}
