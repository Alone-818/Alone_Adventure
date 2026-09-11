package Alone818.com.alone_adventure.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import Alone818.com.alone_adventure.init.ModEffects;

import javax.annotation.Nullable;

/**
 * 耐力效果 — 饱食度冻结
 *
 * 在 {@link net.minecraft.world.food.FoodData#addExhaustion(float)} 头部注入，
 * 效果存在时阻止饥饿值累加。
 * <p>
 * 这不会取消 {@link net.minecraft.world.food.FoodData#tick(Player)} 的生命值回复逻辑，
 * 回血依旧正常运作。
 */
@Mixin(net.minecraft.world.food.FoodData.class)
public abstract class FoodDataMixin {

    @Unique
    @Nullable
    private Player alone_adventure$currentPlayer;

    // 记录 tick 时段的玩家引用，供 addExhaustion 判断使用
    @Inject(method = "tick", at = @At("HEAD"))
    private void alone_adventure$capturePlayer(Player player, CallbackInfo ci) {
        this.alone_adventure$currentPlayer = player;
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void alone_adventure$freezeExhaustion(float amount, CallbackInfo ci) {
        Player p = this.alone_adventure$currentPlayer;
        if (p != null && p.hasEffect(ModEffects.ENDURANCE.get())) {
            ci.cancel();
        }
    }
}
