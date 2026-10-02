package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.state.BanruoState;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class banruo_soup extends Item {

    public banruo_soup() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).food(
                new FoodProperties.Builder()
                        .nutrition(0)
                        .saturationMod(0.0F)
                        .alwaysEat()
                        .build()
        ));
    }


    // 饮用时间：0.8秒
    private static final int DRINK_DURATION = 16;

    // 冷却：15秒
    private static final int COOLDOWN = 20 * 15;

    // 效果：60秒
    private static final int EFFECT_DURATION = 20 * 60;

    public banruo_soup(Properties properties) {
        super(
                properties.food(
                        new FoodProperties.Builder()
                                .nutrition(0)
                                .saturationMod(0.0F)
                                .alwaysEat()
                                .build()
                )
        );
    }

    /**
     * 饮用时间
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    /**
     * 原版喝东西动画
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    /**
     * 开始饮用
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        // 15秒冷却
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    /**
     * 饮用完成
     */
    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ) {
        if (!level.isClientSide && entity instanceof Player player) {

            long currentGameTime = level.getGameTime();

            /*
             * 如果之前的般若汤状态已经结束，
             * 从第一口重新计算。
             */
            if (BanruoState.isExpired(player, currentGameTime)) {
                BanruoState.clear(player);
            }

            /*
             * 增加般若汤层数。
             *
             * 第3口开始自动进入爆回。
             */
            BanruoState.drink(
                    player,
                    currentGameTime
            );

            int levelNumber = BanruoState.getLevel(player);

            /*
             * 反胃 I
             */
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION,
                    EFFECT_DURATION,
                    0,
                    false,
                    true,
                    true
            ));

            /*
             * 力量
             *
             * level 1 -> 力量 I
             * level 2 -> 力量 II
             * level 3 -> 力量 III
             * level 4 -> 力量 IV
             */
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    EFFECT_DURATION,
                    levelNumber - 1,
                    false,
                    true,
                    true
            ));

            /*
             * 急迫最高 II
             *
             * 第1口 -> I
             * 第2口 -> II
             * 第3口以后 -> II
             */
            int hasteLevel = Math.min(levelNumber, 2);

            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED,
                    EFFECT_DURATION,
                    hasteLevel - 1,
                    false,
                    true,
                    true
            ));

            /*
             * 15秒冷却
             */
            player.getCooldowns().addCooldown(
                    this,
                    COOLDOWN
            );
        }

        /*
         * 不调用 super.finishUsingItem()
         *
         * 所以般若汤不会消耗。
         */
        return stack;
    }

    /**
     * 物品提示
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.banruo_soup.tooltip.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
