package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunStats;
import Alone818.com.alone_adventure.network.GunFirePacket;
import Alone818.com.alone_adventure.network.GunReloadPacket;
import Alone818.com.alone_adventure.network.GunSwitchAmmoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 枪械客户端事件处理。
 *
 * 设计原则：
 *
 * 1. NBT 与动画状态完全解耦。
 * 2. 全自动严格按照 fireRateTicks 开火。
 * 3. 半自动只在左键按下边沿开火。
 * 4. reload 是一次性客户端动画事件。
 * 5. fire 是一次性客户端动画事件。
 * 6. 不调用 player.swing()，避免原版挥手覆盖 GeckoLib。
 * 7. ItemStack NBT 更新不会刷新枪械 equip 动画。
 */
@Mod.EventBusSubscriber(
        modid = Alone_adventure.MODID,
        value = Dist.CLIENT
)
public final class ClientGunHandler {

    // =========================================================
    // 后坐力
    // =========================================================

    private static final float RECOIL_APPLY_RATE = 0.6F;
    private static final float RECOIL_MIN_STEP = 0.05F;

    private static float recoilPending = 0.0F;

    // =========================================================
    // 左键状态
    // =========================================================

    private static boolean attackWasDown = false;

    // =========================================================
    // 双持排程
    // =========================================================

    private static InteractionHand nextFireHand = InteractionHand.MAIN_HAND;

    private static long mainReadyAt = 0L;
    private static long offhandReadyAt = 0L;

    private static long nextShotAt = 0L;

    // =========================================================
    // 开火动画事件
    //
    // 注意：
    // 这些时间戳只在真正发送 GunFirePacket 时更新。
    // NBT 更新不会修改这里。
    // =========================================================

    private static long mainFireAt = Long.MIN_VALUE;
    private static long offFireAt = Long.MIN_VALUE;

    // =========================================================
    // 开火事件序号
    //
    // 时间戳解决“什么时候开火”。
    // serial 解决“这是第几次开火事件”。
    //
    // 即使连续两次事件发生在同一个 tick，也不会丢失。
    // =========================================================

    private static long mainFireSerial = 0L;
    private static long offFireSerial = 0L;

    private ClientGunHandler() {
    }

    // =========================================================
    // 后坐力
    // =========================================================

    /**
     * 服务端确认开火后，由 GunRecoilPacket 调用。
     */
    public static void enqueueRecoil(float degrees) {
        recoilPending += degrees;
    }

    // =========================================================
    // 开火事件访问接口
    // =========================================================

    public static float ticksSinceFire(
            InteractionHand hand,
            float now
    ) {
        long at = getLastFireAt(hand);

        if (at == Long.MIN_VALUE) {
            return Float.MAX_VALUE;
        }

        return now - at;
    }

    public static long getLastFireAt(
            InteractionHand hand
    ) {
        return hand == InteractionHand.MAIN_HAND
                ? mainFireAt
                : offFireAt;
    }

    /**
     * 获取指定手的开火事件序号。
     */
    public static long getFireSerial(
            InteractionHand hand
    ) {
        return hand == InteractionHand.MAIN_HAND
                ? mainFireSerial
                : offFireSerial;
    }

    /**
     * 获取最近一次开火事件序号。
     *
     * 双持情况下只用于判断是否出现新的 fire 事件。
     */
    public static long getLatestFireSerial() {
        return Math.max(
                mainFireSerial,
                offFireSerial
        );
    }

    /**
     * 获取最近一次开火时间。
     */
    public static long getLatestFireAt() {
        return Math.max(
                mainFireAt,
                offFireAt
        );
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

        if (player == null || player.isSpectator()) {
            return;
        }

        ItemStack main =
                player.getMainHandItem();

        if (main.getItem() instanceof GunItem) {

            /*
             * 枪械完全接管左键。
             *
             * 不允许 vanilla 执行：
             *
             * - 攻击
             * - 挥手
             * - 挖方块
             */
            event.setCanceled(true);
        }
    }

    // =========================================================
    // R：装填
    //
    // G：切换弹药
    // =========================================================

    @SubscribeEvent
    public static void onKeyInput(
            InputEvent.Key event
    ) {

        // =====================================================
        // R：reload
        // =====================================================

        if (ModClientSetup.GUN_RELOAD_KEY.consumeClick()) {

            LocalPlayer player =
                    Minecraft.getInstance().player;

            if (player != null
                    && !player.isSpectator()
                    && (
                    player.getMainHandItem().getItem()
                            instanceof GunItem
                            ||
                            player.getOffhandItem().getItem()
                                    instanceof GunItem
            )) {

                /*
                 * 这里只记录“客户端动画事件”。
                 *
                 * 不读取 NBT。
                 * 不等待服务器修改 NBT。
                 */
                GunItemAnimation.markReload(
                        player.tickCount
                );

                /*
                 * 真正的弹药修改交给服务端。
                 */
                Alone_adventure.NETWORK.sendToServer(
                        new GunReloadPacket()
                );
            }
        }

        // =====================================================
        // G：切换弹药
        // =====================================================

        if (ModClientSetup.GUN_SWITCH_AMMO_KEY.consumeClick()) {

            LocalPlayer player =
                    Minecraft.getInstance().player;

            if (player != null
                    && !player.isSpectator()
                    && player.getMainHandItem()
                    .getItem() instanceof GunItem) {

                Alone_adventure.NETWORK.sendToServer(
                        new GunSwitchAmmoPacket()
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

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc =
                Minecraft.getInstance();

        LocalPlayer player =
                mc.player;

        // =====================================================
        // 没有玩家
        // =====================================================

        if (player == null) {

            recoilPending = 0.0F;

            attackWasDown = false;

            nextFireHand =
                    InteractionHand.MAIN_HAND;

            mainReadyAt = 0L;
            offhandReadyAt = 0L;
            nextShotAt = 0L;

            mainFireAt =
                    Long.MIN_VALUE;

            offFireAt =
                    Long.MIN_VALUE;

            mainFireSerial = 0L;
            offFireSerial = 0L;

            return;
        }

        // =====================================================
        // 后坐力
        // =====================================================

        if (recoilPending > 0.005F) {

            float step = Math.min(
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
                    oldPitch - player.getXRot();

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
                mc.screen == null
                        && mc.options.keyAttack.isDown();

        // =====================================================
        // 主手枪
        // =====================================================

        GunItem mainGunItem =
                player.getMainHandItem()
                        .getItem() instanceof GunItem mainGun
                        ? mainGun
                        : null;

        // =====================================================
        // 副手枪
        // =====================================================

        GunItem offGunItem =
                player.getOffhandItem()
                        .getItem() instanceof GunItem offGun
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

        if (dualWielding && attackDown) {

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
                                mainGunItem.getStats()
                                        .fireRateTicks()
                        );

                /*
                 * 只有真正到下一发时间，
                 * 才产生一次 fire 事件。
                 */
                if (now >= mainReadyAt) {

                    Alone_adventure.NETWORK.sendToServer(
                            new GunFirePacket(
                                    InteractionHand.MAIN_HAND
                            )
                    );

                    /*
                     * 记录真正的一发。
                     */
                    mainFireAt = now;

                    /*
                     * fire serial +1。
                     */
                    mainFireSerial++;

                    /*
                     * 排程下一发。
                     */
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

                /*
                 * 记录真正的一发。
                 */
                mainFireAt = now;

                /*
                 * 产生新的 fire 事件。
                 */
                mainFireSerial++;

                mainReadyAt =
                        now + Math.max(
                                1,
                                mainGunItem.getStats()
                                        .fireRateTicks()
                        );
            }
        }

        // =====================================================
        // 松开左键 / 离开双持
        // =====================================================

        if (!dualWielding || !attackDown) {

            nextFireHand =
                    InteractionHand.MAIN_HAND;

            mainReadyAt = 0L;
            offhandReadyAt = 0L;
            nextShotAt = 0L;
        }

        // =====================================================
        // 保存本 tick 左键状态
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
        // 发送开火包
        // =====================================================

        Alone_adventure.NETWORK.sendToServer(
                new GunFirePacket(hand)
        );

        // =====================================================
        // fireRate
        // =====================================================

        int interval =
                Math.max(
                        1,
                        gun.getStats()
                                .fireRateTicks()
                );

        // =====================================================
        // 记录动画事件
        // =====================================================

        if (hand == InteractionHand.MAIN_HAND) {

            mainReadyAt =
                    now + interval;

            mainFireAt =
                    now;

            mainFireSerial++;

        } else {

            offhandReadyAt =
                    now + interval;

            offFireAt =
                    now;

            offFireSerial++;
        }

        // =====================================================
        // 双持交替
        // =====================================================

        nextShotAt =
                now + Math.max(
                        1,
                        interval / 2
                );

        nextFireHand =
                hand == InteractionHand.MAIN_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;
    }

    // =========================================================
    // 右键瞄准
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
                .getItem() instanceof GunItem gun) {

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