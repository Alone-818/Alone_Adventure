package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.necromancer_ledger;
import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * 亡灵秘典 - 结算事件
 *
 * 特性实现：
 * - 减少 20% 最大生命值上限
 * - 减少 95% 攻击伤害
 * - 每 60 秒获得 10 点黄心（不可堆叠）
 * - 攻击时对目标施加灾厄等级 1，时长 = 攻击伤害 × 20
 * - 多次攻击时间叠加，等级不变只取最高值
 * - 释放技能时，下次攻击叠加灾厄时长至 50 倍
 * - R 技能：周围生物的灾厄时长变为 1/5，等级 +3
 * - 灾厄等级 × 灾厄时长 >= 目标生命值时，致命一击（由 CalamityEffectEvent 处理）
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class NecromancerLedgerEvent {

    private static final int ABSORPTION_INTERVAL_TICKS = 1200;
    private static final double ABSORPTION_AMOUNT = 10.0;

    /** 检测玩家是否佩戴亡灵秘典 */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(new net.minecraft.resources.ResourceLocation(Alone_adventure.MODID, "necromancer_ledger")))
                .isPresent();
    }

    private static ItemStack getStack(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(new net.minecraft.resources.ResourceLocation(Alone_adventure.MODID, "necromancer_ledger")))
                .map(s -> s.stack())
                .orElse(ItemStack.EMPTY);
    }

    /** 减少 20% 最大生命值上限 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isWearing(player)) return;
        if (event.getAmount() <= 0) return;
        event.setAmount((float) (event.getAmount() * (1 - necromancer_ledger.MAX_HEALTH_REDUCTION)));
    }

    /**
     * 减少 95% 攻击伤害，并对目标施加灾厄效果。
     * 灾厄等级 = 1（不变），灾厄时长 = 攻击伤害 × 20。
     * 多次攻击时时长叠加，等级只取最高值。
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!isWearing(player)) return;
        if (event.getAmount() <= 0) return;

        // 减少 95% 攻击伤害（保存原始伤害用于计算普通攻击的灾厄时长）
        float originalDamage = event.getAmount();
        // 防止异常值导致的溢出或服务器卡顿
        if (Float.isNaN(originalDamage) || Float.isInfinite(originalDamage) || originalDamage <= 0) {
            return;
        }
        // 如果已被 CalamityEffectEvent 设置为致命伤害，跳过减伤覆盖
        if (originalDamage >= necromancer_ledger.LETHAL_DAMAGE) {
            return;
        }
        // 合理范围：0.5 到 100.0（对应 0.25 到 50 颗心）
        originalDamage = Math.max(0.5F, Math.min(originalDamage, 100.0F));
        event.setAmount((float) (originalDamage * (1 - necromancer_ledger.ATTACK_DAMAGE_REDUCTION)));

        ItemStack stack = getStack(player);
        if (stack.isEmpty()) return;

        LivingEntity target = event.getEntity();
        MobEffectInstance existing = target.getEffect(ModEffects.CALAMITY.get());

        // 检查技能叠加标记（激活技能后下次攻击时50倍叠加时长）
        CompoundTag tag = stack.getTag();
        boolean skillStacked = tag != null && tag.getBoolean(necromancer_ledger.NB_TAG_SKILL_STACKED);

        // 灾厄时长 = 伤害值（秒）× 0.8 倍率；技能后下次攻击 = 伤害 × 0.8 × 2 秒
        // 1 秒 = 20 tick，确保至少 20 tick（1 秒）防止瞬间消失
        double rawDurationSeconds = originalDamage * 0.8 * (skillStacked ? 2.0 : 1.0);
        int newDurationTicks = (int) Math.max(rawDurationSeconds * 20, 20);
        // 额外保护：防止异常大的时长导致服务器卡顿（上限 1000 tick ≈ 50 秒）
        if (newDurationTicks > 1000) {
            newDurationTicks = 1000;
        }

        if (newDurationTicks <= 0) return;

        if (skillStacked) {
            // 清除标记，仅下次攻击生效
            tag.remove(necromancer_ledger.NB_TAG_SKILL_STACKED);
            stack.setTag(tag);
        }

        if (existing != null) {
            int existingLevel = existing.getAmplifier() + 1;
            int existingDuration = existing.getDuration();
            int maxLevel = Math.max(existingLevel, necromancer_ledger.CALAMITY_BASE_LEVEL);
            int totalDuration = existingDuration + newDurationTicks;
            target.addEffect(new MobEffectInstance(ModEffects.CALAMITY.get(), totalDuration, maxLevel - 1, false, true, true));
        } else {
            target.addEffect(new MobEffectInstance(ModEffects.CALAMITY.get(), newDurationTicks, 0, false, true, true));
        }
    }

    /** 每 60 秒获得 10 点黄心 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;
        if (player.tickCount % ABSORPTION_INTERVAL_TICKS != 0) return;

        ItemStack stack = getStack(player);
        if (stack.isEmpty()) return;

        player.setAbsorptionAmount((float) (player.getAbsorptionAmount() + ABSORPTION_AMOUNT));
    }

    /**
     * R 技能：周围生物的灾厄时长变为 1/5，等级 +3。
     * 必须在 8 格范围内有生物带有灾厄效果才激活。
     */

}
