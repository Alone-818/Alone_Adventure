package Alone818.com.alone_adventure.Items;


import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 招架之盾 - 在攻击命中前的瞬间举起可格挡攻击
 * 完全免疫伤害，反弹 250% 伤害给攻击者并使其虚弱
 */
public class parryshield extends ShieldItem {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    // 招架窗口：举盾后 1 秒内可完成招架
    public static int PARRY_WINDOW_TICKS = 20;

    // 反弹伤害比例：250%
    public static float PARRY_REFLECT_RATIO = 2.5F;

    // 虚弱效果持续时间：3 秒
    public static int WEAKNESS_DURATION_TICKS = 60;

    // 虚弱效果等级：II (放大器 1)
    public static int WEAKNESS_AMPLIFIER = 1;

    // 长时间举盾吸收比例：80%
    public static float LONG_BLOCK_ABSORB = 0.8F;

    // 冷却时间：1 秒
    public static int PARRY_COOLDOWN_TICKS = 20;

    public parryshield() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.parryshield.tooltip.desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.parryshield.tooltip.parry_desc")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("item.alone_adventure.parryshield.tooltip.block_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.parryshield.tooltip.desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }
}