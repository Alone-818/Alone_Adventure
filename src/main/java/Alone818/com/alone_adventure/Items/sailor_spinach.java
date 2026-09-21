package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 水手菠菜 - 食物
 *
 * 食用回复 4 点饱食度（饥饿值）与 8 点饱和度（营养 4 × 饱和系数 1.0 × 2），
 * 并给予力量 II（{@value #STRENGTH_DURATION_TICKS} tick，默认 30 秒）与
 * 生命恢复 II（{@value #REGEN_DURATION_TICKS} tick，默认 10 秒）。
 * 满腹也可食用（alwaysEat），随时作为战斗补给。
 * 效果时长经由 {@link net.minecraft.world.food.FoodProperties} 的惰性 Supplier
 * 在食用时才构造，因此 Config 回填的静态时长对已有物品同样生效。
 */
public class sailor_spinach extends Item {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 力量 II 持续：30 秒（600 tick） */
    public static int STRENGTH_DURATION_TICKS = 600;
    /** 生命恢复 II 持续：10 秒（200 tick） */
    public static int REGEN_DURATION_TICKS = 200;

    public sailor_spinach(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.alone_adventure.sailor_spinach")
                .withStyle(ChatFormatting.GRAY));
    }
}
