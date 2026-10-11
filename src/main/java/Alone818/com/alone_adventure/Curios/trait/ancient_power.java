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
 * 远古力量 - 特质槽位饰品
 *
 * 效果：
 * - 不直接增加攻击伤害
 * - 每等级增加 15% 攻击伤害（基于基础攻击力）
 */
public class ancient_power extends Item implements ICurioItem {

    /** 每等级伤害百分比加成：15% */
    public static final double DAMAGE_PERCENT_PER_LEVEL = 0.15;

    // 固定 UUID：用于属性修饰符
    public static final UUID DAMAGE_UUID = UUID.fromString("7b8c9d0e-1f2a-4b3c-9d4e-5f6a7b8c9d01");

    public ancient_power() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    /**
     * 通过 Curios 属性修饰符添加基于等级的伤害百分比加成
     * 使用 GENERIC_ATTACK_DAMAGE 作为基础属性
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 添加 15% 的基础攻击伤害加成（作为乘法因子）
        // 注意：这里设置为 0.15 表示 15% 的加成
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                DAMAGE_UUID,
                "Ancient Power Damage Bonus",
                DAMAGE_PERCENT_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.ancient_power.tooltip.desc")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.alone_adventure.ancient_power.tooltip.effect_desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.alone_adventure.ancient_power.tooltip.side_effect_desc")
                .withStyle(ChatFormatting.RED));
    }
}
