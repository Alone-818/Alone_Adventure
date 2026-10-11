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
 * 斯特拉克的挑战护手 - 契约槽位饰品
 *
 * 基础效果（根据当前生命值百分比动态切换）：
 * - 生命值 >50%：+20% 最大生命 / -20% 移速
 * - 生命值 ≤50%：-20% 护甲 / +20% 移速 / +10% 攻速
 *
 * 主动技能（按 R 键）：
 * - 生命值 >50%：扣血至 50% 并获得力量 III/速度 II 15 秒
 * - 生命值 ≤50%：回血至 50% 并获得虚弱 II/迟缓 I/再生 III 10 秒 + 最大生命 50% 的黄心
 */
public class strake_challenge_gauntlet extends Item implements ICurioItem {

    // ===== 属性修饰符 UUID =====
    public static final UUID MAX_HEALTH_HIGH_UUID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c9d-1e2f3a4b5c01");
    public static final UUID MOVEMENT_SPEED_HIGH_UUID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c9d-1e2f3a4b5c02");
    public static final UUID ARMOR_LOW_UUID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c9d-1e2f3a4b5c03");
    public static final UUID MOVEMENT_SPEED_LOW_UUID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c9d-1e2f3a4b5c04");
    public static final UUID ATTACK_SPEED_LOW_UUID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c9d-1e2f3a4b5c05");

    // ===== 主动技能冷却 =====
    public static final String TAG_SKILL_READY_AT = "StrakeSkillReadyAt";
    public static final int SKILL_COOLDOWN_TICKS = 300; // 15 秒 = 300 tick

    // ===== 基础属性数值 =====
    public static final double MAX_HEALTH_BONUS_HIGH = 0.20;  // +20% 最大生命
    public static final double MOVEMENT_SPEED_PENALTY_HIGH = -0.20; // -20% 移速
    public static final double ARMOR_PENALTY_LOW = -0.20;     // -20% 护甲
    public static final double MOVEMENT_SPEED_BONUS_LOW = 0.20;   // +20% 移速
    public static final double ATTACK_SPEED_BONUS_LOW = 0.10;     // +10% 攻速

    // ===== 主动技能效果 =====
    public static final int STRENGTH_LEVEL_HIGH = 2;        // 力量 III (amplifier = 2)
    public static final int SPEED_LEVEL_HIGH = 1;          // 速度 II (amplifier = 1)
    public static final int HIGH_HP_DURATION_TICKS = 300;  // 15 秒

    public static final int WEAKNESS_LEVEL_LOW = 2;        // 虚弱 III (amplifier = 2)
    public static final int SLOW_LEVEL_LOW = 2;           // 迟缓 III (amplifier = 2)
    public static final int REGENERATION_LEVEL_LOW = 2;   // 再生 III (amplifier = 2)
    public static final int LOW_HP_DURATION_TICKS = 200;  // 10 秒

    public strake_challenge_gauntlet() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    /**
     * 通过 Curios 属性修饰符提供基础属性
     * 注意：动态属性切换由 StrakeChallengeGauntletEvent 处理
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 默认提供高血量状态的属性（会在 Event 中动态切换）
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                MAX_HEALTH_HIGH_UUID,
                "Strake High HP Max Health",
                MAX_HEALTH_BONUS_HIGH,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                MOVEMENT_SPEED_HIGH_UUID,
                "Strake High HP Movement Speed",
                MOVEMENT_SPEED_PENALTY_HIGH,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));

        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.strake_challenge_gauntlet.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.alone_adventure.strake_challenge_gauntlet.tooltip.high_hp_desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.strake_challenge_gauntlet.tooltip.low_hp_desc")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.alone_adventure.strake_challenge_gauntlet.tooltip.skill_desc")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.strake_challenge_gauntlet.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
