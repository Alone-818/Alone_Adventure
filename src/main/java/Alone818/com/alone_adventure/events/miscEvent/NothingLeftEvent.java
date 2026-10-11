package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.trait.nothing_left;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 一无所有 - 效果处理
 *
 * 效果：
 * - 10s 内未受到伤害则快速回复生命值
 * - 常态获得抗性提升 II
 * - 减少 20% 护甲值与护甲韧性
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class NothingLeftEvent {

    /** 未受伤害时间阈值（tick）：10 秒 = 200 tick */
    private static final int DAMAGE_FREE_THRESHOLD_TICKS = 200;
    /** 回复间隔（tick）：每秒回复 1 点 = 20 tick */
    private static final int REGEN_INTERVAL_TICKS = 20;
    /** 抗性提升等级：抗性 II = amplifier 1 */
    private static final int RESISTANCE_AMPLIFIER = 1;

    /**
     * 判断玩家是否佩戴一无所有
     */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.NOTHING_LEFT.get())
                .isPresent();
    }

    /**
     * 受击时重置计时器并移除抗性效果
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;

        // 移除抗性效果（等待 10 秒后重新添加）
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    /**
     * 检测未受伤害时间，回复生命值和添加抗性
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;

        // 每 20 tick（1 秒）检测一次
        if (player.tickCount % REGEN_INTERVAL_TICKS != 0) return;

        // 检查是否拥有抗性效果（说明已经过了 10 秒未受伤害）
        if (player.getEffect(MobEffects.DAMAGE_RESISTANCE) == null) {
            // 添加抗性 II
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    DAMAGE_FREE_THRESHOLD_TICKS,
                    RESISTANCE_AMPLIFIER,
                    false,
                    true
            ));

            // 回复生命值（每秒 1 点）
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0f);
            }
        }
    }
}
