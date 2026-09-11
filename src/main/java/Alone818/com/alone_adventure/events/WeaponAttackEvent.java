package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Effects.LacerationEffect;
import Alone818.com.alone_adventure.Items.powersword;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 武器攻击事件处理器
 * 负责动力剑的穿甲伤害、火焰伤害和吸血效果；
 * 负责撕裂的延迟减半回贴。
 *
 * 链锯剑的高频扫射已改为「长按右键持续使用」模型，
 * 结算逻辑在 {@link Alone818.com.alone_adventure.Items.chainsawsword#onUseTick} 中完成，
 * 不再需要事件驱动。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class WeaponAttackEvent {

    /**
     * 撕裂延迟结算：每 tick 检查所有实体，
     * 把到期减半后的撕裂等级重新挂上，直到归零。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;

        LacerationEffect.tryReapply(entity);
    }

    /**
     * 处理动力剑的穿甲伤害、火焰伤害和吸血效果
     * 在造成伤害前结算穿甲额外伤害
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getItemInHand(attacker.getUsedItemHand());

            if (weapon.is(ModItems.POWERSWORD.get())) {
                LivingEntity target = event.getEntity();

                // 1. 超频伤害加成：基础伤害 +50%
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
                    target.hurt(target.damageSources().onFire(), powersword.FIRE_DAMAGE);
                    target.setSecondsOnFire(powersword.FIRE_BURN_DURATION / 20);
                }
                if (overclocked) {
                    float healAmount = event.getAmount() * powersword.OVERCLOCK_HEAL_RATIO;
                    attacker.heal(healAmount);
                }
            }
        }
    }
}
