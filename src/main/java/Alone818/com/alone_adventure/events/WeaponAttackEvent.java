package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.powersword;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 武器攻击事件处理器
 * 负责动力剑的穿甲伤害、火焰伤害和吸血效果
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class WeaponAttackEvent {

    /**
     * 处理动力剑的穿甲伤害、火焰伤害和吸血效果
     * 在造成伤害前结算穿甲额外伤害
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getItemInHand(attacker.getUsedItemHand());

            // 检查是否为动力剑
            if (weapon.is(ModItems.POWERSWORD.get())) {
                LivingEntity target = event.getEntity();

                // 1. 超频伤害加成：基础伤害 +50%（穿甲加成结算在倍率之后，不受放大）
                boolean overclocked = powersword.isOverclocked(weapon, attacker.level().getGameTime());
                if (overclocked) {
                    event.setAmount(event.getAmount() * powersword.OVERCLOCK_DAMAGE_MULTIPLIER);
                }

                // 2. 穿甲伤害：额外计算穿透伤害
                float penetrationDamage = powersword.calculatePenetrationDamage(target);
                if (penetrationDamage > 0) {
                    event.setAmount(event.getAmount() + penetrationDamage);
                }

                // 3. 超频效果：火焰伤害+点燃+吸血
                if (overclocked && target != attacker) {
                    // 额外造成3点火焰伤害
                    target.hurt(target.damageSources().onFire(), powersword.FIRE_DAMAGE);
                    // 点燃4秒
                    target.setSecondsOnFire(powersword.FIRE_BURN_DURATION / 20);
                }
                if (overclocked) {
                    // 吸血：造成伤害的60%转化为治疗
                    float healAmount = event.getAmount() * powersword.OVERCLOCK_HEAL_RATIO;
                    attacker.heal(healAmount);
                }
            }
        }
    }
}