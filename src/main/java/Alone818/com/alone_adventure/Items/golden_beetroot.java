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
 * 金甜菜根 - 食物
 *
 * 合成配方：8 个金粒围绕 1 个甜菜根 → 1 个金甜菜根
 * 食用给予耐力 I 持续 60 秒（1200 tick），营养值 6，饱和度 1.2
 * 也是酿造耐力药水的原料（原版酿造台配方：耐力药水 = 金甜菜根 + 水瓶）
 */
public class golden_beetroot extends Item {

    public golden_beetroot(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.alone_adventure.golden_beetroot")
                .withStyle(ChatFormatting.GRAY));
    }
}
