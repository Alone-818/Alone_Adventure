package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/**
 * 帝国天鹰 - 人类帝国的双头鹰徽记（参考《战锤 40K》）。
 *
 * 常态效果：
 * - 负面效果反转：虚弱→力量、缓慢→速度、黑暗→夜视、中毒→再生
 *   （每 tick 在 curioTick 中检查并转换，保留原等级与剩余时长）
 * - 免疫火焰与岩浆伤害（由 {@link Alone818.com.alone_adventure.events.ImperialEagleEvent} 处理）
 *
 * 主动技能（按键触发，见 {@link Alone818.com.alone_adventure.network.EagleSkillPacket}）：
 * - 获得抗性 II、力量 I、生命恢复 II，持续 2 分钟，冷却 30 秒
 */
public class imperial_eagle extends Item implements ICurioItem {

    /** 主动技能持续：2 分钟 = 2400 tick（默认值，可由 Config 覆盖） */
    public static int SKILL_TICKS = 2400;
    /** 主动技能冷却：30 秒 = 600 tick（默认值，可由 Config 覆盖） */
    public static int SKILL_COOLDOWN_TICKS = 600;

    /** NBT 键：技能下次可用时的世界时刻（gameTime） */
    public static final String TAG_SKILL_READY_AT = "EagleSkillReadyAt";

    public imperial_eagle() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    // ── 常态：负面效果反转 ──

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        // 节流：每 5 tick 检查一次。负面效果（虚弱/缓慢等）来源时长都远大于 5 tick，
        // 最多 0.25 秒的反转延迟玩家无感知，换来 4 次效果查找降为 1/5
        if (player.tickCount % 5 != 0) return;

        invert(player, MobEffects.WEAKNESS, MobEffects.DAMAGE_BOOST);
        invert(player, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.MOVEMENT_SPEED);
        invert(player, MobEffects.DARKNESS, MobEffects.NIGHT_VISION);
        invert(player, MobEffects.POISON, MobEffects.REGENERATION);
    }

    /** 把 from 效果替换为 to 效果，保留等级与剩余时长 */
    private static void invert(ServerPlayer player, MobEffect from, MobEffect to) {
        MobEffectInstance instance = player.getEffect(from);
        if (instance != null) {
            player.removeEffect(from);
            player.addEffect(new MobEffectInstance(to, instance.getDuration(),
                    instance.getAmplifier(), false, true));
        }
    }

    // ── 主动技能 ──

    /**
     * 服务端激活：佩戴天鹰且不在冷却中时，
     * 给予抗性 II / 力量 I / 生命恢复 II 各 2 分钟，并进入 30 秒冷却。
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt =
                CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.IMPERIAL_EAGLE.get());
        if (curioOpt.isEmpty()) return false;

        ItemStack stack = curioOpt.get().stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) return false;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SKILL_TICKS, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, SKILL_TICKS, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SKILL_TICKS, 1, false, true));

        tag.putLong(TAG_SKILL_READY_AT, now + SKILL_COOLDOWN_TICKS);
        stack.setTag(tag);
        SlotResult slotResult = curioOpt.get();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        player.level().playSound(null, player, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
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
        tooltip.add(Component.translatable("item.alone_adventure.imperial_eagle.tooltip.desc")
                .withStyle(ChatFormatting.GOLD));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.imperial_eagle.tooltip.invert")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.imperial_eagle.tooltip.immunity")
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.imperial_eagle.tooltip.skill")
                    .withStyle(ChatFormatting.GREEN));

            // 冷却中实时显示剩余秒数（NBT 由 Curios 同步到客户端）
            long remaining = getSkillCooldownRemaining(stack, level);
            if (remaining > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.imperial_eagle.tooltip.cooldown_left",
                                Math.round(remaining / 20.0F))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
