package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 动力剑 - 来自 39 个千年后的神秘帝国武器
 * 由高度压缩的能量驱动剑刃，斩击力度远超寻常合金。
 *
 * 穿甲特性：对护甲值 >8 的敌人每点附加 0.3 点穿甲伤害，
 * 护甲韧性 >6 每点附加 0.5 点穿甲伤害（合计最高 10 点穿甲伤害）。
 * 穿甲/火焰/吸血结算统一在
 * {@link Alone818.com.alone_adventure.events.WeaponAttackEvent} 的注册处理器中完成，
 * 不依赖物品类自身的钩子。
 *
 * 超频特性：右键激活（持续 13 秒，损耗 5 点耐久），
 * 立即获得抗性 II 持续 5 秒，期间攻击伤害 +50%，命中额外造成 3 点火焰伤害并点燃 4 秒，
 * 攻击造成伤害的 60% 转化为自身治疗；使用后冷却 30 秒（原版物品冷却系统，客户端自动显示遮罩）。
 * 超频状态以结束游戏刻写入物品 NBT，跨维度/重登录依然有效。
 */
public class powersword extends SwordItem {

    // 基础战斗属性：伤害 8，攻击速度 1.6（剑的默认值；SwordItem 参数为相对基值 4.0 的修饰符）
    public static final int ATTACK_DAMAGE = 8;
    public static final float ATTACK_SPEED = 1.6F;

    // 穿甲特性参数
    public static final int ARMOR_PENALTY_THRESHOLD = 8;  // 护甲值超过此值才计算穿甲伤害
    public static final int ARMOR_TOUGHNESS_THRESHOLD = 6; // 护甲韧性超过此值才计算穿甲伤害
    public static final float ARMOR_PENALTY_MULTIPLIER = 0.3F;
    public static final float ARMOR_TOUGHNESS_MULTIPLIER = 0.5F;
    public static final int MAX_PENALTY_DAMAGE = 10;  // 穿甲伤害上限

    // 超频特性参数
    public static final int OVERCLOCK_DURATION_TICKS = 260; // 13 秒 * 20 tick/秒
    public static final int OVERCLOCK_DURABILITY_COST = 5;
    public static final int OVERCLOCK_COOLDOWN_TICKS = 600; // 30 秒 * 20 tick/秒
    public static final int RESISTANCE_DURATION_TICKS = 100; // 5 秒 * 20 tick/秒
    public static final int FIRE_DAMAGE = 3;
    public static final int FIRE_BURN_DURATION = 80; // 4 秒 * 20 tick/秒
    public static final float OVERCLOCK_DAMAGE_MULTIPLIER = 1.5F; // +50%
    public static final float OVERCLOCK_HEAL_RATIO = 0.6F; // 吸血比例：造成伤害的 60%

    // NBT 标签：超频结束的游戏刻（level.getGameTime() 口径）
    public static final String TAG_OVERCLOCK_END = "PowerswordOverclockEnd";

    public powersword() {
        super(Tiers.WOOD, ATTACK_DAMAGE, ATTACK_SPEED - 4.0F,
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(1000));
    }

    /**
     * 计算穿甲伤害：基于目标的护甲值和韧性计算额外穿透伤害。
     */
    public static float calculatePenetrationDamage(LivingEntity target) {
        int armor = target.getArmorValue();
        int toughness = (int) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        float extraDamage = 0.0F;

        if (armor > ARMOR_PENALTY_THRESHOLD) {
            extraDamage += (armor - ARMOR_PENALTY_THRESHOLD) * ARMOR_PENALTY_MULTIPLIER;
        }
        if (toughness > ARMOR_TOUGHNESS_THRESHOLD) {
            extraDamage += (toughness - ARMOR_TOUGHNESS_THRESHOLD) * ARMOR_TOUGHNESS_MULTIPLIER;
        }

        return Math.min(extraDamage, MAX_PENETRATION_DAMAGE_CAP());
    }

    private static int MAX_PENETRATION_DAMAGE_CAP() {
        return MAX_PENALTY_DAMAGE;
    }

    /**
     * 激活超频：把结束时刻写入物品 NBT。
     */
    public static void activateOverclock(ItemStack stack, long gameTime) {
        stack.getOrCreateTag().putLong(TAG_OVERCLOCK_END, gameTime + OVERCLOCK_DURATION_TICKS);
    }

    /**
     * 该堆栈当前是否处于超频状态（NBT 驱动，服务端权威）。
     */
    public static boolean isOverclocked(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_OVERCLOCK_END)
                && gameTime < tag.getLong(TAG_OVERCLOCK_END);
    }

    /**
     * 从玩家双手找出处于超频状态的动力剑；没有则返回 EMPTY。
     * 由 WeaponAttackEvent 的注册处理器调用。
     */
    public static ItemStack findOverclockedStack(LivingEntity attacker) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = attacker.getItemInHand(hand);
            if (stack.is(ModItems.POWERSWORD.get()) && isOverclocked(stack, attacker.level().getGameTime())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 超频期间右键激活；使用原版物品冷却系统（getCooldowns），
     * 冷却遮罩自动同步到客户端，冷却期间无法再次激活。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 正确的冷却 API：末影珍珠同款物品冷却
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            // 超频状态写入 NBT（服务端权威，随堆栈存档与同步）
            activateOverclock(stack, level.getGameTime());

            // 立即获得抗性 II 持续 5 秒
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    RESISTANCE_DURATION_TICKS,
                    1, false, true, true));

            // 损耗 5 点耐久
            stack.hurtAndBreak(OVERCLOCK_DURABILITY_COST, player,
                    p -> p.broadcastBreakEvent(hand));

            // 冷却 30 秒
            player.getCooldowns().addCooldown(this, OVERCLOCK_COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.powersword.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.powersword.tooltip.pierce_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.powersword.tooltip.overclock_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.powersword.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        // 超频剩余时间提示
        if (level != null && isOverclocked(stack, level.getGameTime())) {
            long remain = stack.getTag().getLong(TAG_OVERCLOCK_END) - level.getGameTime();
            tooltip.add(Component.translatable("item.alone_adventure.powersword.tooltip.overclock_active",
                            (remain + 19) / 20)
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}