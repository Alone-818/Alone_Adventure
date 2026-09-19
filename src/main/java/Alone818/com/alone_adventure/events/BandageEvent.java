package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.binding_bandage;
import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 紧缚绷带 - 效果处理
 *
 * 1. 佩戴者每次攻击命中时，50% 概率为自己施加耐力 I
 *    （移动速度、跳跃高度提升，饱食度不消耗）。不可叠加：
 *    已有耐力时既不重复施加也不刷新时长。
 * 2. 护盾结算（主动技能获得的点数盾）：
 *    玩家受伤且绷带护盾 > 0 时，抵消本次伤害并扣除 1 点护盾，
 *    给予 2 秒抗性 IV 防止同一次攻击的多段伤害瞬间耗尽护盾
 *    （与 {@link ShieldEvent} 的结晶心脏护盾机制一致）。
 *    当前护盾点数显示在饰品悬停提示中。
 * 3. 攻击虚弱目标：额外造成 虚弱等级×2 点伤害（等级 = amplifier+1；
 *    与墨制刀刃的可叠加虚弱联动，虚弱 V 时单次 +10 点）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class BandageEvent {

    /** 判断玩家是否佩戴紧缚绷带，并返回其槽位结果 */
    private static Optional<SlotResult> findBandage(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BINDING_BANDAGE.get());
    }

    /**
     * 攻击命中：50% 概率对自己施加耐力 I（3 秒），不可叠加。
     * 用 LivingHurtEvent（伤害结算前）与痛击之锤/破损面具的施加时机一致。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        LivingEntity target = event.getEntity();
        if (target == attacker) return;
        if (!findBandage(attacker).isPresent()) return;
        if (attacker.getRandom().nextFloat() >= binding_bandage.ENDURANCE_CHANCE) return;

        // 不可叠加：已有耐力效果时直接跳过（也不刷新时长）
        if (attacker.hasEffect(ModEffects.ENDURANCE.get())) return;

        attacker.addEffect(new MobEffectInstance(ModEffects.ENDURANCE.get(),
                binding_bandage.ENDURANCE_DURATION_TICKS, 0, false, true));
    }

    /**
     * 攻击虚弱目标：额外造成 虚弱等级×2 点伤害。
     * 在 LivingDamageEvent（护甲结算后）追加，额外伤害不被护甲削减。
     */
    @SubscribeEvent
    public static void onAttackDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (event.isCanceled()) return;

        LivingEntity target = event.getEntity();
        if (target == attacker) return;
        if (findBandage(attacker).isEmpty()) return;

        MobEffectInstance weakness = target.getEffect(MobEffects.WEAKNESS);
        if (weakness == null) return;

        // 虚弱等级 = amplifier + 1，每级 +2 点
        event.setAmount(event.getAmount() + (weakness.getAmplifier() + 1) * binding_bandage.WEAKNESS_BONUS_PER_LEVEL);
    }

    /**
     * 绷带护盾结算：优先由结晶心脏等普通护盾处理（本事件在其之后按注册顺序同优先级运行，
     * 若伤害已被取消则直接跳过），抵消伤害并扣 1 点护盾。
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isCanceled()) return;

        // /kill、虚空等无视无敌保护的伤害不能被护盾免疫，必须放行
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        Optional<SlotResult> bandageOpt = findBandage(player);
        if (bandageOpt.isEmpty()) return;

        SlotResult slotResult = bandageOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();

        double currentShield = tag.getDouble(binding_bandage.NB_TAG_SHIELD);
        if (currentShield <= 0) return;

        float damage = event.getAmount();
        if (damage <= 0) return;

        // 扣除 1 点护盾
        tag.putDouble(binding_bandage.NB_TAG_SHIELD, currentShield - 1);
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        // 2 秒抗性 IV（amplifier = 3）：防同一次攻击多段伤害瞬间耗尽护盾
        // （与抗性 V 的 ShieldEvent 不同档位，避免完全吞掉后续免疫链）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, true, true));

        // 免疫此次伤害
        event.setCanceled(true);
    }
}
