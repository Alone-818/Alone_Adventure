package Alone818.com.alone_adventure.Curios;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import Alone818.com.alone_adventure.init.ModItems;
import java.util.List;
import java.util.Optional;

/**
 * 紧缚绷带 - 契约槽位饰品
 *
 * 被动效果（见 {@link Alone818.com.alone_adventure.events.BandageEvent}）：
 * - 每次攻击命中有 10% 概率为自己施加耐力 I（10 秒，不可叠加：
 *   已有耐力时不重复施加、也不刷新时长）
 *
 * 主动技能（按键触发，默认 R，与帝国天鹰共用按键）：
 * - 获得 1 点护盾（存于本饰品 NBT，上限 3 点），冷却 45 秒
 * - 护盾机制与结晶心脏一致：受伤时抵消本次伤害并扣除 1 点护盾，
 *   由 {@link Alone818.com.alone_adventure.events.BandageEvent} 结算，
 *   由 {@link Alone818.com.alone_adventure.client.ShieldHudOverlay} 显示
 */
public class binding_bandage extends Item implements ICurioItem {

    // ===== 被动：攻击自施耐力 =====
    public static final float ENDURANCE_CHANCE = 0.10F;        // 10% 概率
    public static final int ENDURANCE_DURATION_TICKS = 200;    // 10 秒

    // ===== 主动：护盾 =====
    public static final int SHIELD_GAIN = 1;                   // 每次触发 +1 点护盾
    public static final int SHIELD_MAX = 3;                    // 护盾存储上限
    public static final int SKILL_COOLDOWN_TICKS = 900;        // 冷却 45 秒 = 900 tick

    // NBT 键
    public static final String NB_TAG_SHIELD = "BandageShield";            // 当前护盾点数
    public static final String TAG_SKILL_READY_AT = "BandageSkillReadyAt"; // 技能下次可用时刻

    public binding_bandage() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    // ── 护盾数据读写（供 BandageEvent 与 HUD 使用）──

    /** 当前护盾点数（0 表示无盾） */
    public static double getShield(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getDouble(NB_TAG_SHIELD);
    }

    /** 服务端激活：佩戴绷带且不在冷却中时，护盾 +1（不超过上限），进入冷却 */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt =
                CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BINDING_BANDAGE.get());
        if (curioOpt.isEmpty()) return false;

        SlotResult slotResult = curioOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) return false;

        double shield = tag.getDouble(NB_TAG_SHIELD);
        if (shield >= SHIELD_MAX) return false; // 已满时不进入冷却，提示文本可见

        tag.putDouble(NB_TAG_SHIELD, Math.min(SHIELD_MAX, shield + SHIELD_GAIN));
        tag.putLong(TAG_SKILL_READY_AT, now + SKILL_COOLDOWN_TICKS);
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_CHAIN,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    /** 技能剩余冷却 tick（0 表示可用） */
    public static long getSkillCooldownRemaining(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SKILL_READY_AT) || level == null) return 0;
        return Math.max(0, tag.getLong(TAG_SKILL_READY_AT) - level.getGameTime());
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // 脱下时护盾保留在堆栈 NBT 中（重新佩戴可继续使用），此处无需清理
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.binding_bandage.tooltip.desc")
                .withStyle(ChatFormatting.WHITE));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.binding_bandage.tooltip.passive_desc")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.binding_bandage.tooltip.active_desc")
                    .withStyle(ChatFormatting.AQUA));

            double shield = getShield(stack);
            if (shield > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.binding_bandage.tooltip.shield",
                                (int) shield, SHIELD_MAX)
                        .withStyle(ChatFormatting.BLUE));
            }
            long remaining = getSkillCooldownRemaining(stack, level);
            if (remaining > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.binding_bandage.tooltip.cooldown_left",
                                Math.round(remaining / 20.0F))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
