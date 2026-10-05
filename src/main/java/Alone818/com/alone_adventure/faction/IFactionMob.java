package Alone818.com.alone_adventure.faction;

/**
 * 派系生物接口。
 *
 * 以后新增派系生物时实现这个接口，
 * 生物的分类（派系、攻击方式、等级）
 * 以其返回值为准：
 *
 * public class EmpireArcher extends PathfinderMob
 *         implements IFactionMob {
 *
 *     @Override
 *     public Faction getFaction() {
 *         return ModFactions.EMPIRE;
 *     }
 *
 *     // 远程兵种覆写为 RANGED
 *     @Override
 *     public AttackType getAttackType() {
 *         return AttackType.RANGED;
 *     }
 *
 *     // 2 级精锐
 *     @Override
 *     public MobTier getTier() {
 *         return MobTier.TIER_2;
 *     }
 * }
 *
 * 主动索敌目标挂 FactionEnemyTargetGoal；
 * 攻击方式与等级是分类标签，
 * 具体的攻击 AI 与数值加成由实体自己按分类实现。
 */
public interface IFactionMob {

    /**
     * 这个生物属于哪个派系。
     */
    Faction getFaction();

    /**
     * 攻击方式。
     *
     * 默认近战，远程兵种覆写为 RANGED。
     */
    default AttackType getAttackType() {
        return AttackType.MELEE;
    }

    /**
     * 等级。
     *
     * 默认 1 级，精锐 / 王牌覆写为更高等级。
     */
    default MobTier getTier() {
        return MobTier.TIER_1;
    }
}
