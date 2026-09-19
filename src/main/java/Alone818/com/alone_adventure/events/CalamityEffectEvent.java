package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.necromancer_ledger;
import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 灾厄（Calamity）效果 - 独立结算事件
 *
 * 特性：
 * - 灾厄等级 × 灾厄时长 >= 目标生命值时，下次攻击造成 32676 直接伤害
 * - 致命一击后灾厄效果保留，可继续叠加时长与等级
 * - 该效果由亡灵秘典施加，但结算逻辑独立维护
 *
 * 注意：此事件只处理"带有灾厄效果的实体被攻击时的额外伤害判定"，
 *       不负责施加效果（施加在 NecromancerLedgerEvent 中完成）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class CalamityEffectEvent {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // 检查目标是否带有灾厄效果
        MobEffectInstance effect = target.getEffect(ModEffects.CALAMITY.get());
        if (effect == null) return;

        // 灾厄等级 = amplifier + 1
        int calamityLevel = effect.getAmplifier() + 1;
        // 灾厄时长（秒）= 效果剩余持续时间（tick）/ 20
        int durationSeconds = effect.getDuration() / 20;

        if (durationSeconds <= 0 || calamityLevel <= 0) return;

        // 灾厄等级 × 灾厄时长（秒）>= 目标生命值时，触发致命一击
        double targetMaxHealth = target.getMaxHealth();
        double calamityPower = (double) calamityLevel * durationSeconds;

        if (calamityPower >= targetMaxHealth) {
            event.setAmount(necromancer_ledger.LETHAL_DAMAGE);
            // 致命一击触发后保留灾厄效果，可继续叠加
        }
    }
}
