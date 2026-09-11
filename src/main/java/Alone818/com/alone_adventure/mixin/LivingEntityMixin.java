package Alone818.com.alone_adventure.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import Alone818.com.alone_adventure.init.ModEffects;

/**
 * 耐力效果 — 跳跃高度加成
 *
 * 注入 {@link LivingEntity#getJumpBoostPower()} 返回处，
 * 与原版跳跃提升（JUMP_boost）一致的力度：每级 +0.1。
 *  sneak 时原版返回 0，耐力加成同样不生效。
 * 无 tick 开销，仅在起跳瞬间计算。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getJumpBoostPower", at = @At("RETURN"), cancellable = true)
    private void alone_adventure$enduranceJumpBoost(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectInstance effect = self.getEffect(ModEffects.ENDURANCE.get());
        if (effect != null) {
            int level = effect.getAmplifier() + 1;
            float current = cir.getReturnValue();
            // 与原版 JUMP_BOOST 相同：0.1 × 等级
            cir.setReturnValue(current + level * 0.1F);
        }
    }
}
