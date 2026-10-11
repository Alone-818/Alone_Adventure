package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.trait.chronos_reagent;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 克罗诺斯试剂 - 效果处理
 *
 * 效果：
 * - 连续攻击之间不受到攻击将获得可堆叠的力量，上限为 9
 * - 受击后清除力量，获得虚弱等级 3 时间 7 秒
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ChronosReagentEvent {

    /** 力量上限等级 */
    private static final int MAX_STRENGTH_LEVEL = 9;
    /** 虚弱时长（tick）：7 秒 = 140 tick */
    private static final int WEAKNESS_DURATION_TICKS = 140;
    /** 虚弱等级：等级 3 = amplifier 2 */
    private static final int WEAKNESS_AMPLIFIER = 2;

    /**
     * 判断玩家是否佩戴克罗诺斯试剂
     */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.CHRONOS_REAGENT.get())
                .isPresent();
    }

    /**
     * 攻击时增加力量层数
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!isWearing(player)) return;

        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(serverPlayer, ModItems.CHRONOS_REAGENT.get());
        if (curioOpt.isEmpty()) return;

        // 获取当前力量等级 (DAMAGE_BOOST = 伤害提升)
        MobEffectInstance strengthEffect = serverPlayer.getEffect(MobEffects.DAMAGE_BOOST);
        int currentStrength = 0;
        if (strengthEffect != null) {
            currentStrength = strengthEffect.getAmplifier() + 1;
        }

        // 增加力量等级（上限 9）
        if (currentStrength < MAX_STRENGTH_LEVEL) {
            int newLevel = Math.min(currentStrength + 1, MAX_STRENGTH_LEVEL);
            serverPlayer.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    200, // 10 秒持续时间
                    newLevel - 1, // amplifier = level - 1
                    false, // 不是持久效果
                    true   // 显示粒子
            ));
        }
    }

    /**
     * 受击后清除力量，获得虚弱 III 7 秒
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;

        // 清除力量效果 (DAMAGE_BOOST)
        player.removeEffect(MobEffects.DAMAGE_BOOST);

        // 添加虚弱 III 7 秒 (DAMAGE_TAKE = 受到伤害增加)
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                WEAKNESS_DURATION_TICKS,
                WEAKNESS_AMPLIFIER,
                false,
                true
        ));
    }
}
