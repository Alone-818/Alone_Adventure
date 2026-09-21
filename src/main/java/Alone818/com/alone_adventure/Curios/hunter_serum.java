package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.network.HunterVisionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;

/**
 * 猎人血清 - 契约槽位饰品
 *
 * 被动：使用针剂（injection_syringe）时，获得的药水效果等级 +{@value #POTION_LEVEL_BONUS}，
 * 且效果时长可与现有同类效果叠加（结算见 injection_template.finishUsingItem）。
 *
 * 主动技能（按键触发，默认 R，与其他主动饰品共用）：
 * - 视角变为黑白：客户端由 HunterVisionClient 加载去色后处理着色器
 * - 视野范围内（半径 {@value #HIGHLIGHT_RADIUS} 格）生物以红色线框高亮、
 *   凋落物蓝色、容器（箱子/木桶/潜影盒等）黄色，均穿墙可见且仅本人可见
 * - 视野期间攻击无视目标 {@value #ARMOR_PEN_RATIO} 比例的护甲与
 *   {@value #TOUGHNESS_PEN_RATIO} 比例的护甲韧性（见 HunterSerumEvent）
 * - 持续 {@value #SKILL_DURATION_TICKS} tick，冷却 {@value #SKILL_COOLDOWN_TICKS} tick
 */
public class hunter_serum extends Item implements ICurioItem {

    /** 猎人视野持续：15 秒 = 300 tick（默认值，可由 Config 覆盖） */
    public static int SKILL_DURATION_TICKS = 300;
    /** 技能冷却：60 秒 = 1200 tick */
    public static int SKILL_COOLDOWN_TICKS = 1200;
    /** 高亮半径（格） */
    public static int HIGHLIGHT_RADIUS = 64;
    /** 被动：使用针剂时药水效果的等级加成（默认值，可由 Config 覆盖） */
    public static int POTION_LEVEL_BONUS = 1;
    /** 视野期间：攻击无视的目标护甲比例（0.4 = 40%，默认值，可由 Config 覆盖） */
    public static float ARMOR_PEN_RATIO = 0.4F;
    /** 视野期间：攻击无视的目标护甲韧性比例（0.2 = 20%） */
    public static float TOUGHNESS_PEN_RATIO = 0.2F;

    /** NBT 键：技能下次可用时的世界时刻（gameTime） */
    public static final String TAG_SKILL_READY_AT = "HunterSerumReadyAt";
    /** NBT 键：猎人视野结束时刻（gameTime，服务端穿甲结算用） */
    public static final String TAG_VISION_END = "HunterSerumVisionEnd";

    public hunter_serum() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    /** 技能剩余冷却 tick（0 表示可用） */
    public static long getSkillCooldownRemaining(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SKILL_READY_AT) || level == null) return 0;
        return Math.max(0, tag.getLong(TAG_SKILL_READY_AT) - level.getGameTime());
    }

    /** 判断玩家是否佩戴猎人血清（供针剂等被动机制查询） */
    public static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.HUNTER_SERUM.get()).isPresent();
    }

    /** 判断玩家的猎人视野当前是否激活（视野结束时刻未到且仍佩戴血清） */
    public static boolean isVisionActive(Player player) {
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.HUNTER_SERUM.get());
        if (curioOpt.isEmpty()) return false;
        CompoundTag tag = curioOpt.get().stack().getTag();
        return tag != null && tag.contains(TAG_VISION_END)
                && player.level().getGameTime() < tag.getLong(TAG_VISION_END);
    }

    /**
     * 服务端激活：向客户端发 HunterVisionPacket
     * （去色着色器 + 生物/凋落物/容器线框高亮），随后进入冷却。
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt =
                CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.HUNTER_SERUM.get());
        if (curioOpt.isEmpty()) return false;

        SlotResult slotResult = curioOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) return false;

        // 1. 客户端猎人视野（黑白 + 高亮），仅发给本人
        Alone_adventure.NETWORK.send(PacketDistributor.PLAYER.with(() -> player),
                new HunterVisionPacket(SKILL_DURATION_TICKS));

        // 2. 服务端记录视野结束时刻（穿甲结算用）与技能冷却，写回同步
        tag.putLong(TAG_VISION_END, now + SKILL_DURATION_TICKS);
        tag.putLong(TAG_SKILL_READY_AT, now + SKILL_COOLDOWN_TICKS);
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        player.level().playSound(null, player, SoundEvents.SPYGLASS_USE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.hunter_serum.tooltip.desc")
                .withStyle(ChatFormatting.DARK_AQUA));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.hunter_serum.tooltip.passive_desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.hunter_serum.tooltip.skill_desc")
                    .withStyle(ChatFormatting.GOLD));

            // 冷却中实时显示剩余秒数（NBT 由 Curios 同步到客户端）
            long remaining = getSkillCooldownRemaining(stack, level);
            if (remaining > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.hunter_serum.tooltip.cooldown_left",
                                Math.round(remaining / 20.0F))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
