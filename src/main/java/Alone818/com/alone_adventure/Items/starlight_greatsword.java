package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 星辉大剑 - 高冷却的重型大剑
 *
 * 特性（结算逻辑见 {@link Alone818.com.alone_adventure.events.StarlightGreatswordEvent}）：
 * - 高 CD：攻击间隔 0.8 秒（攻速 0.5），单发伤害高
 * - 举在手上持续聚能：每 3 秒 +1 层星辉储能（上限 20 层）
 * - 命中时：基础伤害 15 + 每层储能 +10% 伤害，并根据储能获得对应等级的伤害吸收（黄心，20 秒），
 *   随后储能清空重新积攒（未命中不消耗储能）
 */
public class starlight_greatsword extends SwordItem {

    // 基础战斗属性：伤害 15、攻速 0.5（高冷却武器，约 2 tick 起步、40 tick 一挥）
    public static final int ATTACK_DAMAGE = 15;
    public static final float ATTACK_SPEED = 0.5F;

    // ===== 星辉储能 =====
    /** 储能上限（= 伤害吸收等级上限） */
    public static final int MAX_CHARGE = 20;
    /** 每层储能的回复间隔：60 tick = 3 秒 */
    public static final int CHARGE_INTERVAL_TICKS = 60;
    /** 每层储能提供的攻击伤害加成（10%） */
    public static final float DAMAGE_PER_CHARGE = 0.10F;
    /** 命中后吸收（黄心）持续时长：20 秒 */
    public static final int ABSORPTION_DURATION_TICKS = 400;

    /** NBT 键：当前星辉储能层数 */
    public static final String TAG_CHARGE = "StarlightCharge";

    public starlight_greatsword() {
        super(Tiers.WOOD, ATTACK_DAMAGE, ATTACK_SPEED - 4.0F,
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(1200));
    }

    /** 当前储能层数（0 表示未聚能） */
    public static int getCharge(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CHARGE)) return 0;
        return Math.max(0, Math.min(MAX_CHARGE, tag.getInt(TAG_CHARGE)));
    }

    /** 设置储能层数（服务端调用；玩家背包堆栈会自动同步到客户端） */
    public static void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt(TAG_CHARGE, Math.max(0, Math.min(MAX_CHARGE, charge)));
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.starlight_greatsword.tooltip.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.starlight_greatsword.tooltip.charge_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.starlight_greatsword.tooltip.hit_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        // 储能常驻显示（NBT 在玩家背包中，随容器自动同步到客户端）
        int charge = getCharge(stack);
        if (charge > 0) {
            tooltip.add(Component.translatable("item.alone_adventure.starlight_greatsword.tooltip.charge",
                            charge, MAX_CHARGE)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
