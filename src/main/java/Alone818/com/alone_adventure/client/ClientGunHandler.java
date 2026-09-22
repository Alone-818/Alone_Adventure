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
 * <b>左键开火</b>：原版 {@code handleKeybinds} 在使用物品（瞄准）时会把攻击键
 * 点击直接排空丢弃——{@code InteractionKeyMappingTriggered} 事件根本不会触发，
 * 因此开火改为在客户端 tick 里轮询攻击键状态。右键<b>只负责瞄准</b>：
 * <ul>
 *   <li>单持枪：按下边沿触发一次开火（半自动）</li>
 *   <li>全自动：按住左键持续开火</li>
 *   <li>双持两把枪：按住左键交替开火——每把枪射速独立计算，
 *       两发之间至少间隔上一把枪射速的一半（同款枪 T：主手 t=0，
 *       副手 t=T/2，主手 t=T……）</li>
 * </ul>
 * 攻击键事件本身只用于取消原版攻击（防挥空/挖方块）。
 *
 * <b>第三人称开火反馈</b>：由服务端在开火成功时 {@code player.swing} 广播
 * （ClientboundAnimatePacket 不回发本人）——其他玩家第三人称可见挥手，
 * 配合枪口粒子；<b>本地不播</b>，第一人称不甩枪（开火表现为 fire 踢枪动画）。
 *
 * <b>R 键装填</b>：按 {@link ModClientSetup#GUN_RELOAD_KEY}（默认 R）发送
 * {@link GunReloadPacket}，服务端对主副手所有持枪开始装填
 * （ClientInputHandler 在持枪时让位，饰品技能不触发）。
 *
 * <b>右键瞄准</b>：右键按住即使用物品，这里在 FOV 计算事件中把持枪瞄准的
 * 视野缩小（×0.5），灵敏度随 FOV 自动降低。
 *
 * <b>后坐力（经典上抬·不回落）</b>：服务端经 GunRecoilPacket 通知开火成功后，
 * 视角在约 2 tick 内快速上抬（干脆的一记顶枪，无缓慢爬升、无生硬瞬跳），
 * 之后<b>保持不动、不自动回落</b>——抬升即真实的瞄准偏移，需要玩家自己压枪
 * （枪模的踢枪动画照常回落，那是枪身归位，与视角无关）。
 * 因此连发时视角持续上抬（由玩家控制），停火后也不会出现"被拽回去"的大幅下沉。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, value = Dist.CLIENT)
public final class ClientGunHandler {

    // ── 后坐力状态（客户端本地：快速上抬、不回落） ──
    /** 每 tick 施加的剩余上抬比例（0.6 → 约 2 tick 抬满，干脆不拖沓） */
    private static final float RECOIL_APPLY_RATE = 0.6F;
    /** 单次施加强制完成阈值（度），避免长尾蠕动 */
    private static final float RECOIL_MIN_STEP = 0.05F;

    /** 待上抬的剩余角度（度）；上抬后不回落，由玩家自己压枪 */
    private static float recoilPending = 0.0F;

    // ── 左键：单持为按下边沿，双持为按住交替 ──
    private static boolean attackWasDown = false;

    // ── 双持交替开火排程（仅客户端本地节奏，服务端为最终权威） ──
    /** 下一发由哪只手开火 */
    private static InteractionHand nextFireHand = InteractionHand.MAIN_HAND;
    /** 主手枪下次可开火的本地时刻（玩家 tickCount） */
    private static long mainReadyAt = 0L;
    /** 副手枪下次可开火的本地时刻 */
    private static long offhandReadyAt = 0L;
    /** 交替最小间隔：上一把枪开火后经过其射速一半的时刻 */
    private static long nextShotAt = 0L;

    // ── 开火动画计时：各手最近一次开火的本地时刻（踢枪动画用） ──
    private static long mainFireAt = Long.MIN_VALUE;
    private static long offFireAt = Long.MIN_VALUE;

    /** 距指定手最近一次开火过去的 tick（无记录返回 Float.MAX_VALUE，供动画判定跳过） */
    public static float ticksSinceFire(InteractionHand hand, float now) {
        long at = hand == InteractionHand.MAIN_HAND ? mainFireAt : offFireAt;
        return at == Long.MIN_VALUE ? Float.MAX_VALUE : now - at;
    }

    private ClientGunHandler() {
    }

    /** 服务端确认开火后，由 GunRecoilPacket 调用：排入一段视角上抬（不回落） */
    public static void enqueueRecoil(float degrees) {
        recoilPending += degrees;
    }

    // ===== 左键 = 开火（取消原版攻击；发包在 tick 轮询，见下） =====

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GunItem) {
            // 只取消原版攻击/挥空；开火包由 onClientTick 的攻击键轮询统一发送
            //（瞄准（使用物品）时本事件不会触发，原版会自行丢弃攻击键，无需处理）
            event.setCanceled(true);
        }
    }

    // ===== R 键 = 装填 / G 键 = 切换弹药 =====

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // consumeClick 只在"本次 tick 刚按下"时返回 true，同时清掉按下次数
        if (ModClientSetup.GUN_RELOAD_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !player.isSpectator()
                    && (player.getMainHandItem().getItem() instanceof GunItem
                        || player.getOffhandItem().getItem() instanceof GunItem)) {
                Alone_adventure.NETWORK.sendToServer(new GunReloadPacket());
            }
        }
        if (ModClientSetup.GUN_SWITCH_AMMO_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !player.isSpectator()
                    && player.getMainHandItem().getItem() instanceof GunItem) {
                Alone_adventure.NETWORK.sendToServer(new GunSwitchAmmoPacket());
            }
        }
    }

    // ===== 客户端 tick：后坐力平滑 / 左键开火（单持点击·双持交替） =====

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            recoilPending = 0.0F;
            attackWasDown = false;
            nextFireHand = InteractionHand.MAIN_HAND;
            mainFireAt = Long.MIN_VALUE;
            offFireAt = Long.MIN_VALUE;
            return;
        }

        // ── 后坐力：快速上抬（约 2 tick 拉满，干脆不生硬），之后保持不回落 ──
        if (recoilPending > 0.005F) {
            float step = Math.min(
                    Math.max(recoilPending * RECOIL_APPLY_RATE, RECOIL_MIN_STEP),
                    recoilPending);
            float oldPitch = player.getXRot();
            player.setXRot(Mth.clamp(oldPitch - step, -90.0F, 90.0F));
            // 按实际施加量扣减（视角钳制在 -90° 时不多扣）
            recoilPending -= Math.min(step, oldPitch - player.getXRot());
        }

        // ── 左键开火 ──
        boolean attackDown = mc.screen == null && mc.options.keyAttack.isDown();
        GunItem mainGunItem = player.getMainHandItem().getItem() instanceof GunItem mainGun
                ? mainGun : null;
        GunItem offGunItem = player.getOffhandItem().getItem() instanceof GunItem offGun
                ? offGun : null;
        boolean mainGun = mainGunItem != null;
        // 双持条件：双手枪占用双手，主手是双手枪时不可双持；副手枪必须在该手可用
        boolean dualWielding = !player.isSpectator() && mainGun && offGunItem != null
                && mainGunItem.getStats().handedness() == GunStats.Handedness.ONE_HANDED
                && GunItem.isUsableInHand(offGunItem, InteractionHand.OFF_HAND);

        if (dualWielding && attackDown) {
            // 双持：按住左键交替开火
            long now = player.tickCount;
            // 本地排程未到时不发包（该枪自身射速 + 交替最小间隔）
            long readyAt = nextFireHand == InteractionHand.MAIN_HAND ? mainReadyAt : offhandReadyAt;
            if (now >= Math.max(nextShotAt, readyAt)) {
                fireDualShot(player, nextFireHand, now);
            }
        } else if (!dualWielding && attackDown && mainGun) {
            // 射击模式判定：全自动持续开火 / 半自动点击开火
            if (mainGunItem.getStats().fireMode() == GunStats.FireMode.FULL_AUTO) {
                // 全自动：按住左键持续开火（服务端有射速冷却验证）
                Alone_adventure.NETWORK.sendToServer(new GunFirePacket(InteractionHand.MAIN_HAND));
                mainFireAt = player.tickCount;
            } else if (!attackWasDown) {
                // 半自动：按下边沿触发一次开火
                //（第三人称挥手反馈由服务端 tryFire 成功后广播，本地不播——
                //  本地 player.swing 会让第一人称甩枪，开火表现由 fire 踢枪动画负责）
                Alone_adventure.NETWORK.sendToServer(new GunFirePacket(InteractionHand.MAIN_HAND));
                mainFireAt = player.tickCount;
            }
        }

        if (!dualWielding || !attackDown) {
            // 离开双持开火状态：重置交替（重新按住时先主手开火）
            nextFireHand = InteractionHand.MAIN_HAND;
            mainReadyAt = 0L;
            offhandReadyAt = 0L;
            nextShotAt = 0L;
        }
        attackWasDown = attackDown;
    }

    /** 双持交替开火：发送指定手的开火包并推进交替排程 */
    private static void fireDualShot(LocalPlayer player, InteractionHand hand, long now) {
        ItemStack stack = hand == InteractionHand.MAIN_HAND
                ? player.getMainHandItem()
                : player.getOffhandItem();
        if (!(stack.getItem() instanceof GunItem gun)) return;

        Alone_adventure.NETWORK.sendToServer(new GunFirePacket(hand));
        // 第三人称挥手反馈由服务端 tryFire 广播（本地不播，避免第一人称甩枪）

        // 排程：该枪冷却独立计时，两发之间至少间隔刚开火枪射速的一半
        int interval = gun.getStats().fireRateTicks();
        if (hand == InteractionHand.MAIN_HAND) {
            mainReadyAt = now + interval;
            mainFireAt = now;
        } else {
            offhandReadyAt = now + interval;
            offFireAt = now;
        }
        nextShotAt = now + Math.max(1, interval / 2);
        nextFireHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
    }

    // ===== 右键按住 = 瞄准（按枪械瞄准倍率缩放 FOV） =====

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem()) return;
        if (player.getUseItem().getItem() instanceof GunItem gun) {
            float zoom = Math.max(1.0F, gun.getStats().aimZoom());
            event.setNewFovModifier(event.getNewFovModifier() / zoom);
        }
    }
}
