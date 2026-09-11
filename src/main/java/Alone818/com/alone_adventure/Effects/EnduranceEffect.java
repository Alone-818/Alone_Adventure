package Alone818.com.alone_adventure.Effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * 耐力效果
 *
 * 等级 = amplifier + 1，效果期间：
 * - 移动速度 + 等级 × 0.1（MOVEMENT_SPEED 属性加法修正）
 * - 跳跃高度 + 等级 × 0.50 格（由 {@link Alone818.com.alone_adventure.mixin.LivingEntityMixin} 注入 getJumpBoostPower 实现）
 * - 台阶高度 + 等级 × 1 格（ForgeMod.STEP_HEIGHT_ADDITION 属性加法修正）
 * - 饱食度不随活动消耗（由 {@link Alone818.com.alone_adventure.mixin.FoodDataMixin} 注入 addExhaustion 实现，
 *   不影响 FoodData.tick 中的生命值回复逻辑）
 *
 * 通过 {@link MobEffect#addAttributeModifier} 在构造函数中注册属性，
 * 原版会自动按等级放大数值（ADDITION 操作 × (amplifier+1)），
 * 并让药水/效果悬浮提示正确显示增加的属性。
 */
public class EnduranceEffect extends MobEffect {

    public static final double SPEED_PER_LEVEL = 0.1;
    public static final double STEP_HEIGHT_PER_LEVEL = 1.0;

    // 固定 UUID：同一效果重复添加时覆盖而非叠加
    private static final UUID SPEED_UUID = UUID.fromString("5c5b7e81-00e3-4d31-8e14-4c2d9bb0b001");
    private static final UUID STEP_UUID = UUID.fromString("5c5b7e81-00e3-4d31-8e14-4c2d9bb0b003");

    public EnduranceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2E8B57); // 耐力绿
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_UUID.toString(),
                SPEED_PER_LEVEL, AttributeModifier.Operation.ADDITION);
        this.addAttributeModifier(ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_UUID.toString(),
                STEP_HEIGHT_PER_LEVEL, AttributeModifier.Operation.ADDITION);
    }

    // 不需要 per-tick 处理
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
