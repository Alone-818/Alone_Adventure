package Alone818.com.alone_adventure.Effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 灾厄 - 亡灵秘典专属负面药水效果
 *
 * 特性：
 * - 负面效果（HARMFUL）
 * - 灾厄等级 = 攻击伤害 × 20（技能后 × 50）
 * - 灾厄等级 × 药水时长 ≥ 目标生命值时，下次攻击造成 32676 直接伤害
 */
public class CalamityEffect extends MobEffect {

    public CalamityEffect() {
        // HARMFUL：负面效果；0x3A0E0E 深暗红色，体现灾厄的恐怖感
        super(MobEffectCategory.HARMFUL, 0x3A0E0E);
    }
}
