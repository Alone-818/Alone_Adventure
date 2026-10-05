package Alone818.com.alone_adventure.entity.faction;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * 中士 / 将军的自救目标：
 *
 * 血量过低时拿出治疗药水喝下，
 * 回复一定比例的最大生命。
 *
 * 喝药次数存在实体身上（随存档保存）：
 * 中士全场限 1 次，将军限 3 次，
 * 见 FactionSoldier。
 *
 * 喝药期间锁定移动与视角，
 * 手上会显示治疗药水
 * （客户端由 FactionMobRenderer 抬手渲染）。
 */
public class SelfHealDrinkGoal
        extends Goal {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 触吸血线：低于最大生命的该比例 */
    public static final float HEALTH_TRIGGER = 0.3F;

    /** 喝药回复：最大生命百分比 */
    public static final float HEAL_RATIO = 0.5F;

    /** 喝药耗时（与原版药水饮用时间一致） */
    public static final int DRINK_TICKS = 32;

    /** 喝完一次后的最短冷却 */
    public static final long COOLDOWN_TICKS = 100L;

    /** 喝药中途被打断后的重试间隔 */
    public static final long RETRY_TICKS = 40L;

    private final FactionMobEntity mob;

    /** 剩余喝药时间 */
    private int drinkTicks = 0;

    /** 下一次允许触发的时间（gameTime） */
    private long nextAllowedTime = 0L;

    /** 喝药前主手的物品，喝完归还 */
    @Nullable
    private ItemStack savedItem = ItemStack.EMPTY;

    public SelfHealDrinkGoal(FactionMobEntity mob) {

        this.mob = mob;

        // 喝药期间停下移动与攻击
        setFlags(
                EnumSet.of(
                        Goal.Flag.MOVE,
                        Goal.Flag.LOOK
                )
        );
    }

    /**
     * 冷却结束、还有喝药次数、血量过低才激活。
     */
    @Override
    public boolean canUse() {

        if (mob.level().isClientSide) {
            return false;
        }

        if (mob.level().getGameTime()
                < nextAllowedTime) {
            return false;
        }

        if (mob.getSelfHealUsesLeft() <= 0) {
            return false;
        }

        return mob.getHealth()
                < mob.getMaxHealth()
                * HEALTH_TRIGGER;
    }

    /**
     * 开始喝就喝完：
     * 中途不会因为血量变化取消。
     */
    @Override
    public boolean canContinueToUse() {

        return drinkTicks > 0;
    }

    @Override
    public void start() {

        LOGGER.info(
                "[Faction] {} starts drinking (uses left: {})",
                mob.getType().toShortString(),
                mob.getSelfHealUsesLeft()
        );

        drinkTicks = DRINK_TICKS;

        savedItem =
                mob.getItemInHand(
                        InteractionHand.MAIN_HAND
                );

        /*
         * 手上换成治疗药水。
         * 只是展示用，不真正走原版喝药逻辑，
         * 亡灵等派系不会被瞬间治疗反伤。
         */
        mob.setItemInHand(
                InteractionHand.MAIN_HAND,
                PotionUtils.setPotion(
                        new ItemStack(Items.POTION),
                        Potions.STRONG_HEALING
                )
        );

        mob.setDrinkingPotion(true);

        mob.getNavigation().stop();

        mob.playSound(
                SoundEvents.WITCH_DRINK,
                0.8F,
                1.0F
        );
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {

        if (drinkTicks <= 0) {
            return;
        }

        drinkTicks--;

        // 喝药过程音效
        if (drinkTicks > 0
                && drinkTicks % 6 == 0) {

            mob.playSound(
                    SoundEvents.WITCH_DRINK,
                    0.5F,
                    1.1F
            );
        }

        if (drinkTicks == 0) {
            finishDrink();
        }
    }

    @Override
    public void stop() {

        /*
         * 中途被打断：
         * 归还物品，不消耗次数，稍后重试。
         */
        if (drinkTicks > 0) {

            restoreHand();

            nextAllowedTime =
                    mob.level().getGameTime()
                            + RETRY_TICKS;
        }
    }

    /**
     * 喝完：消耗次数、回复生命。
     */
    private void finishDrink() {

        restoreHand();

        mob.consumeSelfHealUse();

        LOGGER.info(
                "[Faction] {} finished drinking, healed +{}",
                mob.getType().toShortString(),
                mob.getMaxHealth() * HEAL_RATIO
        );

        mob.heal(
                mob.getMaxHealth()
                        * HEAL_RATIO
        );

        nextAllowedTime =
                mob.level().getGameTime()
                        + COOLDOWN_TICKS;

        if (mob.level()
                instanceof ServerLevel server) {

            server.sendParticles(
                    ParticleTypes.HEART,
                    mob.getX(),
                    mob.getY()
                            + mob.getBbHeight() * 0.75D,
                    mob.getZ(),
                    6,
                    0.3D,
                    0.2D,
                    0.3D,
                    0.05D
            );
        }

        mob.playSound(
                SoundEvents.WITCH_DRINK,
                0.8F,
                0.9F
        );
    }

    /**
     * 收尾：恢复手部状态。
     */
    private void restoreHand() {

        mob.setDrinkingPotion(false);

        mob.setItemInHand(
                InteractionHand.MAIN_HAND,
                savedItem
        );

        savedItem = ItemStack.EMPTY;

        drinkTicks = 0;
    }
}
