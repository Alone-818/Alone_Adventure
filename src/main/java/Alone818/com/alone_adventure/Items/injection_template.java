package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import java.util.List;

/**
 * 通用药水针剂模板
 *
 * 合成配方：瓶装药水 + 萤石粉 + 2 个空针管 → 2 个对应针剂
 * 针剂效果持续时间 = 原药水效果的 0.8 倍
 * 针剂使用时间 = 药水使用时间的 0.5 倍（药水 32 tick，针剂 16 tick）
 *
 * 所有瓶装药水（含混制、喷溅、滞留）都能检测，效果存储在针剂的 CustomPotionEffects 中。
 */
public class injection_template extends Item {

    public static final double DURATION_RATIO = 0.8;   // 效果时长倍率
    public static final double USE_RATIO = 0.5;        // 使用时间倍率

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
        ItemStack out = new ItemStack(Alone818.com.alone_adventure.init.ModItems.INJECTION_SYRINGE.get(), count);
        ListTag list = new ListTag();
        for (MobEffectInstance eff : effects) {
            int newDur = (int) Math.max(1, eff.getDuration() * DURATION_RATIO);
            MobEffectInstance n = new MobEffectInstance(
                    eff.getEffect(), newDur, eff.getAmplifier(),
                    eff.isAmbient(), eff.isVisible(), eff.showIcon());
            list.add(n.save(new CompoundTag()));
        }
        CompoundTag tag = out.getOrCreateTag();
        tag.put(TAG_STORED_EFFECTS, list);
        return out;
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
            if (e != null) list.add(e);
        }
        return list;
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
            for (MobEffectInstance eff : effects) {
                living.addEffect(eff);
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