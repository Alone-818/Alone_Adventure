package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunStats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 枪械动画辅助类。
 *
 * 重要：
 *
 * FIRE 不再通过 ClientGunHandler 的：
 *
 * mainFireAt
 * offFireAt
 *
 * 判断。
 *
 * 真正成功开火后，由 GunItem.tryFire()
 * 直接对“当前 ItemStack 的 GeckoLib 实例”
 * 触发 fire。
 */
public final class GunItemAnimation {

    public static final String FIRE = "fire";
    public static final String RELOAD = "reload";
    public static final String AIM = "aim";
    public static final String RUN = "run";
    public static final String IDLE = "idle";

    private static final int RELOAD_ANIMATION_TICKS = 32;

    private static int mainReloadUntil = -1;
    private static int offReloadUntil = -1;

    private GunItemAnimation() {
    }

    // =========================================================
    // Reload
    // =========================================================

    /**
     * 标记主手 / 副手开始 reload。
     *
     * 这里只作为客户端提前显示 reload 的辅助状态。
     *
     * 真正的 reload 状态仍然以 ItemStack NBT 为准。
     */
    public static void markReload(
            int tick,
            InteractionHand hand
    ) {

        int until =
                tick + RELOAD_ANIMATION_TICKS;

        if (hand == InteractionHand.MAIN_HAND) {

            mainReloadUntil = until;

        } else {

            offReloadUntil = until;
        }
    }

    /**
     * 兼容旧代码。
     *
     * 默认主手。
     */
    public static void markReload(
            int tick
    ) {

        markReload(
                tick,
                InteractionHand.MAIN_HAND
        );
    }

    /**
     * 取消指定手的客户端 reload 标记。
     */
    public static void cancelReload(
            InteractionHand hand
    ) {

        if (hand == InteractionHand.MAIN_HAND) {

            mainReloadUntil = -1;

        } else {

            offReloadUntil = -1;
        }
    }

    // =========================================================
    // GeckoLib Controller
    // =========================================================

    /**
     * 普通行为 Controller。
     *
     * 这里负责：
     *
     * reload
     * aim
     * run
     * idle
     *
     * FIRE 不在这里判断。
     *
     * FIRE 是 GeckoLib triggerable animation，
     * 由 GunItem.tryFire() 在服务器确认成功开火后直接触发。
     */
    public static <T extends GunItem> PlayState handle(
            AnimationState<T> state
    ) {

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null) {
            return PlayState.STOP;
        }

        T gun =
                state.getAnimatable();

        if (gun == null) {
            return PlayState.STOP;
        }

        ResourceLocation itemId =
                ForgeRegistries.ITEMS.getKey(gun);

        if (itemId == null) {
            return PlayState.STOP;
        }

        String gunId =
                itemId.getPath();

        // =====================================================
        // 找到当前这个 GunItem 对应的 ItemStack
        // =====================================================

        ItemStack mainStack =
                player.getMainHandItem();

        ItemStack offStack =
                player.getOffhandItem();

        boolean mainMatch =
                mainStack.getItem() == gun;

        boolean offMatch =
                offStack.getItem() == gun;

        if (!mainMatch && !offMatch) {
            return PlayState.STOP;
        }

        // =====================================================
        // Reload
        // =====================================================

        boolean mainReload =
                mainMatch
                        && isReloading(
                        InteractionHand.MAIN_HAND,
                        mainStack,
                        player.tickCount
                );

        boolean offReload =
                offMatch
                        && isReloading(
                        InteractionHand.OFF_HAND,
                        offStack,
                        player.tickCount
                );

        if (mainReload || offReload) {

            if (GunGeoModel.resolveAnimation(
                    gunId,
                    RELOAD
            ) != null) {

                state.getController().setAnimation(
                        RawAnimation.begin()
                                .thenPlay(RELOAD)
                );

                return PlayState.CONTINUE;
            }
        }

        // =====================================================
        // AIM
        // =====================================================

        boolean aiming =
                player.isUsingItem()
                        && player.getUseItem()
                        .getItem() == gun;

        if (aiming) {

            if (GunGeoModel.resolveAnimation(
                    gunId,
                    AIM
            ) != null) {

                state.getController().setAnimation(
                        RawAnimation.begin()
                                .thenPlay(AIM)
                );

                return PlayState.CONTINUE;
            }
        }

        // =====================================================
        // RUN
        // =====================================================

        if (player.isSprinting()) {

            if (GunGeoModel.resolveAnimation(
                    gunId,
                    RUN
            ) != null) {

                state.getController().setAnimation(
                        RawAnimation.begin()
                                .thenLoop(RUN)
                );

                return PlayState.CONTINUE;
            }
        }

        // =====================================================
        // IDLE
        // =====================================================

        if (GunGeoModel.resolveAnimation(
                gunId,
                IDLE
        ) != null) {

            state.getController().setAnimation(
                    RawAnimation.begin()
                            .thenLoop(IDLE)
            );

            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    // =========================================================
    // Reload 状态
    // =========================================================

    private static boolean isReloading(
            InteractionHand hand,
            ItemStack stack,
            int currentTick
    ) {

        boolean marked;

        if (hand == InteractionHand.MAIN_HAND) {

            marked =
                    currentTick <= mainReloadUntil;

        } else {

            marked =
                    currentTick <= offReloadUntil;
        }

        if (marked) {
            return true;
        }

        /*
         * 服务器同步过来的真正 reload 状态。
         */
        if (stack.hasTag()
                && stack.getTag().contains(
                GunItem.TAG_RELOAD_START
        )) {

            return true;
        }

        return false;
    }

    // =========================================================
    // 双持
    // =========================================================

    public static boolean isDualWield(
            ItemStack stack
    ) {

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null) {
            return false;
        }

        ItemStack main =
                player.getMainHandItem();

        ItemStack off =
                player.getOffhandItem();

        if (!(main.getItem()
                instanceof GunItem mainGun)) {

            return false;
        }

        if (!(off.getItem()
                instanceof GunItem offGun)) {

            return false;
        }

        return mainGun.getStats()
                .handedness()
                == GunStats.Handedness.ONE_HANDED

                && offGun.getStats()
                .handedness()
                == GunStats.Handedness.ONE_HANDED;
    }
}