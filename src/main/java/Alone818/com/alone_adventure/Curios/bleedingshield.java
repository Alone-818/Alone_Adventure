package Alone818.com.alone_adventure.Curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
 * 护盾饰品 - 减少玩家80%最大生命值
 * 同时提供护甲和护甲韧性的额外加成
 */
public class bleedingshield extends Item implements ICurioItem {

    // 配置常量（默认值，可由 Config 在 alone_adventure-common.toml 覆盖）
    public static double ARMOR_REDUCTION_RATIO = 0.8;    // 减少 80% 护甲
    public static double HEALTH_REDUCTION_RATIO = 0.2;   // 减少 20% 最大生命值
    public static double ARMOR_PER_TOUGHNESS = 4.0;      // 每减少 2 点护甲 +1 护甲韧性
    public static double HEALTH_PER_TOUGHNESS = 3.0;     // 每减少 1 点生命 +1 护甲韧性

    // 修饰符使用固定 UUID（与结晶之心的做法一致），便于检测是否已生效与手动兜底
    public static final UUID ARMOR_REDUCTION_UUID = UUID.fromString("1a2b3c4d-0001-4a5b-8c9d-abcdef000001");
    public static final UUID HEALTH_REDUCTION_UUID = UUID.fromString("1a2b3c4d-0002-4a5b-8c9d-abcdef000002");
    public static final UUID TOUGHNESS_BONUS_UUID = UUID.fromString("1a2b3c4d-0003-4a5b-8c9d-abcdef000003");

    public bleedingshield() {
        super(new Properties().stacksTo(1));
    }

    /**
     * 通过 Curios 标准路径应用减益修饰符（比例型，数值不随装备变化，Curios 应用一次即正确）。
     * 护甲韧性的加成不在这里：其数值取决于实际减少的护甲/血量点数，由 applyToughnessBonus 动态维护。
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 1. 减少护甲值（比例）
        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                ARMOR_REDUCTION_UUID,
                "Bleeding Shield Armor Reduction",
                -ARMOR_REDUCTION_RATIO,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        // 2. 减少最大生命值（比例）
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                HEALTH_REDUCTION_UUID,
                "Bleeding Shield Max Health Reduction",
                -HEALTH_REDUCTION_RATIO,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    /**
     * 计算被减少的护甲点数。修饰符已生效时 getArmorValue() 是剩余值，
     * 被减少量 = 剩余 × ratio/(1-ratio)；未生效时当前值为原值，直接乘 ratio。
     */
    public static double computeReducedArmor(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ARMOR);
        boolean applied = attr != null && attr.getModifier(ARMOR_REDUCTION_UUID) != null;
        double armor = player.getArmorValue();
        return applied
                ? armor * ARMOR_REDUCTION_RATIO / (1.0 - ARMOR_REDUCTION_RATIO)
                : armor * ARMOR_REDUCTION_RATIO;
    }

    /**
     * 计算被减少的最大生命值点数，原理同 computeReducedArmor。
     */
    public static double computeReducedHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        boolean applied = attr != null && attr.getModifier(HEALTH_REDUCTION_UUID) != null;
        double maxHealth = player.getMaxHealth();
        return applied
                ? maxHealth * HEALTH_REDUCTION_RATIO / (1.0 - HEALTH_REDUCTION_RATIO)
                : maxHealth * HEALTH_REDUCTION_RATIO;
    }

    /**
     * 动态换算额外护甲韧性：被减少的护甲点数 / 2 + 被减少的生命点数 / 1。
     * 示例：钻石套（护甲 20，被减 16 点）+ 满血 20（被减 4 点）= 8 + 4 = 12 点韧性。
     */
    public static double computeToughnessBonus(Player player) {
        return computeReducedArmor(player) / ARMOR_PER_TOUGHNESS
                + computeReducedHealth(player) / HEALTH_PER_TOUGHNESS;
    }

    /**
     * 幂等应用动态护甲韧性加成（ADDITION 点数型修饰符）。
     * 护甲或生命值变化导致换算结果变化时自动更新；数值未变时不做任何操作。
     */
    public static void applyToughnessBonus(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attr == null) return;

        double bonus = computeToughnessBonus(player);
        AttributeModifier existing = attr.getModifier(TOUGHNESS_BONUS_UUID);
        if (existing != null && Math.abs(existing.getAmount() - bonus) < 1e-6) return;

        attr.removeModifier(TOUGHNESS_BONUS_UUID);
        attr.addTransientModifier(new AttributeModifier(
                TOUGHNESS_BONUS_UUID,
                "Bleeding Shield Toughness Bonus",
                bonus,
                AttributeModifier.Operation.ADDITION
        ));
    }

    public static void removeToughnessBonus(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attr != null) {
            attr.removeModifier(TOUGHNESS_BONUS_UUID);
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;

        // 根据当前的护甲/生命值换算并应用韧性加成
        applyToughnessBonus(player);

        // 佩戴瞬间血量高于新上限（原最大生命值的 80%）时，扣血到新上限
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // 减益修饰符由 Curios 自行移除，这里只移除动态维护的韧性加成
        if (slotContext.entity() instanceof Player player) {
            removeToughnessBonus(player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.bleedingshield.tooltip.desc").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.bleedingshield.tooltip.toughness_desc").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.bleedingshield.tooltip.desc").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}