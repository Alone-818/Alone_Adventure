package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunStats;
import Alone818.com.alone_adventure.network.GunFirePacket;
import Alone818.com.alone_adventure.network.GunReloadPacket;
import Alone818.com.alone_adventure.client.screen.AmmoWheelScreen;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 枪械客户端事件处理。
 *
 * 注意：
 *
 * ClientGunHandler 只负责：
 *
 * - 左键输入
 * - 双持开火排程
 * - 半自动 / 全自动
 * - 后坐力
 * - reload / ammo switch 按键
 *
 * 不负责 GeckoLib fire 动画。
 *
 * fire 动画由 GunItem.tryFire() 在服务器真正成功
 * 生成子弹之后，对具体 ItemStack 直接触发。
 */
@Mod.EventBusSubscriber(
        modid = Alone_adventure.MODID,
        value = Dist.CLIENT
)
public final class ClientGunHandler {

    // =========================================================
    // 后坐力
    // =========================================================

    private static final float RECOIL_APPLY_RATE =
            0.6F;

    private static final float RECOIL_MIN_STEP =
            0.05F;

    private static float recoilPending =
            0.0F;

    // =========================================================
    // 左键
    // =========================================================

    private static boolean attackWasDown =
            false;

    // =========================================================
    // 双持排程
    // =========================================================

    private static InteractionHand nextFireHand =
            InteractionHand.MAIN_HAND;

    private static long mainReadyAt =
            0L;

    private static long offhandReadyAt =
            0L;

    private static long nextShotAt =
            0L;
    // =========================================================
// 弹药轮盘状态
// =========================================================

    private static boolean ammoWheelOpened = false;

    /** 当前弹药轮盘实例。仅作为 HUD 控制器，不设置 Minecraft Screen。 */
    private static AmmoWheelScreen ammoWheel = null;

    private ClientGunHandler() {
    }

    // =========================================================
    // 后坐力
    // =========================================================

    /**
     * 服务端 GunRecoilPacket 调用。
     */
    public static void enqueueRecoil(
            float degrees
    ) {

        recoilPending +=
                degrees;
    }

    // =========================================================
    // 左键：取消原版攻击
    // =========================================================

    @SubscribeEvent
    public static void onInteractionKey(
            InputEvent.InteractionKeyMappingTriggered event
    ) {

        if (!event.isAttack()) {
            return;
        }

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null
                || player.isSpectator()) {

            return;
        }

        ItemStack main =
                player.getMainHandItem();

        if (main.getItem()
                instanceof GunItem) {

            event.setCanceled(true);
        }
    }

    // =========================================================
    // R / G
    // =========================================================
    @SubscribeEvent
    public static void onKeyInput(
            InputEvent.Key event
    ) {


        Minecraft mc =
                Minecraft.getInstance();



        // =====================================================
        // R：reload
        // =====================================================

        if (ModClientSetup.GUN_RELOAD_KEY.consumeClick()) {


            LocalPlayer player =
                    mc.player;


            if (player != null
                    && !player.isSpectator()
                    && (
                    player.getMainHandItem()
                            .getItem()
                            instanceof GunItem

                            ||

                            player.getOffhandItem()
                                    .getItem()
                                    instanceof GunItem
            )) {


                GunItemAnimation.markReload(
                        player.tickCount,
                        InteractionHand.MAIN_HAND
                );


                Alone_adventure.NETWORK.sendToServer(
                        new GunReloadPacket()
                );
            }
        }
    }

    // =========================================================
    // Client Tick
    // =========================================================

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {

        if (event.phase
                != TickEvent.Phase.END) {

            return;
        }

        Minecraft mc =
                Minecraft.getInstance();

        LocalPlayer player =
                mc.player;
        // =====================================================
// =====================================================
        // G：打开 / 保持 / 松开弹药轮盘
        // =====================================================

        boolean ammoWheelKeyDown =
                ModClientSetup.GUN_SWITCH_AMMO_KEY.isDown();

        // 按住 G：第一次打开轮盘。这里不调用 mc.setScreen()，
        // 因而 WASD / Space / Shift 等键盘操作仍然正常。
        if (player != null
                && !player.isSpectator()
                && mc.screen == null
                && ammoWheelKeyDown
                && !ammoWheelOpened) {

            ItemStack stack =
                    player.getMainHandItem();

            if (stack.getItem() instanceof GunItem gun
                    && gun.getStats(stack).ammoTypeCount() > 1) {

                ammoWheel =
                        new AmmoWheelScreen(stack);

                ammoWheelOpened = true;

                // 鼠标释放后只控制轮盘，不再旋转第一人称视角。
                mc.mouseHandler.releaseMouse();
            }
        }

        // 轮盘打开期间持续更新鼠标所在圆环扇区。
        if (ammoWheelOpened
                && ammoWheel != null
                && player != null) {

            ammoWheel.tick();
        }

        // G 松开：确认选择并关闭轮盘。
        if (ammoWheelOpened
                && !ammoWheelKeyDown) {

            if (ammoWheel != null) {
                ammoWheel.confirm();
            }

            ammoWheel = null;
            ammoWheelOpened = false;

            // 恢复正常第一人称鼠标。
            if (mc.screen == null) {
                mc.mouseHandler.grabMouse();
            }
        }
// =====================================================
        // 玩家不存在
        // =====================================================

        if (player == null) {

            recoilPending =
                    0.0F;

            attackWasDown =
                    false;

            nextFireHand =
                    InteractionHand.MAIN_HAND;

            mainReadyAt =
                    0L;

            offhandReadyAt =
                    0L;

            nextShotAt =
                    0L;

            ammoWheel = null;
            ammoWheelOpened = false;

            if (mc.screen == null) {
                mc.mouseHandler.grabMouse();
            }

            return;
        }

        // =====================================================
        // 后坐力
        // =====================================================

        if (recoilPending > 0.005F) {

            float step =
                    Math.min(
                            Math.max(
                                    recoilPending
                                            * RECOIL_APPLY_RATE,
                                    RECOIL_MIN_STEP
                            ),
                            recoilPending
                    );

            float oldPitch =
                    player.getXRot();

            player.setXRot(
                    Mth.clamp(
                            oldPitch - step,
                            -90.0F,
                            90.0F
                    )
            );

            float applied =
                    oldPitch
                            - player.getXRot();

            recoilPending -=
                    Math.min(
                            step,
                            Math.max(
                                    0.0F,
                                    applied
                            )
                    );
        }

        // =====================================================
        // 左键
        // =====================================================

        boolean attackDown =
                !ammoWheelOpened
                        && mc.screen == null
                        && mc.options.keyAttack.isDown();

        // =====================================================
        // 主手
        // =====================================================

        GunItem mainGunItem =
                player.getMainHandItem()
                        .getItem()
                        instanceof GunItem mainGun
                        ? mainGun
                        : null;

        // =====================================================
        // 副手
        // =====================================================

        GunItem offGunItem =
                player.getOffhandItem()
                        .getItem()
                        instanceof GunItem offGun
                        ? offGun
                        : null;

        boolean mainGun =
                mainGunItem != null;

        // =====================================================
        // 双持
        // =====================================================

        boolean dualWielding =
                !player.isSpectator()
                        && mainGun
                        && offGunItem != null
                        && mainGunItem.getStats()
                        .handedness()
                        == GunStats.Handedness.ONE_HANDED
                        && GunItem.isUsableInHand(
                        offGunItem,
                        InteractionHand.OFF_HAND
                );

        // =====================================================
        // 双持开火
        // =====================================================

        if (dualWielding
                && attackDown) {

            long now =
                    player.tickCount;

            long readyAt =
                    nextFireHand
                            == InteractionHand.MAIN_HAND
                            ? mainReadyAt
                            : offhandReadyAt;

            if (now >= Math.max(
                    nextShotAt,
                    readyAt
            )) {

                fireDualShot(
                        player,
                        nextFireHand,
                        now
                );
            }

        }

        // =====================================================
        // 单持
        // =====================================================

        else if (!dualWielding
                && attackDown
                && mainGunItem != null) {

            GunStats.FireMode fireMode =
                    mainGunItem.getStats()
                            .fireMode();

            // =================================================
            // 全自动
            // =================================================

            if (fireMode
                    == GunStats.FireMode.FULL_AUTO) {

                long now =
                        player.tickCount;

                int interval =
                        Math.max(
                                1,
                                mainGunItem
                                        .getStats()
                                        .fireRateTicks()
                        );

                if (now >= mainReadyAt) {

                    Alone_adventure.NETWORK.sendToServer(
                            new GunFirePacket(
                                    InteractionHand.MAIN_HAND
                            )
                    );

                    mainReadyAt =
                            now + interval;
                }
            }

            // =================================================
            // 半自动
            // =================================================

            else if (!attackWasDown) {

                long now =
                        player.tickCount;

                Alone_adventure.NETWORK.sendToServer(
                        new GunFirePacket(
                                InteractionHand.MAIN_HAND
                        )
                );

                mainReadyAt =
                        now + Math.max(
                                1,
                                mainGunItem
                                        .getStats()
                                        .fireRateTicks()
                        );
            }
        }

        // =====================================================
        // 松开左键 / 结束双持
        // =====================================================

        if (!dualWielding
                || !attackDown) {

            nextFireHand =
                    InteractionHand.MAIN_HAND;

            mainReadyAt =
                    0L;

            offhandReadyAt =
                    0L;

            nextShotAt =
                    0L;
        }

        // =====================================================
        // 保存左键状态
        // =====================================================

        attackWasDown =
                attackDown;
    }

    // =========================================================
    // 双持交替开火
    // =========================================================

    private static void fireDualShot(
            LocalPlayer player,
            InteractionHand hand,
            long now
    ) {

        ItemStack stack =
                hand == InteractionHand.MAIN_HAND
                        ? player.getMainHandItem()
                        : player.getOffhandItem();

        if (!(stack.getItem()
                instanceof GunItem gun)) {

            return;
        }

        // =====================================================
        // 发送真正的开火请求
        // =====================================================

        Alone_adventure.NETWORK.sendToServer(
                new GunFirePacket(
                        hand
                )
        );

        // =====================================================
        // 射速
        // =====================================================

        int interval =
                Math.max(
                        1,
                        gun.getStats()
                                .fireRateTicks()
                );

        // =====================================================
        // 当前手下一次可以开火
        // =====================================================

        if (hand
                == InteractionHand.MAIN_HAND) {

            mainReadyAt =
                    now + interval;

        } else {

            offhandReadyAt =
                    now + interval;
        }

        // =====================================================
        // 双持交替间隔
        // =====================================================

        nextShotAt =
                now + Math.max(
                        1,
                        interval / 2
                );

        // =====================================================
        // 切换下一只手
        // =====================================================

        nextFireHand =
                hand == InteractionHand.MAIN_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;
    }

    // =========================================================
    // 弹药轮盘 HUD 绘制
    // =========================================================

    @SubscribeEvent
    public static void onRenderGuiOverlay(
            RenderGuiOverlayEvent.Post event
    ) {

        if (!ammoWheelOpened || ammoWheel == null) {
            return;
        }

        // 每帧只绘制一次。选择 HOTBAR 作为稳定的 HUD 绘制点。
        if (!event.getOverlay().id()
                .equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }

        Minecraft mc =
                Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        ammoWheel.renderWheel(
                event.getGuiGraphics()
        );
    }

    // =========================================================
    // FOV
    // =========================================================

    @SubscribeEvent
    public static void onComputeFov(
            ComputeFovModifierEvent event
    ) {

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null
                || !player.isUsingItem()) {

            return;
        }

        if (player.getUseItem()
                .getItem()
                instanceof GunItem gun) {

            float zoom =
                    Math.max(
                            1.0F,
                            gun.getStats()
                                    .aimZoom()
                    );

            event.setNewFovModifier(
                    event.getNewFovModifier()
                            / zoom
            );
        }
    }
}