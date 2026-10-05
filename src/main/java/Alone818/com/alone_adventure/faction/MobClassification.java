package Alone818.com.alone_adventure.faction;

/**
 * 一个实体类型的完整派系分类：
 *
 * 派系 + 攻击方式 + 等级。
 *
 * 用于按实体类型注册的生物（原版生物）。
 * 模组生物不经过这里，
 * 直接实现 IFactionMob 自报分类。
 *
 * 只允许通过 FactionManager.registerMember 创建。
 */
public final class MobClassification {

    /** 所属派系 */
    private final Faction faction;

    /** 攻击方式 */
    private final AttackType attackType;

    /** 等级 */
    private final MobTier tier;

    MobClassification(
            Faction faction,
            AttackType attackType,
            MobTier tier
    ) {
        this.faction = faction;
        this.attackType = attackType;
        this.tier = tier;
    }

    /**
     * 所属派系。
     */
    public Faction getFaction() {
        return faction;
    }

    /**
     * 攻击方式。
     */
    public AttackType getAttackType() {
        return attackType;
    }

    /**
     * 等级。
     */
    public MobTier getTier() {
        return tier;
    }
}
