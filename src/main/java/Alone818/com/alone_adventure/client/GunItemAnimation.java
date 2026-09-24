package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import net.minecraftforge.registries.ForgeRegistries;

import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class GunItemAnimation {

    public static final String FIRE = "fire";
    public static final String RELOAD = "reload";
    public static final String AIM = "aim";
    public static final String RUN = "run";
    public static final String IDLE = "idle";

    private static final float FIRE_TAIL_TICKS = 2.0F;

    /*
     * Reload 开始时间
     */
    private static long reloadAt = -1L;

    /*
     * 已经处理过的 Reload
     */
    private static long handledReloadAt = -1L;

    /*
     * 已经处理过的 Fire
     */
    private static long handledFireAt = -1L;

    private GunItemAnimation() {
    }

    // =========================================================
    // Reload Timestamp
    // =========================================================

    public static void markReload(long tick) {

        reloadAt = tick;
    }

    // =========================================================
    // Main Animation Controller
    // =========================================================

    public static <T extends GunItem> PlayState handle(
            AnimationState<T> state
    ) {

        /*
         * 不使用 DataTickets.CLIENT_PLAYER。
         *
         * 直接获取 Minecraft 当前客户端玩家。
         * 这样兼容你当前 GeckoLib 版本。
         */

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null) {
            return PlayState.STOP;
        }

        /*
         * 当前 GunItem
         */

        GunItem gun =
                state.getAnimatable();

        /*
         * Registry ID
         */

        ResourceLocation id =
                ForgeRegistries.ITEMS.getKey(gun);

        if (id == null) {
            return PlayState.STOP;
        }

        String itemId =
                id.getPath();

        /*
         * 当前 tick + partial tick
         */

        float now =
                player.tickCount
                        + state.getPartialTick();

        // =====================================================
        // RELOAD
        // =====================================================

        Animation reloadAnimation =
                GunGeoModel.resolveAnimation(
                        itemId,
                        RELOAD
                );

        if (reloadAt >= 0
                && reloadAnimation != null) {

            float elapsed =
                    now - reloadAt;

            /*
             * Blockbench：
             *
             * reload = 1.5 秒
             *
             * 1 秒 = 20 tick
             * 1.5 秒 = 30 tick
             *
             * 留 2 tick 缓冲。
             */

            if (elapsed >= 0
                    && elapsed <= 32.0F) {

                /*
                 * 新的一次 reload。
                 *
                 * 使用 controller.forceAnimationReset()
                 * 而不是你当前 GeckoLib 不支持的
                 * state.resetCurrentAnimation()。
                 */

                if (handledReloadAt != reloadAt) {

                    handledReloadAt =
                            reloadAt;

                    state.getController()
                            .forceAnimationReset();
                }

                return state.setAndContinue(
                        RawAnimation.begin()
                                .thenPlay(RELOAD)
                );
            }
        }

        // =====================================================
        // FIRE
        // =====================================================
        long mainFireAt =
                ClientGunHandler.getLastFireAt(
                        InteractionHand.MAIN_HAND
                );

        long offFireAt =
                ClientGunHandler.getLastFireAt(
                        InteractionHand.OFF_HAND
                );

        long fireAt =
                Math.max(mainFireAt, offFireAt);
        Animation fireAnimation =
                GunGeoModel.resolveAnimation(
                        itemId,
                        FIRE
                );

        if (fireAt >= 0
                && fireAnimation != null) {

            float elapsed =
                    now - fireAt;

            /*
             * Blockbench：
             *
             * fire = 0.5 秒
             * = 10 tick
             *
             * 再加 2 tick 尾部。
             */

            if (elapsed >= 0
                    && elapsed <=
                    10.0F + FIRE_TAIL_TICKS) {

                if (handledFireAt != fireAt) {

                    handledFireAt =
                            fireAt;

                    state.getController()
                            .forceAnimationReset();
                }

                return state.setAndContinue(
                        RawAnimation.begin()
                                .thenPlay(FIRE)
                );
            }
        }

        // =====================================================
        // AIM
        // =====================================================

        if (player.isUsingItem()
                && player.getUseItem()
                .getItem() instanceof GunItem) {

            Animation aimAnimation =
                    GunGeoModel.resolveAnimation(
                            itemId,
                            AIM
                    );

            if (aimAnimation != null) {

                return state.setAndContinue(
                        RawAnimation.begin()
                                .thenLoop(AIM)
                );
            }
        }

        // =====================================================
        // RUN
        // =====================================================

        if (player.isSprinting()) {

            Animation runAnimation =
                    GunGeoModel.resolveAnimation(
                            itemId,
                            RUN
                    );

            if (runAnimation != null) {

                return state.setAndContinue(
                        RawAnimation.begin()
                                .thenLoop(RUN)
                );
            }
        }

        // =====================================================
        // IDLE
        // =====================================================

        Animation idleAnimation =
                GunGeoModel.resolveAnimation(
                        itemId,
                        IDLE
                );

        if (idleAnimation != null) {

            return state.setAndContinue(
                    RawAnimation.begin()
                            .thenLoop(IDLE)
            );
        }

        // =====================================================
        // STOP
        // =====================================================

        return PlayState.STOP;
    }

    // =========================================================
    // Dual Wield
    // =========================================================

    public static boolean isDualWield(
            ItemStack stack
    ) {

        if (stack == null
                || stack.isEmpty()) {

            return false;
        }

        if (!(stack.getItem()
                instanceof GunItem gun)) {

            return false;
        }

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null) {
            return false;
        }

        ItemStack main =
                player.getMainHandItem();

        ItemStack off =
                player.getOffhandItem();

        return main.getItem() instanceof GunItem
                && off.getItem() instanceof GunItem
                && main.getItem() == gun
                && off.getItem() == gun;
    }
}