package Alone818.com.alone_adventure.Curios.trait;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * 一无所有 - 特质槽位饰品
 *
 * 效果：
 * - 10s 内未受到伤害则快速回复生命值
 * - 常态获得抗性 2
 * - 减少 20% 护甲值（Curios 属性修饰符）
 * - 减少 20% 护甲韧性（Curios 属性修饰符）
 */
public class nothing_left extends Item implements ICurioItem {

    /** 未受伤害时间阈值（tick）：10 秒 = 200 tick */
    public static final int DAMAGE_FREE_THRESHOLD_TICKS = 200;
    /** 回复间隔（tick）：每秒回复 1 点 = 20 tick */
    public static final int REGEN_INTERVAL_TICKS = 20;
    /** 抗性提升等级：抗性 II = amplifier 1 */
    public static final int RESISTANCE_AMPLIFIER = 1;
    /** 护甲减免百分比 */
    public static final double ARMOR_REDUCTION_PERCENT = -0.20;
    /** 护甲韧性减免百分比 */
    public static final double TOUGHNESS_REDUCTION_PERCENT = -0.20;

    // 固定 UUID：用于属性修饰符
    public static final UUID ARMOR_UUID = UUID.fromString("3a4b5c6d-7e8f-4a9b-8c1d-2e3f4a5b6c03");
    public static final UUID TOUGHNESS_UUID = UUID.fromString("3a4b5c6d-7e8f-4a9b-8c1d-2e3f4a5b6c04");

    public nothing_left() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    /**
     * 通过 Curios 属性修饰符减少护甲和护甲韧性
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 减少 20% 护甲值
        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                ARMOR_UUID,
                "Nothing Left Armor Reduction",
                ARMOR_REDUCTION_PERCENT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        // 减少 20% 护甲韧性
        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                TOUGHNESS_UUID,
                "Nothing Left Toughness Reduction",
                TOUGHNESS_REDUCTION_PERCENT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.nothing_left.tooltip.desc")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.alone_adventure.nothing_left.tooltip.regen_desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.alone_adventure.nothing_left.tooltip.resistance_desc")
                .withStyle(ChatFormatting.BLUE));
    }
}
