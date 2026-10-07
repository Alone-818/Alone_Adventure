package Alone818.com.alone_adventure.Curios.miscCurios;

import Alone818.com.alone_adventure.init.ModItems;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 秃鹫 - 特质槽位饰品
 *
 * 属性效果：
 * - 移速 +20%
 * - 最大生命值 +10%
 * - 幸运 II（常态）
 *
 * 被动效果：
 * - 击杀怪物时有概率获得 2 倍掉落物
 * - 该效果在抢夺（Looting）附魔结算后追加，可与抢夺叠加
 */
public class bald_eagle extends Item implements ICurioItem {

    /** 移速 +20%（可由 Config 覆盖） */
    public static final float MOVEMENT_SPEED_BONUS = 0.2F;
    /** 最大生命值 +10%（可由 Config 覆盖） */
    public static final float MAX_HEALTH_BONUS = 0.1F;
    /** 掉落数翻倍概率：30%（可由 Config 覆盖） */
    public static float DOUBLING_CHANCE = 0.3F;

    // 固定 UUID：用于属性修饰符，便于幂等检测
    public static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
    public static final UUID MAX_HEALTH_UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

    public bald_eagle() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        // 每 20 tick (1 秒) 检查一次幸运效果
        if (player.tickCount % 20 != 0) return;

        // 确保玩家拥有幸运 II
        if (player.getEffect(MobEffects.LUCK) == null ||
            player.getEffect(MobEffects.LUCK).getAmplifier() < 1) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 1, false, true));
        }
    }

    /**
     * 通过 Curios 标准路径应用属性修饰符：
     * 1. 移速 +20%（MULTIPLY_BASE）
     * 2. 最大生命值 +10%（MULTIPLY_TOTAL）
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

        // 移速 +20%
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                MOVEMENT_SPEED_UUID,
                "Bald Eagle Movement Speed",
                MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE));

        // 最大生命值 +10%
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                MAX_HEALTH_UUID,
                "Bald Eagle Max Health",
                MAX_HEALTH_BONUS,
                AttributeModifier.Operation.MULTIPLY_TOTAL));

        return modifiers;
    }

    /** 判断玩家是否佩戴秃鹫（供掉落倍率检测使用） */
    public static boolean isWearing(Player player) {
        Optional<?> result = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.BALD_EAGLE.get());
        return result.isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.bald_eagle.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.bald_eagle.tooltip.luck_desc")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.alone_adventure.bald_eagle.tooltip.kill_desc")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.bald_eagle.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}