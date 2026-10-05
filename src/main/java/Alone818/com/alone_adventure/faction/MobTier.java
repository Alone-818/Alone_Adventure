package Alone818.com.alone_adventure.faction;

import net.minecraft.network.chat.Component;

/**
 * 派系生物的等级。
 *
 * 1 级：基础兵种
 * 2 级：精锐
 * 3 级：王牌
 *
 * 等级是分类标签，具体数值加成
 * （生命、伤害、装备强度）以后
 * 在实体的属性注册或生成逻辑里按等级区分。
 */
public enum MobTier {

    /** 1 级：基础兵种 */
    TIER_1(1),

    /** 2 级：精锐 */
    TIER_2(2),

    /** 3 级：王牌 */
    TIER_3(3);

    /** 等级数字：1 / 2 / 3 */
    private final int level;

    MobTier(int level) {
        this.level = level;
    }

    /**
     * 等级数字：1 / 2 / 3。
     */
    public int getLevel() {
        return level;
    }

    /**
     * 显示名（I / II / III）。
     */
    public Component getDisplayName() {

        return Component.translatable(
                "tier.alone_adventure."
                        + name().toLowerCase()
        );
    }
}
