package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 链锯剑 —— 从 alone_journey 的 chainsword 移植到 alone_adventure。
 *
 * <p>移植时去除了 alone_journey 独有的依赖：
 * <ul>
 *   <li>{@code Config.chainswordOverclockDuration / chainswordOverclockCooldown}
 *       —— 改为本地常量 {@link #SLASH_MAX_USE_TICKS} / {@link #SLASH_COOLDOWN_TICKS}</li>
 *   <li>{@code ModEnchantments.METICULOUS_CRAFT} —— 附魔减冷却逻辑移除</li>
 *   <li>{@code ModEnchantments.HIGH_SPEED_SLASH} —— 附魔加伤 / 加撕裂逻辑移除</li>
 * </ul>
 * 即扫射伤害固定 {@value #SLASH_DAMAGE}、撕裂叠加固定 {@value #SLASH_LACERATION_LEVELS} 层、
 * 冷却固定 {@value #SLASH_COOLDOWN_TICKS} tick，完全不受附魔影响。
 */
public class chainsawsword extends SwordItem {

    // ===== 撕裂参数 =====
    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖；基础攻击伤害/攻速在注册期固定，不参与配置
    /** 普通攻击命中叠加的撕裂层数 */
    public static int LACERATION_LEVELS_PER_ATTACK = 4;
    /** 撕裂持续时间（tick），每次命中刷新 */
    public static int LACERATION_DURATION_TICKS = 40;

    // ===== 扫射参数 =====
    /** 扫射间隔：每 N tick 一刀（2 tick → 10 刀/秒） */
    public static int SLASH_INTERVAL_TICKS = 2;
    /** 每刀基础伤害（低伤，靠频率与撕裂累积） */
    public static float SLASH_DAMAGE = 0.5F;
    /** 每刀命中的撕裂叠加层数 */
    public static int SLASH_LACERATION_LEVELS = 1;
    /** 扫射扇形总角度（度），半角 = 该值 / 2 */
    public static float SLASH_ARC_DEGREES = 90.0F;
    /** 扫射作用半径（格） */
    public static float SLASH_RANGE = 4.0F;

    // ===== 时长 / 冷却 =====
    /** 单次扫射最长持续：7 秒（140 tick） */
    public static int SLASH_MAX_USE_TICKS = 20 * 7;
    /** 扫射结束后的冷却：15 秒（300 tick） */
    public static int SLASH_COOLDOWN_TICKS = 20 * 15;
    /** 不足此 tick 数视为短按，不施加冷却 */
    public static int MIN_USE_FOR_COOLDOWN_TICKS = 10;

    public chainsawsword() {
        // 钻石材料：附加伤害 5（+钻石基伤 3 = 8）、攻速修正 -2.8、耐久 880
        super(Tiers.DIAMOND, 5, -2.8F,
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(880));
    }

    // ===== 右键启动 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 冷却中直接拦截（客户端会经 ClientboundCooldownPacket 同步冷却，两侧一致）
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** 使用时长上限 = 7 秒。到时原版自动调用 finishUsingItem 收刀。 */
    @Override
    public int getUseDuration(ItemStack stack) {
        return SLASH_MAX_USE_TICKS;
    }

    // ===== 冷却 =====

    /** 松开右键时结算冷却。 */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        applySkillCooldown(entity, getUseDuration(stack) - timeLeft);
    }

    /** 使用时长自然耗尽（7 秒）时结算冷却。 */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        applySkillCooldown(entity, getUseDuration(stack));
        return super.finishUsingItem(stack, level, entity);
    }

    /**
     * 统一施加冷却：使用时长 ≥ {@value #MIN_USE_FOR_COOLDOWN_TICKS} tick 才触发，
     * 仅在服务端执行（客户端通过数据包同步冷却）。已去除附魔减冷却逻辑。
     */
    private void applySkillCooldown(LivingEntity entity, int heldTicks) {
        if (heldTicks < MIN_USE_FOR_COOLDOWN_TICKS) return;
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;
        player.getCooldowns().addCooldown(this, SLASH_COOLDOWN_TICKS);
    }

    // ===== 扫射结算 =====

    /**
     * 获取扫射伤害：基础伤害 × 玩家攻击伤害属性修正。
     * 使用 magic 伤害源，扫射伤害无视所有护甲与护甲韧性。
     */
    private float calculateSlashDamage(Player player) {
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // 链锯剑钻石剑尖：额外伤害 +5（剑本属性），但扫射使用玩家当前属性
        return (float) (SLASH_DAMAGE * attackDamage*0.2);
    }

    /**
     * 使用期间的每 tick 结算——扫射核心。
     * 每 {@value #SLASH_INTERVAL_TICKS} tick 对身前扇形（{@value #SLASH_ARC_DEGREES}°、
     * 半径 {@value #SLASH_RANGE} 格）内的所有生物造成 {@value #SLASH_DAMAGE} 点伤害并刷新撕裂。
     * 命中前把 {@code invulnerableTime} 归零以突破原版无敌帧。
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        // 间隔节流：用实体自身 tickCount 判定，避免依赖世界时间对齐
        if (entity.tickCount % SLASH_INTERVAL_TICKS != 0) return;

        Vec3 eyePos = entity.position().add(0, entity.getEyeHeight(), 0);
        Vec3 lookDir = entity.getLookAngle().normalize();
        AABB area = new AABB(
                eyePos.x - SLASH_RANGE, eyePos.y - SLASH_RANGE, eyePos.z - SLASH_RANGE,
                eyePos.x + SLASH_RANGE, eyePos.y + SLASH_RANGE, eyePos.z + SLASH_RANGE);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area);
        double cosHalfArc = Math.cos(Math.toRadians(SLASH_ARC_DEGREES / 2.0));

        for (LivingEntity target : candidates) {
            if (target == entity) continue;
            if (target.isAlliedTo(entity)) continue;

            Vec3 toTarget = target.position()
                    .add(0, target.getEyeHeight(), 0)
                    .subtract(eyePos)
                    .normalize();
            // 扇形夹角判定
            if (lookDir.dot(toTarget) < cosHalfArc) continue;

            // 归零无敌帧，使高频低伤真正生效
            target.invulnerableTime = 0;
            // 使用 magic 伤害源，扫射伤害无视所有护甲和护甲韧性
            float damage = calculateSlashDamage(player);
            target.hurt(entity.damageSources().magic(), damage);
            // 扫射不产生击退：清零水平速度
            target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
            // 命中刷新撕裂
            addLaceration(target, SLASH_LACERATION_LEVELS);
            // 命中消耗耐久，损毁则立刻收刀
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));

            if (stack.isEmpty()) {
                player.stopUsingItem();
                break;
            }
        }
    }

    // ===== 普通攻击叠撕裂 =====

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            addLaceration(target, LACERATION_LEVELS_PER_ATTACK);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * 叠加撕裂：新等级 = 原有等级 + 层数，并刷新持续时间。
     * amplifier = 等级 - 1，对 amplifier 直接相加即等价于层数相加。
     */
    public static void addLaceration(LivingEntity target, int stacks) {
        if (stacks <= 0) return;
        MobEffectInstance existing = target.getEffect(ModEffects.LACERATION.get());
        int newAmplifier = (existing == null ? stacks - 1 : existing.getAmplifier() + stacks);
        target.addEffect(new MobEffectInstance(
                ModEffects.LACERATION.get(),
                LACERATION_DURATION_TICKS,
                newAmplifier));
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.chainsawsword.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.chainsawsword.tooltip.tear_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.chainsawsword.tooltip.sweep_desc")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.chainsawsword.tooltip.desc")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}