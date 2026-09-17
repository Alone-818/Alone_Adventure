package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.starlight_greatsword;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 星辉大剑结算
 *
 * 聚能（服务端玩家 tick，60 tick 一次）：主手或副手持有大剑时储能 +1（上限 20）。
 * 与 CHARGE_INTERVAL_TICKS 同为 60 的节拍，玩家背包堆栈 NBT 由原版容器自动同步客户端。
 *
 * 命中（LivingDamageEvent）：储能 > 0 时
 * 1. 伤害 = 基础伤害 + 储能 × 10%
 * 2. 给予与储能同层数的伤害吸收（Absorption，每层 2 点黄心，20 秒），最高 20 级
 * 3. 储能清零（未命中的挥击不消耗储能）
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class StarlightGreatswordEvent {

    /** 聚能：每 60 tick 为手持的大剑 +1 层储能 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % starlight_greatsword.CHARGE_INTERVAL_TICKS != 0) return;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STARLIGHT_GREATSWORD.get())) continue;
            int charge = starlight_greatsword.getCharge(stack);
            if (charge < starlight_greatsword.MAX_CHARGE) {
                starlight_greatsword.setCharge(stack, charge + 1);
            }
            return; // 只结算一只手（主手优先），且储能已满时也停止
        }
    }

    /** 命中：按储能追加伤害并转化为同层数的伤害吸收，随后清零储能 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ItemStack weapon = player.getItemInHand(player.getUsedItemHand());
        if (!weapon.is(ModItems.STARLIGHT_GREATSWORD.get())) return;

        int charge = starlight_greatsword.getCharge(weapon);
        if (charge <= 0) return;

        // 1. 伤害加成：基础伤害 × (1 + 储能 × 10%)
        event.setAmount(starlight_greatsword.ATTACK_DAMAGE * (1.0F + charge * starlight_greatsword.DAMAGE_PER_CHARGE));

        // 2. 吸收转化：黄心 = 伤害 / 5（每 5 点伤害 → 1 层吸收，对应 2 点黄心）
        int absorptionLevels = (int) Math.ceil(event.getAmount() / 5.0F);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                starlight_greatsword.ABSORPTION_DURATION_TICKS,
                absorptionLevels - 1, false, true, true));

        // 3. 储能清零，开始新一轮聚能
        starlight_greatsword.setCharge(weapon, 0);
    }
}
