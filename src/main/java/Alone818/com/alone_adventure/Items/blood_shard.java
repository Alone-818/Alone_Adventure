package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 血脉碎片 - 击杀敌对生物概率掉落的合成材料
 *
 * 掉落规则（见 {@link Alone818.com.alone_adventure.events.BloodShardEvent}）：
 * - 被玩家击杀的敌对生物（Enemy）有 5% 概率掉落 1 个
 * - 击杀者佩戴猎人血清时提升为 10% 概率掉落 1 个
 * - 不受抢夺（Looting）加成影响
 */
public class blood_shard extends Item {

    public blood_shard() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.blood_shard.tooltip.desc")
                .withStyle(ChatFormatting.RED));
    }
}