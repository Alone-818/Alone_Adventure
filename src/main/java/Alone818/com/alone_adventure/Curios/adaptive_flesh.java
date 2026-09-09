package Alone818.com.alone_adventure.Curios;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class adaptive_flesh extends Item implements ICurioItem {

    public adaptive_flesh() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.adaptive_flesh.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.adaptive_flesh.tooltip.mechanism_desc")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.adaptive_flesh.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}