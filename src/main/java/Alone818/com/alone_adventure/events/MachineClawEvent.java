package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.machine_claw;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageTypes;  // ← 修正此处
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 机器爪刃 - 连击结算
 *
 * 主手持爪刃的近战命中（LivingDamageEvent，护甲结算后追加）：
 * 1. 伤害额外 + 连击层数 × 每层加成（第 N 次连续命中 = 基础 + (N-1)×加成，
 *    首次命中无加成；连击超时后从 0 重新计）
 * 2. 连击 +1 并刷新计时；2 秒内未命中（含挥空）时加成视为已清零
 *
 * 连击层数与上次命中时刻存于物品 NBT（每把爪刃独立记录，
 * 随背包容器同步客户端 tooltip）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class MachineClawEvent {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        // 只结算近战挥击：投掷物等其他伤害来源不叠连击
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;

        LivingEntity target = event.getEntity();
        if (target == player) return;

        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(ModItems.MACHINE_CLAW.get())) return;

        long now = player.level().getGameTime();
        int combo = machine_claw.getEffectiveCombo(weapon, now);

        // 1. 连击伤害加成（LivingDamageEvent 追加，不被护甲削减）
        if (combo > 0) {
            event.setAmount(event.getAmount() + combo * machine_claw.COMBO_DAMAGE_PER_HIT);
        }

        // 2. 连击 +1 并刷新计时（写回 NBT，随背包容器同步客户端）
        CompoundTag tag = weapon.getOrCreateTag();
        tag.putInt(machine_claw.NB_TAG_COMBO, combo + 1);
        tag.putLong(machine_claw.NB_TAG_LAST_HIT, now);
        weapon.setTag(tag);
    }
}