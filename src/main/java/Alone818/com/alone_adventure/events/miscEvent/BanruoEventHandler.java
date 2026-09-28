package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.state.BanruoState;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = "alone_adventure",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class BanruoEventHandler {

    // 爆回攻击半径：3格
    private static final double ATTACK_RADIUS = 3.0D;

    // 每5 tick攻击一次
    private static final int ATTACK_INTERVAL = 5;

    // 范围攻击伤害倍率
    // 1.0 = 100%
    // 0.5 = 50%
    // 2.0 = 200%
    private static final float DAMAGE_MULTIPLIER = 0.2F;


    /**
     * 玩家Tick。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        // 只处理每个Tick结束阶段
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        // 只在服务器执行
        if (player.level().isClientSide) {
            return;
        }

        /*
         * 没有爆回就不处理。
         */
        if (!BanruoState.isRampage(player)) {
            return;
        }

        long gameTime = player.level().getGameTime();

        /*
         * 般若汤效果结束
         *
         * → 自动结束爆回
         */
        if (BanruoState.isExpired(player, gameTime)) {

            BanruoState.clear(player);

            return;
        }

        /*
         * 每5 tick进行一次范围攻击。
         */
        if (gameTime % ATTACK_INTERVAL != 0) {
            return;
        }

        attackNearbyMonsters(player);
    }


    /**
     * 攻击玩家周围范围内的怪物。
     */
    private static void attackNearbyMonsters(Player player) {

        List<Monster> monsters =
                player.level().getEntitiesOfClass(
                        Monster.class,
                        player.getBoundingBox().inflate(ATTACK_RADIUS),
                        monster -> {

                            if (!monster.isAlive()) {
                                return false;
                            }

                            if (monster.isSpectator()) {
                                return false;
                            }

                            return true;
                        }
                );


        /*
         * 玩家当前攻击力 × 伤害倍率
         *
         * 会受到：
         * - 武器
         * - 力量
         * - 其他攻击力属性
         *
         * 的影响。
         */
        float damage = (float) player.getAttributeValue(
                Attributes.ATTACK_DAMAGE
        ) * DAMAGE_MULTIPLIER;


        if (damage <= 0.0F) {
            return;
        }


        /*
         * 对附近每一个怪物造成范围伤害。
         */
        for (Monster monster : monsters) {

            if (!monster.isAlive()) {
                continue;
            }

            monster.hurt(
                    player.damageSources().playerAttack(player),
                    damage
            );
        }
    }
}