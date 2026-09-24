package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;

import Alone818.com.alone_adventure.Items.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.registries.ForgeRegistries;

import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 枪械 GeckoLib 动画控制器。
 *
 * 动画：
 * fire   - 开火
 * reload - 装填
 * aim    - 瞄准
 * run    - 奔跑
 * idle   - 待机
 *
 * fire 使用 GeckoLib Triggerable Animation。
 * reload 支持 ClientGunHandler.markReload() 本地触发。
 */
public final class GunItemAnimation {

    // =========================================================
    // 动画名称
    // =========================================================

    public static final String FIRE = "fire";
    public static final String RELOAD = "reload";
    public static final String AIM = "aim";
    public static final String RUN = "run";
    public static final String IDLE = "idle";

    // =========================================================
    // 动画时间
    // =========================================================

    /**
     * reload 动画最长持续时间。
     *
     * Blockbench 中你的 reload 是 1.5 秒：
     *
     * 1.5 × 20 = 30 tick
     *
     * 留 2 tick 余量。
     */
    private static final int RELOAD_ANIMATION_TICKS = 32;

    /**
     * ClientGunHandler 按 R 时，
     * 会调用：
     *
     * GunItemAnimation.markReload(player.tickCount);
     *
     * 这里记录客户端本地的 reload 动画结束时间。
     */
    private static int reloadMarkedUntil = -1;

    private GunItemAnimation() {
    }

    // =========================================================
    // Reload 本地标记
    // =========================================================

    /**
     * 客户端开始装填时调用。
     *
     * 这个方法是给 ClientGunHandler 使用的。
     */
    public static void markReload(int tick) {

        reloadMarkedUntil =
                tick + RELOAD_ANIMATION_TICKS;
    }

    // =========================================================
    // GeckoLib 动画主处理
    // =========================================================

    public static <T extends GunItem> PlayState handle(
            AnimationState<T> state
    ) {

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null) {
            return PlayState.STOP;
        }

        // =====================================================
        // 找当前玩家正在显示的枪
        // =====================================================

        ItemStack stack =
                getCurrentGunStack(player);

        if (stack.isEmpty()
                || !(stack.getItem() instanceof GunItem gun)) {

            return PlayState.STOP;
        }

        // =====================================================
        // 获取枪械注册名
        // =====================================================

        ResourceLocation itemId =
                ForgeRegistries.ITEMS.getKey(gun);

        if (itemId == null) {
            return PlayState.STOP;
        }

        String gunId =
                itemId.getPath();

        // =====================================================
        // Reload
        // =====================================================

        /*
         * Reload 有两种检测方式：
         *
         * 1. ClientGunHandler.markReload()
         * 2. 服务端同步到 ItemStack 的 TAG_RELOAD_START
         *
         * 这样按 R 后不会因为服务器同步稍慢而出现动画延迟。
         */
        boolean reloading =
                player.tickCount <= reloadMarkedUntil;

        CompoundTag tag =
                stack.getTag();

        if (tag != null
                && tag.contains(GunItem.TAG_RELOAD_START)) {

            reloading = true;
        }

        if (reloading) {

            Animation reload =
                    GunGeoModel.resolveAnimation(
                            gunId,
                            RELOAD
                    );

            if (reload != null) {

                state.getController()
                        .setAnimation(
                                RawAnimation.begin()
                                        .thenPlay(RELOAD)
                        );

                return PlayState.CONTINUE;
            }
        }

        // =====================================================
        // Aim
        // =====================================================

        boolean aiming =
                player.isUsingItem()
                        && player.getUseItem().getItem()
                        instanceof GunItem;

        if (aiming) {

            Animation aim =
                    GunGeoModel.resolveAnimation(
                            gunId,
                            AIM
                    );

            if (aim != null) {

                state.getController()
                        .setAnimation(
                                RawAnimation.begin()
                                        .thenLoop(AIM)
                        );

                return PlayState.CONTINUE;
            }
        }

        // =====================================================
        // Run
        // =====================================================

        if (player.isSprinting()) {

            Animation run =
                    GunGeoModel.resolveAnimation(
                            gunId,
                            RUN
                    );

            if (run != null) {

                state.getController()
                        .setAnimation(
                                RawAnimation.begin()
                                        .thenLoop(RUN)
                        );

                return PlayState.CONTINUE;
            }
        }

        // =====================================================
        // Idle
        // =====================================================

        Animation idle =
                GunGeoModel.resolveAnimation(
                        gunId,
                        IDLE
                );

        if (idle != null) {

            state.getController()
                    .setAnimation(
                            RawAnimation.begin()
                                    .thenLoop(IDLE)
                    );

            return PlayState.CONTINUE;
        }

        // =====================================================
        // 没有动画
        // =====================================================

        return PlayState.STOP;
    }

    // =========================================================
    // 获取当前枪械
    // =========================================================

    private static ItemStack getCurrentGunStack(
            LocalPlayer player
    ) {

        /*
         * 主手优先。
         */
        ItemStack main =
                player.getMainHandItem();

        if (main.getItem() instanceof GunItem) {
            return main;
        }

        /*
         * 主手不是枪时检查副手。
         */
        ItemStack off =
                player.getOffhandItem();

        if (off.getItem() instanceof GunItem) {
            return off;
        }

        return ItemStack.EMPTY;
    }

    // =========================================================
    // 双持判断
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

        /*
         * 主手和副手都必须是枪。
         */
        if (!(main.getItem() instanceof GunItem mainGun)) {
            return false;
        }

        if (!(off.getItem() instanceof GunItem offGun)) {
            return false;
        }

        /*
         * 两把枪都必须是单手枪。
         *
         * ONE_HANDED = 可以双持
         * TWO_HANDED = 不能双持
         */
        return mainGun.getStats().handedness()
                == GunStats.Handedness.ONE_HANDED
                && offGun.getStats().handedness()
                == GunStats.Handedness.ONE_HANDED;
    }}