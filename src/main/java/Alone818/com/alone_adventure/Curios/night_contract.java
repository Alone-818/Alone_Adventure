package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.events.NightContractEvent;
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

public class night_contract extends Item implements ICurioItem {

    public night_contract() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.sun_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.night_desc")
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.sleep_desc")
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.moon_desc")
                    .withStyle(ChatFormatting.GRAY));

            // 实时显示当前月相与实际加成数值（tooltip 每次悬停都会重新计算）
            if (level != null) {
                int phase = level.getMoonPhase();
                double bonus = NightContractEvent.getMoonBonus(phase);
                tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.current_phase",
                                Component.translatable("item.alone_adventure.night_contract.tooltip.phase." + phase),
                                Math.round(bonus * 100) + "%")
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.night_contract.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

}