package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 痛击之锤 - 攻击速度 0.5、伤害 10 的小范围重型武器
 *
 * 范围打击：直接命中时以命中点为中心造成范围伤害——周围 {@value #AREA_RADIUS} 格内的
 * 敌人受到攻击者攻击伤害的 {@value #AREA_DAMAGE_RATIO} 倍，并同样叠加易伤
 * （不伤及与攻击者友方的生物，含驯兽，与链锯剑扫射口径一致）。
 *
 * 易伤：每次命中（含范围命中）叠加 2 层易伤：
 * 等级 = 目标原有易伤等级 + 2（没有易伤时视为 0 级，即施加 2 级）
 *
 * 易伤的增伤与清除逻辑由 {@link Alone818.com.alone_adventure.events.VulnerabilityEvent} 处理，
 * 等级的叠加在此处的命中回调中完成。
 */
public class painstrike_hammer extends SwordItem {

    // 伤害 10：WOOD 层级本身加成 0，参数直接给 10
    // 攻击速度 0.5：属性基值 4.0 + 修饰符 = 0.5，故修饰符为 -3.5
    public static final int ATTACK_DAMAGE = 10;
    public static final float ATTACK_SPEED = 0.5F;

    // 范围打击参数（默认值，可由 Config 在 alone_adventure-common.toml 覆盖；基础攻击伤害/攻速在注册期固定，不参与配置）
    // 以命中目标为中心的半径（格）
    public static double AREA_RADIUS = 2.0D;
    // 范围打击伤害：攻击者攻击伤害的比例
    public static double AREA_DAMAGE_RATIO = 0.6D;

    // 易伤持续时间：10 秒
    public static int VULNERABILITY_DURATION_TICKS = 200;

    // 每次命中提升的易伤等级
    public static int VULNERABILITY_LEVEL_STEP = 2;

    public painstrike_hammer() {
        super(Tiers.WOOD, ATTACK_DAMAGE, ATTACK_SPEED - 4.0F,
                new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    /**
     * 武器命中实体：
     * 1. 范围打击——命中点周围 {@value #AREA_RADIUS} 格内的非友方生物受到
     *    攻击伤害 × {@value #AREA_DAMAGE_RATIO} 的伤害并叠加易伤；
     * 2. 直接命中目标叠加易伤：新等级 = 原有等级 + 2，并刷新持续时间。
     * 不对驯兽施加易伤（与原版武器行为一致）。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            strikeArea(target, attacker);
        }

        if (!(target instanceof TamableAnimal)) {
            applyVulnerability(target);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * 范围打击：对命中目标周围 {@value #AREA_RADIUS} 格内的非友方生物造成
     * 攻击者攻击伤害 × {@value #AREA_DAMAGE_RATIO} 的伤害并叠加易伤。
     * 伤害随攻击者的攻击伤害属性走（含力量等加成）。
     * 命中前把 {@code invulnerableTime} 归零以稳定命中（与链锯剑扫射一致）。
     */
    private static void strikeArea(LivingEntity target, LivingEntity attacker) {
        float areaDamage = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * AREA_DAMAGE_RATIO);
        if (areaDamage <= 0) return;

        List<LivingEntity> nearby = attacker.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(AREA_RADIUS),
                entity -> entity != target && entity != attacker
                        && entity.isAlive() && !entity.isAlliedTo(attacker));

        for (LivingEntity victim : nearby) {
            // 归零无敌帧，使范围伤害稳定生效
            victim.invulnerableTime = 0;
            if (attacker instanceof Player player) {
                victim.hurt(attacker.damageSources().playerAttack(player), areaDamage);
            } else {
                victim.hurt(attacker.damageSources().mobAttack(attacker), areaDamage);
            }
            applyVulnerability(victim);
        }
    }

    /**
     * 叠加易伤：新等级 = 原有等级 + {@value #VULNERABILITY_LEVEL_STEP}，并刷新持续时间。
     * amplifier = 等级 - 1；无效果时原有等级按 0 计，即新 amplifier = -1 + 2 = 1（2 级）
     */
    public static void applyVulnerability(LivingEntity target) {
        MobEffectInstance existing = target.getEffect(ModEffects.VULNERABILITY.get());
        int newAmplifier = (existing == null ? -1 : existing.getAmplifier()) + VULNERABILITY_LEVEL_STEP;
        target.addEffect(new MobEffectInstance(
                ModEffects.VULNERABILITY.get(),
                VULNERABILITY_DURATION_TICKS,
                newAmplifier));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.painstrike_hammer.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.painstrike_hammer.tooltip.area_desc")
                    .withStyle(ChatFormatting.DARK_AQUA));
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
