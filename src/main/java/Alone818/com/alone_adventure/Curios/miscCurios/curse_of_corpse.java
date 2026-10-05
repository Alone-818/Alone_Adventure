package Alone818.com.alone_adventure.Curios.miscCurios;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 诅咒之躯 - 特质槽位饰品
 *
 * 被动效果：
 * 1. 攻击时对攻击目标和自身随机施加中毒、缓慢、虚弱、饥饿、反胃 30 秒
 * 2. 对负面 buff 越多的目标攻击伤害越高，每层 10%
 */
public class curse_of_corpse extends Item implements ICurioItem {

    public curse_of_corpse() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.curse_of_corpse.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.alone_adventure.curse_of_corpse.tooltip.debuff_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.curse_of_corpse.tooltip.dmg_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.curse_of_corpse.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
