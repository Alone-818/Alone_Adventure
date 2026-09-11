package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.events.ImmunityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 龙胤之力 - 源自《只狼》中的「龙胤」：樱龙的恩赐，如露水般流淌于体内的不死之力。
 *
 * 贴合原作的规则：
 * - 受到致命伤害时，龙胤令持有者免于死亡，原地复活——但只恢复 50% 生命
 *   （原作复活并非满血，只是让倒下的人重新站起）
 * - 龙胤之露需时间重新凝聚：7 分钟冷却
 *
 * 伤害免疫、冷却计时与写回槽位由 {@link ImmunityEvent} 统一处理，
 * 本类只提供冷却常量与触发成功后的效果（复活与三连效果）。
 */
public class dragon_power extends Item implements ICurioItem {

    // 冷却时长：7 分钟 = 420 秒 = 8400 tick
    public static final int COOLDOWN_TICKS = 8400;

    // 效果持续时间（tick）：隐身 7s、无敌 5s、黑暗 3s
    public static final int INVISIBILITY_TICKS = 140;
    public static final int INVULNERABLE_TICKS = 100;
    public static final int DARKNESS_TICKS = 60;

    // 复活恢复的生命比例（半血站起，而非满血）
    public static final float REVIVE_HEALTH_RATIO = 0.5F;

    public dragon_power() {
        super(new Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    }

    /**
     * 免疫致命伤害后的效果：原地复活，但只恢复 50% 生命。
     * 贴合原作设定：龙胤让不死者倒下后重新站起，
     * 而非像不死图腾那样直接恢复到满血。
     */
    public static void reviveInPlace(ServerPlayer player) {
        player.setHealth(player.getMaxHealth() * REVIVE_HEALTH_RATIO);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0, false, true));
        // 抗性 V（amplifier 4）及以上可免疫常规伤害，作为 5 秒无敌
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, INVULNERABLE_TICKS, 4, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.dragon_power.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.alone_adventure.dragon_power.tooltip.immunity")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.dragon_power.tooltip.effects")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.alone_adventure.dragon_power.tooltip.cooldown")
                    .withStyle(ChatFormatting.AQUA));

            // 冷却中时实时显示剩余时间（NBT 由 Curios 同步到客户端）
            if (level != null) {
                long remaining = ImmunityEvent.getRemainingCooldown(stack);
                if (remaining > 0) {
                    tooltip.add(Component.translatable("item.alone_adventure.dragon_power.tooltip.cooldown_left",
                                    Math.round(remaining / 20.0F))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.dragon_power.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
