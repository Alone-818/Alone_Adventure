package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.Alone_adventure;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 突击盾
 *
 * 右键：
 * 1. 举起盾牌
 * 2. 向前冲刺
 * 3. 冲刺期间完全免疫伤害
 */
@Mod.EventBusSubscriber(
        modid = Alone_adventure.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class assaultshield extends ShieldItem {

    // =========================================================
    // Config 可调参数
    // =========================================================

    /**
     * 冲刺冷却。
     *
     * 默认：
     * 140 tick = 7 秒
     */
    public static int DASH_COOLDOWN_TICKS = 140;

    /**
     * 单次冲刺距离。
     *
     * 默认 7 格。
     */
    public static double DASH_DISTANCE = 7.0D;

    /**
     * 冲刺持续时间。
     *
     * 默认：
     * 10 tick = 0.5 秒
     */
    public static int DASH_DURATION_TICKS = 10;

    // =========================================================
    // 玩家 NBT
    // =========================================================

    /**
     * 当前剩余冲刺时间。
     */
    private static final String TAG_DASH_TICKS =
            "AloneAdventureAssaultDashTicks";

    /**
     * 冲刺冷却。
     */
    private static final String TAG_DASH_COOLDOWN =
            "AloneAdventureAssaultDashCooldown";

    // =========================================================
    // 构造
    // =========================================================

    public assaultshield() {
        super(
                new Properties()
                        .stacksTo(1)
        );
    }

    // =========================================================
    // 右键
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        /*
         * 右键：
         * 举盾。
         */
        player.startUsingItem(hand);

        /*
         * 只在服务器启动真实冲刺。
         */
        if (!level.isClientSide) {

            CompoundTag data =
                    player.getPersistentData();

            int cooldown =
                    data.getInt(TAG_DASH_COOLDOWN);

            int dashTicks =
                    data.getInt(TAG_DASH_TICKS);

            /*
             * 冷却结束并且当前没有冲刺，
             * 才能启动新的冲刺。
             */
            if (cooldown <= 0 && dashTicks <= 0) {

                data.putInt(
                        TAG_DASH_TICKS,
                        DASH_DURATION_TICKS
                );

                data.putInt(
                        TAG_DASH_COOLDOWN,
                        DASH_COOLDOWN_TICKS
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }

    // =========================================================
    // 使用时间
    // =========================================================

    @Override
    public int getUseDuration(
            ItemStack stack
    ) {
        return 72000;
    }

    // =========================================================
    // 盾牌动画
    // =========================================================

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.BLOCK;
    }

    // =========================================================
    // 玩家 Tick
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player =
                event.player;

        /*
         * 真正的冲刺只在服务器执行。
         */
        if (player.level().isClientSide) {
            return;
        }

        CompoundTag data =
                player.getPersistentData();

        // =====================================================
        // 冷却
        // =====================================================

        int cooldown =
                data.getInt(TAG_DASH_COOLDOWN);

        if (cooldown > 0) {

            data.putInt(
                    TAG_DASH_COOLDOWN,
                    cooldown - 1
            );
        }

        // =====================================================
        // 冲刺时间
        // =====================================================

        int dashTicks =
                data.getInt(TAG_DASH_TICKS);

        if (dashTicks <= 0) {
            return;
        }

        // =====================================================
        // 检查玩家是否还在右键
        // =====================================================

        boolean usingShield =
                player.isUsingItem()
                        && player.getUseItem().getItem()
                        instanceof assaultshield;

        /*
         * 松开右键：
         * 立即结束冲刺。
         */
        if (!usingShield) {

            data.putInt(
                    TAG_DASH_TICKS,
                    0
            );

            return;
        }

        // =====================================================
        // 计算每 tick 冲刺速度
        // =====================================================

        double dashSpeed =
                DASH_DISTANCE
                        / Math.max(
                        1,
                        DASH_DURATION_TICKS
                );

        // =====================================================
        // 根据玩家水平朝向计算方向
        // =====================================================

        float yaw =
                player.getYRot();

        double x =
                -Mth.sin(
                        yaw * ((float) Math.PI / 180F)
                );

        double z =
                Mth.cos(
                        yaw * ((float) Math.PI / 180F)
                );

        Vec3 direction =
                new Vec3(
                        x,
                        0.0D,
                        z
                ).normalize();

        // =====================================================
        // 设置冲刺速度
        // =====================================================

        Vec3 current =
                player.getDeltaMovement();

        player.setDeltaMovement(
                direction.x * dashSpeed,
                current.y,
                direction.z * dashSpeed
        );

        player.hurtMarked = true;
        player.hasImpulse = true;

        /*
         * 冲刺期间保持奔跑动作。
         */
        player.setSprinting(true);

        // =====================================================
        // 冲刺计时
        // =====================================================

        dashTicks--;

        data.putInt(
                TAG_DASH_TICKS,
                dashTicks
        );
    }

    // =========================================================
    // 无敌
    // =========================================================

    /**
     * 拦截攻击伤害。
     */
    @SubscribeEvent
    public static void onLivingAttack(
            LivingAttackEvent event
    ) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isDashing(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * 拦截实际伤害。
     */
    @SubscribeEvent
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isDashing(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * 冲刺期间取消摔落伤害。
     */
    @SubscribeEvent
    public static void onLivingFall(
            LivingFallEvent event
    ) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isDashing(player)) {
            event.setCanceled(true);
        }
    }

    // =========================================================
    // 判断是否正在冲刺
    // =========================================================

    private static boolean isDashing(
            Player player
    ) {

        CompoundTag data =
                player.getPersistentData();

        return data.getInt(
                TAG_DASH_TICKS
        ) > 0;
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        if (Screen.hasShiftDown()) {

            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.assaultshield.tooltip.desc"
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );

            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.assaultshield.tooltip.dash"
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );

            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.assaultshield.tooltip.invincible"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );

        } else {

            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.assaultshield.tooltip.desc"
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );

            tooltip.add(
                    Component.translatable(
                            "tooltip.alone_adventure.press_shift"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY,
                            ChatFormatting.ITALIC
                    )
            );
        }
    }
}