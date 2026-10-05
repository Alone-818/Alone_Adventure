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
 * 渴血症 - 特质槽位饰品
 *
 * 被动效果：
 * 1. 攻击回复攻击伤害的 50%
 * 2. 提高 30% 最大生命值
 * 3. 当血量高于 80% 时获得虚弱 III
 * 4. 当血量低于 30% 时获得急迫 II
 */
public class thirst_for_blood extends Item implements ICurioItem {

    public thirst_for_blood() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.thirst_for_blood.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.alone_adventure.thirst_for_blood.tooltip.heal_desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.thirst_for_blood.tooltip.health_desc")
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("item.alone_adventure.thirst_for_blood.tooltip.effect_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.thirst_for_blood.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
