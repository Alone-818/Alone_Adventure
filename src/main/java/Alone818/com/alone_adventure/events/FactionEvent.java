package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
import Alone818.com.alone_adventure.faction.IFactionMob;
import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 派系行为规则。
 *
 * 1. 目标锁定过滤：
 *    派系生物不允许锁定同派系 / 同盟目标，
 *    防止误伤、反弹、引怪打到自己人。
 *    创造 / 旁观玩家也永远不会被锁定。
 *
 * 2. 同派系支援：
 *    模组派系生物（IFactionMob）被攻击时，
 *    附近同派系的模组派系生物会锁定攻击者。
 *
 * 3. 玩家派系保护：
 *    加入派系的玩家无法对该派系生物造成伤害
 *    （伤害事件拦截，直接归零）。
 *
 * 支援只在模组生物之间生效：
 * 原版生物（例如划入亡灵派系的原版亡灵）
 * 不会引来支援，也不会响应支援，
 * 保持原版行为。
 *
 * 所有派系对玩家默认中立：
 * 不主动索敌玩家，玩家先动手才反击。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class FactionEvent {

    /** 同派系支援半径（格） */
    public static final double ASSIST_RADIUS = 12.0D;

    /**
     * 目标锁定过滤。
     */
    @SubscribeEvent
    public static void onChangeTarget(
            LivingChangeTargetEvent event
    ) {

        LivingEntity target =
                event.getNewTarget();

        if (target == null) {
            return;
        }

        /*
         * 创造 / 旁观玩家永远不会被派系生物锁定。
         *
         * 这条守在 setTarget 的唯一入口上，
         * 覆盖所有来源：
         * 主动索敌、被打反击（HurtByTarget）、支援集火。
         */
        if (target instanceof Player player
                && (player.isCreative()
                || player.isSpectator())) {

            event.setCanceled(true);
            return;
        }

        Faction own =
                FactionManager.getFaction(
                        event.getEntity()
                );

        if (own == null) {
            return;
        }

        Faction targetFaction =
                FactionManager.getFaction(target);

        if (targetFaction == null) {
            return;
        }

        /*
         * 同派系生物之间不会互相锁定。
         *
         * 但当玩家（没有派系）被攻击时，支援代码会调用 setTarget()，
         * 这个需求需要允许它们锁定玩家。
         * 所以这里只阻止同派系生物锁定彼此。
         *
         * 如果攻击者是加入派系的玩家，
         * 同派系生物应该锁定该玩家。
         */
        if (own == targetFaction) {
            // 如果目标是玩家且有派系，检查是否是同派系玩家
            if (target instanceof Player targetPlayer) {
                Faction targetPlayerFaction = RaidManager.getPlayerFaction(targetPlayer.getUUID());
                if (targetPlayerFaction == own) {
                    event.setCanceled(true); // 同派系玩家，不允许锁定
                }
                // 否则（不同派系或无派系玩家）允许锁定
            } else {
                event.setCanceled(true); // 非玩家目标，同派系不允许锁定
            }
        }
    }

    /**
     * 同派系支援 & 玩家派系保护。
     */
    @SubscribeEvent
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {

        LivingEntity victim =
                event.getEntity();

        if (victim.level().isClientSide) {
            return;
        }

        if (!(event.getSource()
                .getEntity()
                instanceof LivingEntity attacker)) {

            return;
        }

        if (attacker == victim) {
            return;
        }

        // ========== 玩家派系保护 ==========
        // 加入派系的玩家无法对该派系生物造成伤害
        if (attacker instanceof Player player) {
            Faction playerFaction = RaidManager.getPlayerFaction(player.getUUID());

            if (playerFaction != null) {
                Faction victimFaction = FactionManager.getFaction(victim);

                if (playerFaction == victimFaction) {
                    // 同派系玩家无法伤害该派系生物
                    event.setAmount(0);
                    event.setCanceled(true);
                    return;
                }
            }
        }

        /*
         * 支援只在模组派系生物之间启用：
         * 被打的必须是模组派系生物。
         */
        if (!(victim instanceof IFactionMob)) {
            return;
        }

        Faction victimFaction =
                FactionManager.getFaction(victim);

        if (victimFaction == null) {
            return;
        }

        /*
         * 同派系内部误伤：
         * 不触发支援（锁定也已被上面的过滤取消）。
         *
         * 但玩家加入派系后攻击其他派系生物，
         * 应该触发同派系支援。
         */
        Faction attackerFaction = FactionManager.getFaction(attacker);

        // 如果是玩家且有派系，用玩家派系判断
        if (attacker instanceof Player player) {
            Faction playerFaction = RaidManager.getPlayerFaction(player.getUUID());
            if (playerFaction == victimFaction) {
                return; // 同派系玩家，不触发支援
            }
        } else if (attackerFaction == victimFaction) {
            return; // 同派系生物，不触发支援
        }

        if (!(victim.level()
                instanceof ServerLevel server)) {

            return;
        }

        /*
         * 附近同派系生物锁定攻击者。
         *
         * 攻击者没有派系（玩家、铁傀儡等）时
         * 同样会触发：打派系生物就是与整个派系为敌。
         */
        AABB area =
                victim.getBoundingBox()
                        .inflate(ASSIST_RADIUS);

        for (LivingEntity nearby :
                server.getEntitiesOfClass(
                        LivingEntity.class,
                        area
                )) {

            if (nearby == victim
                    || nearby == attacker) {

                continue;
            }

            if (!(nearby instanceof Mob mob)) {
                continue;
            }

            /*
             * 响应支援的也必须是模组派系生物。
             */
            if (!(mob instanceof IFactionMob)) {
                continue;
            }

            if (FactionManager.getFaction(mob)
                    != victimFaction) {

                continue;
            }

            mob.setTarget(attacker);
        }
    }
}
