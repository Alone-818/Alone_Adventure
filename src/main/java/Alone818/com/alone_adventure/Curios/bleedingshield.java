package Alone818.com.alone_adventure.Curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * 鲜血护盾 - 以自身鲜血换取更强护甲的契约饰品
 *
 * 佩戴时减少 20% 最大生命值，同时获得 +30% 护甲加成（比例型修饰符，
 * MULTIPLY_TOTAL，实际护甲 = 基础护甲 × 1.3）。
 * 护甲减少与生命值减少的比例型修饰符由 Curios 自动管理，
 * 无需手动维护。
 */
public class bleedingshield extends Item implements ICurioItem {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 最大生命值削减比例（0.2 = 减少 20%） */
    public static double HEALTH_REDUCTION_RATIO = 0.2;
    /** 护甲加成比例（0.3 = +30%） */
    public static double ARMOR_BONUS_RATIO = 0.3;

    // 修饰符使用固定 UUID，便于幂等应用与检测
    public static final UUID HEALTH_REDUCTION_UUID = UUID.fromString("1a2b3c4d-0002-4a5b-8c9d-abcdef000002");
    public static final UUID ARMOR_BONUS_UUID = UUID.fromString("1a2b3c4d-0004-4a5b-8c9d-abcdef000004");

    public bleedingshield() {
        super(new Properties().stacksTo(1));
    }

    /**
     * 通过 Curios 标准路径应用修饰符：
     * 1. 减少最大生命值（比例）
     * 2. 增加护甲值（比例）
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 1. 减少最大生命值（比例）
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                HEALTH_REDUCTION_UUID,
                "Bleeding Shield Max Health Reduction",
                -HEALTH_REDUCTION_RATIO,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        // 2. 增加护甲值（比例 +30%）
        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                ARMOR_BONUS_UUID,
                "Bleeding Shield Armor Bonus",
                ARMOR_BONUS_RATIO,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    /**
     * 佩戴瞬间血量高于新上限（原最大生命值的 80%）时，扣血到新上限
     */
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.bleedingshield.tooltip.desc").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.bleedingshield.tooltip.armor_desc").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.bleedingshield.tooltip.desc").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
