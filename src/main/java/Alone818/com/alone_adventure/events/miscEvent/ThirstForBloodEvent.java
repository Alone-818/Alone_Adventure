package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.miscCurios.thirst_for_blood;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Objects;

/**
 * 渴血症 - 效果处理
 *
 * 1. 攻击回复攻击伤害的 50%
 * 2. 提高 30% 最大生命值
 * 3. 当血量高于 80% 时获得虚弱 III
 * 4. 当血量低于 30% 时获得急迫 II
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ThirstForBloodEvent {

    private static final int WEAKNESS_DURATION = 200; // 10 秒
    private static final int HASTE_DURATION = 200;    // 10 秒

    /**
     * 判断玩家是否佩戴渴血症
     */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.THIRST_FOR_BLOOD.get())
                .isPresent();
    }

    /**
     * 攻击回复伤害的 50%
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!isWearing(attacker)) return;

        float damage = event.getAmount();
        if (damage > 0) {
            attacker.heal(damage * 0.5F);
        }
    }

    /**
     * 血量大于 80% → 虚弱 III；血量小于 30% → 急迫 II
     * 5 秒检测一次
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;
        if (player.tickCount % 100 != 0) return; // 5 秒检测一次

        float current = player.getHealth();
        float max = player.getMaxHealth();

        // 提高 30% 最大生命值（属性修饰符在 attributeModifiers 中处理）
        // 这里仅处理虚弱/急迫效果

        if (current / max > 0.8F) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION, 2, false, false));
        } else if (current / max < 0.3F) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, HASTE_DURATION, 1, false, false));
        }
    }
}
