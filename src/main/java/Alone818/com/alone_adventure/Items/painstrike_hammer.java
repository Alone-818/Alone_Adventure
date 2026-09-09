package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 痛击之锤 - 攻击速度 0.5、伤害 15 的重型武器
 * 命中目标后施加易伤：等级 = 目标原有易伤等级 + 2（没有易伤时视为 0 级，即施加 2 级）
 *
 * 易伤的增伤与清除逻辑由 {@link Alone818.com.alone_adventure.events.VulnerabilityEvent} 处理，
 * 等级的叠加在此处的命中回调中完成。
 */
public class painstrike_hammer extends SwordItem {

    // 伤害 15：WOOD 层级本身加成 0，参数直接给 15
    // 攻击速度 0.5：属性基值 4.0 + 修饰符 = 0.5，故修饰符为 -3.5
    public static final int ATTACK_DAMAGE = 15;
    public static final float ATTACK_SPEED = 0.5F;

    // 易伤持续时间：10 秒
    public static final int VULNERABILITY_DURATION_TICKS = 200;

    // 每次命中提升的易伤等级
    public static final int VULNERABILITY_LEVEL_STEP = 2;

    public painstrike_hammer() {
        super(Tiers.WOOD, ATTACK_DAMAGE, ATTACK_SPEED - 4.0F,
                new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    /**
     * 武器命中实体时叠加易伤：新等级 = 原有等级 + 2，并刷新持续时间。
     * 不伤害驯兽（与原版武器行为一致）。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target instanceof TamableAnimal)) {
            MobEffectInstance existing = target.getEffect(ModEffects.VULNERABILITY.get());
            // amplifier = 等级 - 1；无效果时原有等级按 0 计，即新 amplifier = -1 + 2 = 1（2 级）
            int newAmplifier = (existing == null ? -1 : existing.getAmplifier()) + VULNERABILITY_LEVEL_STEP;
            target.addEffect(new MobEffectInstance(
                    ModEffects.VULNERABILITY.get(),
                    VULNERABILITY_DURATION_TICKS,
                    newAmplifier));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.painstrike_hammer.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.painstrike_hammer.tooltip.charge_desc")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.painstrike_hammer.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}