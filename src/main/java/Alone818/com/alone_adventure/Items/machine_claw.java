package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 机器爪刃 - 高攻速连击武器
 *
 * 基础伤害 3、攻击速度 1.6（快）。连续命中实体时，每次命中使后续
 * 攻击伤害 +{@value #COMBO_DAMAGE_PER_HIT}（第 N 次连续命中伤害 = 基础 + (N-1)×1，
 * 首次命中无加成）。停顿超过 {@value #COMBO_TIMEOUT_TICKS} tick（默认 2 秒，
 * 含挥空落空）后连击加成清零。
 *
 * 连击层数与上次命中时刻存于物品 NBT（每把爪刃独立记录，
 * NBT 随背包容器同步到客户端供 tooltip 显示），
 * 伤害结算与连击递增由 {@link Alone818.com.alone_adventure.events.MachineClawEvent} 完成。
 */
public class machine_claw extends SwordItem {

    // 基础战斗属性：伤害 3、攻速 1.6（注册期固定，不参与配置）
    public static final int ATTACK_DAMAGE = 3;
    public static final float ATTACK_SPEED = 1.6F;

    // ===== 连击参数（默认值，可由 Config 在 alone_adventure-common.toml 覆盖）=====
    /** 每次命中的连击伤害加成（点） */
    public static int COMBO_DAMAGE_PER_HIT = 1;
    /** 连击重置的停顿时长（tick，40 = 2 秒） */
    public static int COMBO_TIMEOUT_TICKS = 40;

    // NBT 键：当前连击层数 / 上次命中时刻（gameTime）
    public static final String NB_TAG_COMBO = "ClawCombo";
    public static final String NB_TAG_LAST_HIT = "ClawLastHit";

    public machine_claw() {
        // WOOD 层级本身加成 0，参数直接给 3；攻速 1.6 = 基值 4.0 + 修饰符 -2.4
        super(Tiers.WOOD, ATTACK_DAMAGE, ATTACK_SPEED - 4.0F,
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(1000));
    }

    /** 当前连击层数（0 表示无连击，不做超时判断） */
    public static int getCombo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(NB_TAG_COMBO));
    }

    /**
     * 生效中的连击层数：已超时（停顿超过 {@link #COMBO_TIMEOUT_TICKS} tick，
     * 含挥空）时视为 0，下次命中重新从无加成开始。
     */
    public static int getEffectiveCombo(ItemStack stack, long now) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        int combo = Math.max(0, tag.getInt(NB_TAG_COMBO));
        if (combo > 0 && now - tag.getLong(NB_TAG_LAST_HIT) > COMBO_TIMEOUT_TICKS) return 0;
        return combo;
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.machine_claw.tooltip.desc")
                .withStyle(ChatFormatting.GRAY));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.machine_claw.tooltip.combo_desc")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        // 连击常驻显示（NBT 随背包容器同步到客户端；超时按客户端时刻显示为 0 层）
        int combo = getCombo(stack);
        if (combo > 0) {
            long now = level != null ? level.getGameTime() : 0L;
            int effective = getEffectiveCombo(stack, now);
            tooltip.add(Component.translatable("item.alone_adventure.machine_claw.tooltip.combo",
                            effective, effective * COMBO_DAMAGE_PER_HIT)
                    .withStyle(effective > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
    }
}
