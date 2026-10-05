package Alone818.com.alone_adventure.Curios.miscCurios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
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
 * 习武之人 - 特质槽位饰品
 *
 * 被动效果：
 * 1. 攻击速度 +10%
 * 2. 伤害 +20%
 * 3. 造成伤害时有概率触发二连击（基础概率 10%，未触发时每次攻击 +5%，触发后重置）
 */
public class weapon_master extends Item implements ICurioItem {

    public static final float ATTACK_SPEED_BONUS = 0.1F;
    public static final float DAMAGE_BONUS = 0.2F;
    public static final float BASE_DOUBLE_ATTACK_CHANCE = 0.1F;
    public static final float PROBABILISTIC_INCREMENT = 0.05F;
    public static final float MAX_DOUBLE_ATTACK_CHANCE = 0.95F;

    // 固定 UUID：用于属性修饰符，便于幂等检测
    public static final UUID ATTACK_SPEED_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    public static final UUID DAMAGE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    public weapon_master() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    /**
     * 通过 Curios 标准路径应用属性修饰符：
     * 1. 攻击速度 +10%（MULTIPLY_TOTAL）
     * 2. 伤害 +20%（MULTIPLY_TOTAL）
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 攻击速度 +10%
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                ATTACK_SPEED_UUID,
                "Weapon Master Attack Speed",
                ATTACK_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_TOTAL));

        // 伤害 +20%
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                DAMAGE_UUID,
                "Weapon Master Damage",
                DAMAGE_BONUS,
                AttributeModifier.Operation.MULTIPLY_TOTAL));

        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.weapon_master.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.weapon_master.tooltip.speed_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.weapon_master.tooltip.damage_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.weapon_master.tooltip.double_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.weapon_master.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
