package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.trait.ancient_power;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

/**
 * 远古力量 - 效果处理
 *
 * 效果：
 * - 佩戴时若身上有力量效果，额外获得基于力量等级 * 0.7 的同等时长的虚弱效果
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class AncientPowerEvent {

    /** 虚弱等级倍率：0.7 */
    private static final double WEAKNESS_LEVEL_MULTIPLIER = 0.7;

    /**
     * 判断玩家是否佩戴远古力量
     */
    public static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.ANCIENT_POWER.get())
                .isPresent();
    }

    /**
     * 每 Tick 检查：若佩戴远古力量且有力量效果，添加相应时长的虚弱
     * 虚弱等级 = 力量等级 × 0.7（向下取整）
     * 虚弱时长 = 力量效果剩余时长
     */
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;

        // 获取力量效果
        MobEffectInstance strengthEffect = player.getEffect(MobEffects.DAMAGE_BOOST);
        if (strengthEffect == null) return;

        // 计算虚弱等级：力量等级 × 0.7（向下取整）
        int strengthLevel = strengthEffect.getAmplifier() + 1;
        int weaknessLevel = (int) Math.floor(strengthLevel * WEAKNESS_LEVEL_MULTIPLIER);

        // 至少 1 级虚弱
        if (weaknessLevel < 1) weaknessLevel = 1;

        // 虚弱时长 = 力量效果剩余时长
        int weaknessDuration = strengthEffect.getDuration();

        // 检查是否需要更新虚弱效果
        MobEffectInstance weaknessEffect = player.getEffect(MobEffects.WEAKNESS);
        boolean needsUpdate = false;

        if (weaknessEffect == null) {
            needsUpdate = true;
        } else {
            int currentWeaknessLevel = weaknessEffect.getAmplifier() + 1;
            int currentDuration = weaknessEffect.getDuration();

            // 如果等级或时长差异较大，则更新
            if (currentWeaknessLevel != weaknessLevel || Math.abs(currentDuration - weaknessDuration) > 20) {
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
            // 移除旧的虚弱效果
            player.removeEffect(MobEffects.WEAKNESS);

            // 添加新的虚弱效果
            player.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    weaknessDuration,
                    weaknessLevel - 1, // amplifier = level - 1
                    false,
                    true
            ));
        }
    }
}