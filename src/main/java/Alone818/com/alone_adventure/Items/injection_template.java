package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.Curios.hunter_serum;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用药水针剂模板
 *
 * 合成配方：瓶装药水 + 怪物残灰 + 2 个空针管 → 2 个对应针剂
 * 针剂效果持续时间 = 原药水效果的 0.8 倍
 * 针剂使用时间 = 药水使用时间的 0.5 倍（药水 32 tick，针剂 16 tick）
 * 佩戴猎人血清使用时：效果等级 +1，且时长与现有同类效果叠加
 *
 * 混合配方（见 InjectionMixRecipe）：2 支针剂 + 任意数量的怪物残灰/血脉碎片 → 2 支混合针剂，
 * 每个残灰使全部效果等级 +1、时长 ×0.6；血脉碎片按基础时长累加（N 个 = ×(1 + 0.6N)）；
 * 针剂最多 MAX_EFFECTS（5）种效果，混合针剂仍然是普通针剂。
 * 效果受上限约束：等级 ≤ 3 级（III，MAX_AMPLIFIER），时长 ≤ 30 分钟（MAX_DURATION_TICKS）。
 *
 * 所有瓶装药水（含混制、喷溅、滞留）都能检测，效果存储在针剂的 CustomPotionEffects 中。
 */
public class injection_template extends Item {

    public static final double DURATION_RATIO = 0.8;   // 效果时长倍率
    public static final double USE_RATIO = 0.5;        // 使用时间倍率
    public static final double MIX_ASH_DURATION_RATIO = 0.6;  // 混合时每个残灰的时长倍率（-40%，叠乘）
    public static final double MIX_BLOOD_DURATION_BONUS = 0.6; // 混合时每个血脉碎片的时长加成（+60%，按基础时长累加）
    public static final int MAX_EFFECTS = 5;           // 针剂效果种类上限
    public static final int MAX_AMPLIFIER = 2;         // 效果等级上限（显示为 III 级；原版等级 I = amplifier 0）
    public static final int MAX_DURATION_TICKS = 20 * 60 * 30; // 效果时长上限：30 分钟

    private static final String TAG_STORED_EFFECTS = "alone_adventure.stored_effects";

    public injection_template(Properties props) {
        super(props);
    }

    // ── 检测药水中所有效果，写入针剂 NBT ──

    /**
     * 从瓶装药水提取全部效果（含原版药水效果 + 自定义效果）。
     */
    public static List<MobEffectInstance> getEffects(ItemStack potion) {
        List<MobEffectInstance> list = new ArrayList<>();
        // 配方只允许瓶子，使用原版药水效果 API 提取
        List<MobEffectInstance> base = PotionUtils.getMobEffects(potion);
        list.addAll(base);
        return list;
    }

    /**
     * 构建一个针剂：把所有效果时长 × 0.8 写入 NBT。
     */
    public static ItemStack createSyringe(List<MobEffectInstance> effects, int count) {
        return createSyringe(effects, count, DURATION_RATIO);
    }

    /**
     * 构建针剂（自定义时长倍率）。
     * 混合针剂传入 1.0 —— 0.8 倍缩放只在“药水 → 针剂”时应用，混合时不再重复缩放。
     */
    public static ItemStack createSyringe(List<MobEffectInstance> effects, int count, double durationRatio) {
        ItemStack out = new ItemStack(Alone818.com.alone_adventure.init.ModItems.INJECTION_SYRINGE.get(), count);
        ListTag list = new ListTag();
        for (MobEffectInstance eff : effects) {
            // 时长 ≤ 30 分钟，等级 ≤ 3 级（III）
            int newDur = Math.min((int) Math.max(1, eff.getDuration() * durationRatio), MAX_DURATION_TICKS);
            int amplifier = Math.min(eff.getAmplifier(), MAX_AMPLIFIER);
            MobEffectInstance n = new MobEffectInstance(
                    eff.getEffect(), newDur, amplifier,
                    eff.isAmbient(), eff.isVisible(), eff.showIcon());
            list.add(n.save(new CompoundTag()));
        }
        CompoundTag tag = out.getOrCreateTag();
        tag.put(TAG_STORED_EFFECTS, list);
        return out;
    }

    // ── 混合针剂逻辑 ──

    /**
     * 混合两支针剂的效果：不同效果合并；相同效果取更高等级与更长时长
     * （取 max 而非叠加，防止反复混合无限刷时长）。
     */
    public static List<MobEffectInstance> mixEffects(ItemStack syringeA, ItemStack syringeB) {
        Map<MobEffect, MobEffectInstance> merged = new LinkedHashMap<>();
        mergeInto(merged, readEffects(syringeA));
        mergeInto(merged, readEffects(syringeB));
        return new ArrayList<>(merged.values());
    }

    private static void mergeInto(Map<MobEffect, MobEffectInstance> merged, List<MobEffectInstance> effects) {
        for (MobEffectInstance eff : effects) {
            MobEffectInstance old = merged.get(eff.getEffect());
            if (old == null) {
                merged.put(eff.getEffect(), copy(eff));
            } else {
                // 相同效果：等级、时长各取更高的一方
                merged.put(eff.getEffect(), new MobEffectInstance(
                        eff.getEffect(),
                        Math.max(old.getDuration(), eff.getDuration()),
                        Math.max(old.getAmplifier(), eff.getAmplifier()),
                        old.isAmbient(), old.isVisible(), old.showIcon()));
            }
        }
    }

    /**
     * 应用混合材料加成：每个怪物残灰使全部效果等级 +1、时长 ×0.6（多个叠乘）；
     * 血脉碎片按基础时长累加，N 个使时长 ×(1 + 0.6N)。时长最低保留 1 tick。
     */
    public static List<MobEffectInstance> applyCatalysts(List<MobEffectInstance> effects, int ashCount, int bloodCount) {
        double ratio = Math.pow(MIX_ASH_DURATION_RATIO, ashCount) * (1.0 + MIX_BLOOD_DURATION_BONUS * bloodCount);
        List<MobEffectInstance> out = new ArrayList<>();
        for (MobEffectInstance eff : effects) {
            int amplifier = eff.getAmplifier() + ashCount;
            int duration = (int) Math.max(1, eff.getDuration() * ratio);
            out.add(new MobEffectInstance(
                    eff.getEffect(), duration, amplifier,
                    eff.isAmbient(), eff.isVisible(), eff.showIcon()));
        }
        return out;
    }

    private static MobEffectInstance copy(MobEffectInstance eff) {
        return new MobEffectInstance(
                eff.getEffect(), eff.getDuration(), eff.getAmplifier(),
                eff.isAmbient(), eff.isVisible(), eff.showIcon());
    }

    /**
     * 从针剂读取效果列表。
     */
    public static List<MobEffectInstance> readEffects(ItemStack syringe) {
        List<MobEffectInstance> list = new ArrayList<>();
        CompoundTag tag = syringe.getTag();
        if (tag == null) return list;
        if (!tag.contains(TAG_STORED_EFFECTS, Tag.TAG_LIST)) return list;
        ListTag listTag = tag.getList(TAG_STORED_EFFECTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            MobEffectInstance e = MobEffectInstance.load(listTag.getCompound(i));
            if (e != null) list.add(clampEffect(e));
        }
        return list;
    }

    /** 按针剂上限收紧效果：等级 ≤ 3 级（III）、时长 ≤ 30 分钟（旧版本针剂 NBT 读取时同样生效） */
    private static MobEffectInstance clampEffect(MobEffectInstance eff) {
        int amplifier = Math.min(eff.getAmplifier(), MAX_AMPLIFIER);
        int duration = Math.min(eff.getDuration(), MAX_DURATION_TICKS);
        if (amplifier == eff.getAmplifier() && duration == eff.getDuration()) {
            return eff;
        }
        return new MobEffectInstance(
                eff.getEffect(), duration, amplifier,
                eff.isAmbient(), eff.isVisible(), eff.showIcon());
    }

    // ── 使用逻辑：使用时间为药水的 0.8，喝完应用效果并按输出移除 ──

    @Override
    public int getUseDuration(ItemStack stack) {
        return (int) (32 * USE_RATIO); // 药水 32 tick × 0.5 = 16 tick（0.8 秒）
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide) {
            List<MobEffectInstance> effects = readEffects(stack);
            // 猎人血清被动：佩戴时使用针剂，效果等级 +1 且时长与现有同类效果叠加
            boolean serumBoost = living instanceof Player player && hunter_serum.isWearing(player);
            for (MobEffectInstance eff : effects) {
                if (serumBoost) {
                    int amplifier = eff.getAmplifier() + hunter_serum.POTION_LEVEL_BONUS;
                    int duration = eff.getDuration();
                    MobEffectInstance existing = living.getEffect(eff.getEffect());
                    if (existing != null) {
                        // 时长叠加（现有剩余 + 本次），等级不低于已有等级
                        duration += existing.getDuration();
                        amplifier = Math.max(amplifier, existing.getAmplifier());
                    }
                    living.addEffect(new MobEffectInstance(
                            eff.getEffect(), duration, amplifier,
                            eff.isAmbient(), eff.isVisible(), eff.showIcon()));
                } else {
                    living.addEffect(eff);
                }
            }
        }
        // 使用后减少 1
        stack.shrink(1);
        return stack;
    }

    // ── 物品提示 ──

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        List<MobEffectInstance> effects = readEffects(stack);
        if (effects.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.alone_adventure.injection_template.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.injection_template.header")
                    .withStyle(ChatFormatting.GOLD));
            // 显示各效果与时间
            PotionUtils.addPotionTooltip(effects, tooltip, 1.0F);
        }
    }
}