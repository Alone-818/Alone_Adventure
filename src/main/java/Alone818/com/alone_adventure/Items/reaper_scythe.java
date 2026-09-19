package Alone818.com.alone_adventure.Items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import Alone818.com.alone_adventure.init.ModItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 巨大镰刀 - 击杀新生物获得伤害加成
 *
 * 特性：基础伤害 5，击杀新生物后攻击伤害 +2，无上限
 * 核心机制：击杀记录存储在每个物品的NBT中，物品之间互不影响
 */
public class reaper_scythe extends SwordItem {

    // 基础攻击伤害在注册期固定，不参与配置
    public static final int BASE_ATTACK_DAMAGE = 5;
    /** 每击杀一种新生物的攻击伤害加成（默认值，可由 Config 在 alone_adventure-common.toml 覆盖） */
    public static int DAMAGE_PER_NEW_KILL = 2;

    // 用于辨识这个物品独有的伤害修饰符
    private static final UUID BONUS_DAMAGE_UUID = UUID.fromString("b5b8c9d0-2a4e-4f5a-8b3c-9d6e7f8a9b0c");

    // NBT 数据 key 常量
    public static final String NBT_TAG_DATA = "ReaperScytheData";
    public static final String NBT_KILL_COUNT = "killCount";
    public static final String NBT_KILLED_TYPES = "killedTypes";

    public reaper_scythe() {
        super(Tiers.IRON, BASE_ATTACK_DAMAGE - 4, 1.2F - 4.0F,
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /**
     * 基础属性修饰符：主手给出基础伤害 5
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        if (slot == EquipmentSlot.MAINHAND) {
            modifiers.put(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BONUS_DAMAGE_UUID, "weapon",
                            BASE_ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.reaper_scythe.tooltip.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));

        // 显示当前伤害值（读取物品NBT中的击杀计数）
        int currentDamage = BASE_ATTACK_DAMAGE;
        int killCount = 0;

        CompoundTag scytheData = getReaperScytheData(stack);
        if (scytheData != null) {
            killCount = scytheData.getInt(NBT_KILL_COUNT);
        }

        currentDamage += killCount * DAMAGE_PER_NEW_KILL;



        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.reaper_scythe.tooltip.damage", currentDamage)
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.reaper_scythe.tooltip.bonus", DAMAGE_PER_NEW_KILL)
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.reaper_scythe.tooltip.upcap")
                    .withStyle(ChatFormatting.YELLOW));
        }
        else {        tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /**
     * 获取物品的击杀计数（从物品NBT读取，独立记录）
     */
    public int getItemKillCount(ItemStack stack) {
        CompoundTag data = getReaperScytheData(stack);
        return data != null ? data.getInt(NBT_KILL_COUNT) : 0;
    }

    /**
     * 获取物品击杀的种类集合（从物品NBT读取，独立记录）
     */
    public Set<String> getItemKilledTypes(ItemStack stack) {
        CompoundTag data = getReaperScytheData(stack);
        Set<String> types = new HashSet<>();
        if (data != null && data.contains(NBT_KILLED_TYPES)) {
            String[] items = data.getString(NBT_KILLED_TYPES).split(",");
            for (String item : items) {
                if (!item.isEmpty()) {
                    types.add(item);
                }
            }
        }
        return types;
    }

    /**
     * 增加一次击杀计数（更新物品NBT）
     */
    public void incrementKillCount(ItemStack stack, ResourceLocation entityType) {
        CompoundTag data = getOrCreateReaperScytheData(stack);
        Set<String> types = getItemKilledTypes(stack);
        String entityTypeStr = entityType.toString();

        // 检查是否是新种类
        if (types.contains(entityTypeStr)) {
            return;
        }

        // 是新种类，增加计数
        int count = data.getInt(NBT_KILL_COUNT) + 1;
        data.putInt(NBT_KILL_COUNT, count);
        types.add(entityTypeStr);

        // 更新种类列表
        StringBuilder sb = new StringBuilder();
        for (String type : types) {
            if (sb.length() > 0) sb.append(",");
            sb.append(type);
        }
        data.putString(NBT_KILLED_TYPES, sb.toString());
    }

    /**
     * 获取物品的击杀数据NBT标签
     */
    public CompoundTag getReaperScytheData(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_TAG_DATA)) {
            return stack.getTag().getCompound(NBT_TAG_DATA);
        }
        return null;
    }

    /**
     * 获取或创建物品的击杀数据NBT标签
     */
    public CompoundTag getOrCreateReaperScytheData(ItemStack stack) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }
        CompoundTag data = stack.getTag().getCompound(NBT_TAG_DATA);
        if (!stack.getTag().contains(NBT_TAG_DATA)) {
            data = new CompoundTag();
        }
        stack.getTag().put(NBT_TAG_DATA, data);
        return data;
    }

    /**
     * 获取当前总伤害值（基础伤害 + 击杀加成）
     */
    public int getCurrentTotalDamage(ItemStack stack) {
        return BASE_ATTACK_DAMAGE + getItemKillCount(stack) * DAMAGE_PER_NEW_KILL;
    }

    /**
     * 计算额外伤害（击杀数量 × 每次加成）
     */
    public int calculateBonusDamage(ItemStack stack) {
        return getItemKillCount(stack) * DAMAGE_PER_NEW_KILL;
    }

    /**
     * 处理击杀事件：记录击杀的新种类到物品NBT（每个物品独立记录）
     */
    public static void onKill(Player player, ResourceLocation entityType, ItemStack stack) {
        if (stack.getItem() instanceof reaper_scythe scythe) {
            // 检查是否是新种类
            Set<String> types = scythe.getItemKilledTypes(stack);
            String entityTypeStr = entityType.toString();

            // 已击杀过同类，直接返回
            if (types.contains(entityTypeStr)) {
                return;
            }

            // 是新种类，增加计数到物品NBT
            CompoundTag data = scythe.getOrCreateReaperScytheData(stack);
            int count = data.getInt(NBT_KILL_COUNT) + 1;
            data.putInt(NBT_KILL_COUNT, count);
            types.add(entityTypeStr);

            // 更新种类列表
            StringBuilder sb = new StringBuilder();
            for (String type : types) {
                if (sb.length() > 0) sb.append(",");
                sb.append(type);
            }
            data.putString(NBT_KILLED_TYPES, sb.toString());
        }
    }
}