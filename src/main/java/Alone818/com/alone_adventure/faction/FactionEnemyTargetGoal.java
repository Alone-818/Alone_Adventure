package Alone818.com.alone_adventure.faction;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * 派系敌对索敌目标。
 *
 * 让生物主动锁定敌对派系的生物。
 *
 * 以后新增派系生物时，在 targetSelector 里挂上：
 *
 *     this.targetSelector.addGoal(
 *             2,
 *             new FactionEnemyTargetGoal(this, true)
 *     );
 *
 * 对玩家的态度不用在这里写：
 * 由派系定义（ModFactions 注册时的第三个参数）
 * 加上仇恨-突袭系统（RaidManager）实时解析——
 *
 * - 派系 hostileToPlayers = true：见到玩家就打（恶魔、亡灵）
 * - 派系 hostileToPlayers = false（帝国、部落）：
 *   仇恨过临界线或突袭进行中才主动索敌
 *   （见 RaidManager.isHostileToPlayer），
 *   否则无视玩家，被打才反击
 *
 * 创造 / 旁观玩家无论哪个派系都永远不会被主动索敌。
 *
 * 被动反击不用额外写：
 * 生物被攻击时会锁定攻击者，
 * 同派系误伤由 FactionEvent 全局过滤。
 */
public class FactionEnemyTargetGoal
        extends NearestAttackableTargetGoal<LivingEntity> {

    /**
     * @param mob     挂载这个目标的生物
     * @param mustSee 是否要求看见目标才锁定
     */
    public FactionEnemyTargetGoal(
            Mob mob,
            boolean mustSee
    ) {

        super(
                mob,
                LivingEntity.class,
                mustSee,
                living ->
                        isEnemy(
                                mob,
                                living
                        )
        );
    }

    /**
     * 玩家按所属派系的态度 + 仇恨决定，
     * 其他生物由派系关系决定。
     */
    private static boolean isEnemy(
            Mob mob,
            LivingEntity living
    ) {

        if (living == mob) {
            return false;
        }

        if (living instanceof Player player) {

            return (factionWantsPlayers(mob)
                    || RaidManager.isAngeredAt(
                    mob,
                    player
            ))
                    && !player.isCreative()
                    && !player.isSpectator();
        }

        return FactionManager.isHostile(mob, living);
    }

    /**
     * 生物所属派系是否主动索敌玩家。
     *
     * 索敌时实时解析：
     * registerGoals 在实体构造器里被调用，
     * 那时 faction 字段还没赋值，
     * 不能在注册目标时冻结这个判断。
     */
    private static boolean factionWantsPlayers(Mob mob) {

        if (!(mob instanceof IFactionMob factionMob)) {
            return false;
        }

        Faction faction =
                factionMob.getFaction();

        return faction != null
                && faction.isHostileToPlayers();
    }
}
