package Alone818.com.alone_adventure.Curios.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 克罗诺斯试剂 - 特质槽位饰品
 *
 * 描述："很好，这样能行"
 *
 * 效果：
 * - 连续攻击之间不受到攻击将获得可堆叠的力量，上限为 9
 * - 受击后清除力量，获得虚弱等级 3 时间 7 秒
 */
public class chronos_reagent extends Item {

    /** 力量上限等级 */
    public static final int MAX_STRENGTH_LEVEL = 9;
    /** 虚弱时长（tick）：7 秒 = 140 tick */
    public static final int WEAKNESS_DURATION_TICKS = 140;
    /** 虚弱等级 */
    public static final int WEAKNESS_AMPLIFIER = 2; // Level 3 = amplifier 2

    public chronos_reagent() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.chronos_reagent.tooltip.desc")
                .withStyle(ChatFormatting.DARK_BLUE));
        tooltip.add(Component.translatable("item.alone_adventure.chronos_reagent.tooltip.stack_desc")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.alone_adventure.chronos_reagent.tooltip.clear_desc")
                .withStyle(ChatFormatting.RED));
    }
}
