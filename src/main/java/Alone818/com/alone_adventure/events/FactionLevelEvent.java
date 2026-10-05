package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 派系生物的等级成长触发。
 *
 * 派系生物每击杀一名敌人 +1 级：
 *
 * 敌人 = 玩家，或属于敌对派系的生物。
 *
 * 3 级再升级晋升军衔，
 * 详见 FactionMobEntity。
 *
 * 玩家击杀派系生物则反过来积累仇恨，
 * 见 RaidManager（仇恨-突袭系统）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class FactionLevelEvent {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {

        LivingEntity victim =
                event.getEntity();

        if (victim.level().isClientSide) {
            return;
        }

        /*
         * 玩家击杀派系生物：
         * 积累该派系仇恨（突袭系统的入口）。
         */
        if (event.getSource()
                .getEntity()
                instanceof ServerPlayer player) {

            Faction victimFaction =
                    FactionManager.getFaction(victim);

            if (victimFaction != null) {

                RaidManager.onPlayerKillFactionMob(
                        player,
                        victimFaction
                );
            }
        }

        if (!(event.getSource()
                .getEntity()
                instanceof FactionMobEntity killer)) {

            return;
        }

        if (victim == killer) {
            return;
        }

        /*
         * 玩家永远算有效击杀，
         * 其他生物必须是敌对派系才算。
         */
        if (!(victim instanceof Player)) {

            Faction victimFaction =
                    FactionManager.getFaction(victim);

            if (victimFaction == null
                    || !FactionManager.isHostile(
                    killer.getFaction(),
                    victimFaction
            )) {

                return;
            }
        }

        /*
         * 先结算军衔技能（将军击杀回血等），
         * 再结算升级：晋升会原地替换实体，
         * 顺序反了回血会回给被替换掉的旧实体。
         */
        LOGGER.info(
                "[Faction] {} killed {} (valid kill)",
                killer.getType().toShortString(),
                victim.getType().toShortString()
        );

        killer.onEnemyKilled();

        killer.gainKillLevel();
    }
}
