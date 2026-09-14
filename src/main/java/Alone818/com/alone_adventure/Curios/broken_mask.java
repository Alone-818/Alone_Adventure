package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 破损面具 - 契约槽位饰品
 *
 * 基础属性：+7 护甲、+20% 最大生命、+3 护甲韧性（Curios 标准属性修饰符路径，
 * 佩戴时自动生效并在饰品 GUI 中显示，脱下时自动移除）。
 *
 * 战斗效果（见 {@link Alone818.com.alone_adventure.events.BrokenMaskEvent}）：
 * - 攻击有 20% 概率对目标施加易伤 I（10 秒）
 * - 目标身上的易伤被伤害消耗时：佩戴者获得力量 I（5 秒）并回复固定数值生命
 */
public class broken_mask extends Item implements ICurioItem {

    // ===== 基础属性数值 =====
    public static final double ARMOR_BONUS = 7.0;        // +7 护甲（点数）
    public static final double MAX_HEALTH_BONUS = 0.20;  // +20% 最大生命（比例）
    public static final double TOUGHNESS_BONUS = 3.0;    // +3 护甲韧性（点数）

    // ===== 主动技能 =====
    // 半径（方块）：以自身为中心的球形范围
    public static final double SKILL_RADIUS = 8.0;
    // 范围内实体获得的易伤 II 持续时长：1 分钟
    public static final int SKILL_VULN_DURATION_TICKS = 1200;
    // 自身获得的抗性 II 持续时长：1 分钟（与易伤一致）
    public static final int SKILL_RESIST_DURATION_TICKS = 1200;
    // 自身治疗比例：生命上限的一半
    public static final float SKILL_HEAL_RATIO = 0.5F;

    // ===== 主动技能冷却 =====
    // 面具CD改为90秒 = 1800 tick
    public static final int SKILL_COOLDOWN_TICKS = 1800;

    // NBT 键：技能下次可用时的世界时刻（gameTime）
    public static final String TAG_SKILL_READY_AT = "BrokenMaskSkillReadyAt";

    // 固定 UUID：与结晶之心/流血护盾做法一致，便于幂等检测且不会叠加
    public static final UUID ARMOR_UUID = UUID.fromString("7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f01");
    public static final UUID MAX_HEALTH_UUID = UUID.fromString("7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f02");
    public static final UUID TOUGHNESS_UUID = UUID.fromString("7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f03");

    public broken_mask() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                ARMOR_UUID,
                "Broken Mask Armor",
                ARMOR_BONUS,
                AttributeModifier.Operation.ADDITION));

        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                MAX_HEALTH_UUID,
                "Broken Mask Max Health",
                MAX_HEALTH_BONUS,
                AttributeModifier.Operation.MULTIPLY_TOTAL));

        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                TOUGHNESS_UUID,
                "Broken Mask Toughness",
                TOUGHNESS_BONUS,
                AttributeModifier.Operation.ADDITION));

        return modifiers;
    }

    // ── 主动技能 ──

    /**
     * 服务端激活：佩戴破损面具且不在冷却中时，
     * - 对周围 {@link #SKILL_RADIUS} 格内所有 LivingEntity 施加易伤 II（1 分钟）
     * - 自身回复生命上限的一半并获得抗性 II（1 分钟）
     * 并进入 {@link #SKILL_COOLDOWN_TICKS} 冷却。
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt =
                CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BROKEN_MASK.get());
        if (curioOpt.isEmpty()) return false;

        ItemStack stack = curioOpt.get().stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) return false;

        Level level = player.level();

        // 1. 范围内所有实体施加易伤 II（amplifier = 1），跳过玩家自身
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SKILL_RADIUS));
        for (LivingEntity entity : nearby) {
            if (entity == player) continue;
            entity.addEffect(new MobEffectInstance(ModEffects.VULNERABILITY.get(),
                    SKILL_VULN_DURATION_TICKS, 1, false, true));
        }

        // 2. 自身治疗生命上限的一半并获得抗性 II（amplifier = 1）
        player.heal(player.getMaxHealth() * SKILL_HEAL_RATIO);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                SKILL_RESIST_DURATION_TICKS, 1, false, true));

        // 3. 写入冷却并写回真实槽位
        tag.putLong(TAG_SKILL_READY_AT, now + SKILL_COOLDOWN_TICKS);
        stack.setTag(tag);
        SlotResult slotResult = curioOpt.get();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        level.playSound(null, player, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 技能剩余冷却 tick（0 表示可用） */
    public static long getSkillCooldownRemaining(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SKILL_READY_AT) || level == null) return 0;
        return Math.max(0, tag.getLong(TAG_SKILL_READY_AT) - level.getGameTime());
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.vuln_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.consume_desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.attr_desc")
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.skill_desc")
                    .withStyle(ChatFormatting.GOLD));

            // 冷却中实时显示剩余秒数（NBT 由 Curios 同步到客户端）
            long remaining = getSkillCooldownRemaining(stack, level);
            if (remaining > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.cooldown_left",
                                Math.round(remaining / 20.0F))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.broken_mask.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
