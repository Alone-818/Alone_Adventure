package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Items.parryshield;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Alone818.com.alone_adventure.Alone_adventure.MODID)
public class ParryEvent {

    // 服务端记录每个玩家本次举盾的开始时间（游戏刻），用于计算举盾时长
    private static final Map<UUID, Long> BLOCK_START = new HashMap<>();

    // 防止反弹伤害再次触发反弹造成递归（如双方都举着招架之盾对弹）
    private static boolean reflecting = false;

    /**
     * 跟踪举盾时长：开始举盾时记录时间，放下盾牌时清除记录。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务器端且在Tick的END阶段处理，避免双倍运行
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        if (isUsingParryShield(player)) {
            BLOCK_START.putIfAbsent(player.getUUID(), player.level().getGameTime());
        } else {
            BLOCK_START.remove(player.getUUID());
        }
    }

    /**
     * 判断玩家是否正在使用招架之盾（不要求举满 5 tick，瞬间招架需要）。
     */
    private static boolean isUsingParryShield(Player player) {
        return player.isUsingItem() && player.getUseItem().is(ModItems.PARRYSHIELD.get());
    }

    /**
     * 获取玩家已举盾的 tick 数。
     */
    private static long getBlockTicks(Player player) {
        Long start = BLOCK_START.get(player.getUUID());
        if (start == null) {
            // 兜底：本 tick 刚开始举盾
            return 0;
        }
        return player.level().getGameTime() - start;
    }

    /**
     * 判断玩家的盾反是否处于冷却中。
     * 使用原版物品冷却系统（与末影珍珠同款），冷却状态会自动同步到客户端，
     * 物品栏图标上会显示白色扫光遮罩，且冷却期间无法再次使用物品。
     */
    private static boolean isParryOnCooldown(Player player) {
        return player.getCooldowns().isOnCooldown(ModItems.PARRYSHIELD.get());
    }

    /**
     * 方向判定（与原版 isDamageSourceBlocked 一致）：攻击来源大致在玩家正前时才可招架。
     */
    private static boolean isFacingSource(Player player, DamageSource source) {
        Vec3 src = source.getSourcePosition();
        if (src == null) {
            return true;
        }
        Vec3 view = player.getViewVector(1.0F);
        Vec3 toPlayer = src.vectorTo(player.position()).normalize();
        toPlayer = new Vec3(toPlayer.x, 0.0D, toPlayer.z);
        return toPlayer.dot(view) < 0.0D;
    }

    /**
     * 招架判定：在 LivingAttackEvent（早于一切伤害结算和原版格挡逻辑）中处理，
     * 因此不受原版"举盾前 5 tick 不生效"的延迟影响，起手瞬间即可招架。
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (reflecting) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isUsingParryShield(player)) {
            return;
        }
        if (getBlockTicks(player) >= parryshield.PARRY_WINDOW_TICKS) {
            // 超出招架窗口，交给长举格挡逻辑处理
            return;
        }
        if (isParryOnCooldown(player)) {
            // 盾反冷却中，无法招架，交给长举格挡逻辑处理
            return;
        }
        if (!isFacingSource(player, event.getSource())) {
            return;
        }

        // 招架成功：完全免疫本次伤害，并进入盾反冷却（末影珍珠式的物品冷却显示）
        event.setCanceled(true);
        player.getCooldowns().addCooldown(ModItems.PARRYSHIELD.get(), parryshield.PARRY_COOLDOWN_TICKS);
        // 冷却期间无法举盾，强制放下当前正在使用的盾
        player.stopUsingItem();

        // 反弹 70% 伤害并给予攻击者虚弱
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            reflecting = true;
            try {
                attacker.hurt(player.damageSources().playerAttack(player),
                        event.getAmount() * parryshield.PARRY_REFLECT_RATIO);
            } finally {
                reflecting = false;
            }
            attacker.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    parryshield.WEAKNESS_DURATION_TICKS,
                    parryshield.WEAKNESS_AMPLIFIER));
        }
    }

    /**
     * 长时间举盾格挡：ShieldBlockEvent 在原版格挡结算时触发，
     * 可通过 setBlockedDamage 控制吸收比例（原版默认吸收 100%，这里改为 80%）。
     */
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getUseItem().is(ModItems.PARRYSHIELD.get())) {
            return;
        }
        // 吸收 80% 伤害，其余 20% 穿透
        event.setBlockedDamage(event.getOriginalBlockedDamage() * parryshield.LONG_BLOCK_ABSORB);
    }
}