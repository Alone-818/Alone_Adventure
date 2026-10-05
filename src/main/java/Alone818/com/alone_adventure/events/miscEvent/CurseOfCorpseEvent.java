package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 诅咒之躯 - 效果处理
 *
 * 1. 攻击命中时：对攻击目标和自身随机施加中毒、缓慢、虚弱、饥饿、反胃之一，持续 30 秒
 * 2. 对负面 buff 越多的目标伤害越高：每层负面 buff +10% 伤害
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class CurseOfCorpseEvent {

    private static final int DEBUFF_DURATION_TICKS = 20 * 30; // 30 秒

    // 可施加的负面效果列表：中毒、缓慢、虚弱、饥饿、反胃
    private static final MobEffect[] DEBUFF_EFFECTS = {
            MobEffects.POISON,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.HUNGER,
            MobEffects.CONFUSION
    };

    /**
     * 判断玩家是否佩戴诅咒之躯
     */
    private static Optional<SlotResult> findCurseOfCorpse(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.CURSE_OF_CORPSE.get());
    }

    /**
     * 统计实体身上负面效果层数（使用指定效果列表）
     */
    private static int countNegativeEffects(LivingEntity entity) {
        int count = 0;
        for (MobEffect effect : DEBUFF_EFFECTS) {
            if (entity.hasEffect(effect)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 随机施加一个负面效果
     */
    private static void applyRandomDebuff(LivingEntity target) {
        MobEffect chosen = DEBUFF_EFFECTS[target.getRandom().nextInt(DEBUFF_EFFECTS.length)];
        target.addEffect(new MobEffectInstance(chosen, DEBUFF_DURATION_TICKS, 0));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        if (target == attacker) return;
        if (target.isDeadOrDying()) return;

        if (findCurseOfCorpse(attacker).isEmpty()) return;

        // 对目标随机施加一个负面效果
        applyRandomDebuff(target);

        // 对自身随机施加一个负面效果
        applyRandomDebuff(attacker);

        // 根据目标身上负面效果层数增加伤害：每层 +10%
        int debuffLayers = countNegativeEffects(target);
        if (debuffLayers > 0) {
            float multiplier = 1.0F + debuffLayers * 0.10F;
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage * multiplier);
        }
    }
}
