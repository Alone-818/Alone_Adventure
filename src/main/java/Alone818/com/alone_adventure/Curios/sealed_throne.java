package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
 * 封印王座 - 契约槽位饰品
 *
 * 星辉机制：
 * - 星辉上限 {@value #MAX_STARS} 点，佩戴时缓慢回复：每 {@value #STAR_REGEN_TICKS} tick
 *   （5 秒）回复 1 点（curioTick 服务端计时，NBT 随堆栈同步到客户端显示）
 *
 * 被动（按当前星辉动态调整，见 {@link #refreshStarAttributes}）：
 * - 攻击伤害：每个星辉 +5%（MULTIPLY_TOTAL）
 * - 护甲：每 4 个星辉 +2 点（向下取整）
 *
 * 受击（见 {@link Alone818.com.alone_adventure.events.ThroneEvent}）：
 * - 受到攻击且实际扣除红心时，减少 {@value #STAR_LOSS_PER_HIT} 点星辉
 *   （护盾抵消本次伤害时红心未减少，不扣星辉）
 *
 * 主动技能（按键触发，默认 R，与其他主动饰品共用）：
 * - 消耗全部星辉：每 6 点星辉获得 1 点王座护盾；
 *   获得等于星辉数量的黄心（原版吸收效果）
 */
public class sealed_throne extends Item implements ICurioItem {

    // ===== 星辉 =====
    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 星辉上限 */
    public static int MAX_STARS = 12;
    /** 星辉回复间隔：5 秒 = 100 tick */
    public static int STAR_REGEN_TICKS = 100;
    /** 受击扣除红心时损失的星辉 */
    public static int STAR_LOSS_PER_HIT = 2;
    /** 每个星辉提供的攻击伤害比例（5%） */
    public static double DAMAGE_PER_STAR = 0.05;
    /** 每 4 个星辉提供的护甲点数 */
    public static int STARS_PER_ARMOR = 4;
    public static double ARMOR_PER_GROUP = 2.0;

    // ===== 主动技能 =====
    /** 每 6 点星辉转化为 1 点护盾 */
    public static int STARS_PER_SHIELD = 6;
    /** 王座护盾堆叠上限：4 点 */
    public static int SHIELD_MAX = 4;
    /** 黄心（吸收）持续时间：20 秒 */
    public static int ABSORPTION_DURATION_TICKS = 400;

    // NBT 键
    public static final String NB_TAG_STARS = "ThroneStars";           // 当前星辉
    public static final String NB_TAG_SHIELD = "ThroneShield";         // 当前护盾点数
    public static final String NB_TAG_NEXT_STAR = "ThroneNextStar";    // 下一次星辉回复时刻
    public static final String NB_TAG_SKILL_PENDING = "ThroneSkillPending"; // 技能消耗后等待星辉回复的标记

    // 动态属性修饰符（固定 UUID，随星辉数量增删改）
    public static final UUID STAR_DAMAGE_UUID = UUID.fromString("5e6f7a8b-2c3d-4e5f-9a0b-1c2d3e4f5a01");
    public static final UUID STAR_ARMOR_UUID = UUID.fromString("5e6f7a8b-2c3d-4e5f-9a0b-1c2d3e4f5a02");

    public sealed_throne() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    // ── 星辉数据读写 ──

    public static int getStars(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, Math.min(MAX_STARS, tag.getInt(NB_TAG_STARS)));
    }

    /** 设置星辉并同步属性修饰符（服务端）；星辉归零时护盾随之熄灭 */
    public static void setStars(Player player, ItemStack stack, int stars) {
        stars = Math.max(0, Math.min(MAX_STARS, stars));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NB_TAG_STARS, stars);
        if (stars <= 0) tag.putDouble(NB_TAG_SHIELD, 0);
        refreshStarAttributes(player, stars);
    }

    public static double getShield(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getDouble(NB_TAG_SHIELD);
    }

    // ── 动态属性：攻击伤害与护甲随星辉变化 ──

    /**
     * 按当前星辉重设两个 transient 修饰符。
     * 修饰符数值与目标一致时跳过写入，避免每 tick 重复操作属性实例。
     * 未佩戴（curioTick 不再运行）时由 {@link #onUnequip} 负责移除。
     */
    public static void refreshStarAttributes(Player player, int stars) {
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            double wantDamageMul = DAMAGE_PER_STAR * stars;
            AttributeModifier mod = damage.getModifier(STAR_DAMAGE_UUID);
            boolean damageOk = stars == 0 ? mod == null
                    : mod != null && Math.abs(mod.getAmount() - wantDamageMul) < 1.0E-6;
            if (!damageOk) {
                damage.removeModifier(STAR_DAMAGE_UUID);
                if (stars > 0) {
                    damage.addTransientModifier(new AttributeModifier(
                            STAR_DAMAGE_UUID, "Sealed Throne Starlight Damage",
                            wantDamageMul, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            double wantArmor = stars / STARS_PER_ARMOR * ARMOR_PER_GROUP;
            AttributeModifier mod = armor.getModifier(STAR_ARMOR_UUID);
            boolean armorOk = wantArmor <= 0 ? mod == null
                    : mod != null && Math.abs(mod.getAmount() - wantArmor) < 1.0E-6;
            if (!armorOk) {
                armor.removeModifier(STAR_ARMOR_UUID);
                if (wantArmor > 0) {
                    armor.addTransientModifier(new AttributeModifier(
                            STAR_ARMOR_UUID, "Sealed Throne Starlight Armor",
                            wantArmor, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    // ── 佩戴 tick：星辉回复与属性刷新 ──

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        CompoundTag tag = stack.getOrCreateTag();
        boolean tagModified = false;

        long now = player.level().getGameTime();

        if (!tag.contains(NB_TAG_STARS)) {
            tag.putInt(NB_TAG_STARS, 0);
            tag.putLong(NB_TAG_NEXT_STAR, now + STAR_REGEN_TICKS);
            tagModified = true;
        }

        int stars = getStars(stack);
        boolean starsChanged = false;

        if (stars < MAX_STARS) {
            long next = tag.contains(NB_TAG_NEXT_STAR)
                    ? tag.getLong(NB_TAG_NEXT_STAR) : now + STAR_REGEN_TICKS;
            if (now >= next) {
                stars = Math.min(MAX_STARS, stars + 1);
                tag.putInt(NB_TAG_STARS, stars);
                tag.putLong(NB_TAG_NEXT_STAR, now + STAR_REGEN_TICKS);
                starsChanged = true;
                tagModified = true;
            }
        }

        // 写回堆栈并同步到 Curios 客户端
        if (tagModified) {
            stack.setTag(tag);
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.setEquippedCurio(slotContext.identifier(),
                            slotContext.index(), stack));
        }

        // 星辉变化时才刷新属性；每 20 tick 兜底一次，
        // 覆盖重登录等 transient 修饰符被清除的情况（幂等检查开销极低）
        if (starsChanged || player.tickCount % 20 == 0) {
            refreshStarAttributes(player, stars);
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // 佩戴瞬间按当前星辉恢复属性，避免等到 20 tick 兜底才生效
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            refreshStarAttributes(player, getStars(stack));
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // 移除动态属性修饰符（星辉与护盾数据保留在堆栈 NBT 中，重新佩戴可继续使用）
        if (slotContext.entity() instanceof Player player) {
            AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.removeModifier(STAR_DAMAGE_UUID);
            AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(STAR_ARMOR_UUID);
        }
    }

    // ── 主动技能 ──

    /**
     * 服务端激活：佩戴王座且拥有星辉时，消耗全部星辉——
     * 每 {@value #STARS_PER_SHIELD} 点星辉 +1 护盾，
     * 并获得等同于星辉数量的黄心（吸收，20 秒）。星辉回复速度即天然冷却。
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt =
                CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.SEALED_THRONE.get());
        if (curioOpt.isEmpty()) return false;

        SlotResult slotResult = curioOpt.get();
        ItemStack stack = slotResult.stack();
        int stars = getStars(stack);
        if (stars <= 0) return false; // 无星辉可消耗

        // 1. 消耗全部星辉并立即刷新属性；打上"待就绪"标记，
        //    星辉回复到第一点时由 SkillReadyEvent 播放升级声并给出字幕提示
        //    保存旧护盾值，避免 setStars 归零星辉时连带清盾导致无法叠加
        double oldShield = getShield(stack);
        setStars(player, stack, 0);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(NB_TAG_NEXT_STAR, player.level().getGameTime() + STAR_REGEN_TICKS);
        tag.putBoolean(NB_TAG_SKILL_PENDING, true);

        // 2. 护盾：每 6 点星辉 +1，叠加到剩余护盾上（上限 SHIELD_MAX）
        double newShield = Math.min(SHIELD_MAX, oldShield + stars / STARS_PER_SHIELD);
        tag.putDouble(NB_TAG_SHIELD, newShield);
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        // 3. 黄心：数量 = 星辉数（吸收每级 2 点 = 1 心，amplifier 为 星辉-1）
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                ABSORPTION_DURATION_TICKS, stars - 1, false, true));

        player.level().playSound(null, player, SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.star_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.passive_desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.hit_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.skill_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        // 星辉与护盾常驻显示（NBT 由 Curios 同步到客户端）
        int stars = getStars(stack);
        Component starsLine = Component.translatable("item.alone_adventure.sealed_throne.tooltip.stars",
                        stars, MAX_STARS)
                .withStyle(stars == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.LIGHT_PURPLE);
        tooltip.add(starsLine);
        // 星辉回复提示：每 5 秒 +1（仅 Shift 时显示）
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.regen_desc")
                    .withStyle(ChatFormatting.AQUA));
        }
        double shield = getShield(stack);
        if (shield > 0) {
            tooltip.add(Component.translatable("item.alone_adventure.sealed_throne.tooltip.shield",
                            (int) shield)
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
