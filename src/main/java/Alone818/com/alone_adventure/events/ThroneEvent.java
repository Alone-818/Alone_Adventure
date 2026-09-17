package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.sealed_throne;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 封印王座 - 受击结算
 *
 * 在 {@link LivingDamageEvent} 末尾（LOWEST 优先级）处理，此时结晶心脏 / 绷带
 * 护盾若已抵消伤害会把事件置为 canceled，本处理器读到 canceled 即跳过——
 * 与"只有真正扣除红心才损失星辉"的语义一致。
 *
 * 结算顺序：
 * 1. 王座护盾 > 0：抵消本次伤害，扣 1 点护盾并给 2 秒抗性 IV（防多段伤害瞬间耗尽）。
 *    此时红心未减少，不损失星辉。
 * 2. 否则：本次伤害扣除红心的部分 = 总伤害 - 当前黄心吸收量，
 *    若 > 0 说明红心确实减少 → 损失 {@value sealed_throne#STAR_LOSS_PER_HIT} 点星辉。
 *    （黄心在原版 hurt 阶段先于本事件扣除红心，故能吸收掉整击时 amount≤absorption，不扣星。）
 *
 * 星辉变化后由 {@link sealed_throne#setStars} 立即刷新伤害/护甲被动属性。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ThroneEvent {

    /** 检测玩家是否佩戴封印王座，并返回其槽位结果 */
    private static Optional<SlotResult> findThrone(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.SEALED_THRONE.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isCanceled()) return; // 已被其他护盾抵消，红心未减少
        if (player.level().isClientSide()) return;
        // /kill、虚空等无视保护的伤害不构成"受击扣星"，也不消耗王座护盾
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        Optional<SlotResult> throneOpt = findThrone(player);
        if (throneOpt.isEmpty()) return;

        SlotResult slotResult = throneOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();

        float damage = event.getAmount();
        if (damage <= 0) return;

        double shield = sealed_throne.getShield(stack);
        int stars = sealed_throne.getStars(stack);

        // 1. 王座护盾优先：整击抵消，红心未减少 → 不损失星辉
        if (shield > 0) {
            tag.putDouble(sealed_throne.NB_TAG_SHIELD, shield - 1);
            writeBack(player, slotResult, stack);
            // 2 秒抗性 IV：与绷带同档，防止多段伤害瞬间耗尽护盾
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, true, true));
            event.setCanceled(true);
            return;
        }

        // 2. 无护盾：判断红心是否真的减少（黄心在原版已先扣一层）
        if (stars <= 0) return;
        float toHearts = damage - player.getAbsorptionAmount();
        if (toHearts <= 0) return; // 全部被黄心吸收，红心未减少

        sealed_throne.setStars(player, stack,
                Math.max(0, stars - sealed_throne.STAR_LOSS_PER_HIT));
        writeBack(player, slotResult, stack);
    }

    /** 把修改后的堆栈写回真实槽位，触发 Curios 的 NBT 客户端同步 */
    private static void writeBack(Player player, SlotResult slotResult, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));
    }
}
