package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 枪械动画行为状态机 —— GeckoLib 控制器谓词。
 *
 * 每把枪一个 "behavior" 控制器，按优先级选取行为动画：
 * <b>aim（瞄准）&gt; fire（开火）&gt; reload（装填）&gt; run（跑动）&gt; idle（待机）</b>。
 * 各行为经 {@link GunGeoModel} 解析：单文件 {@code animations/gun/<物品id>.animation.json}
 * （Blockbench 整包导出，推荐）优先，逐行为文件
 * {@code animations/gun/<物品id>_<行为>.animation.json}（旧约定）兼容；
 * <b>缺文件自动降级到下一优先级</b>（一把枪可以只提供部分行为的动画），
 * 全缺则静止。状态间过渡由 GeckoLib 控制器的过渡时长平滑（5 tick）。
 *
 * 状态来源（全客户端可读，无需额外同步）：
 * <ul>
 *   <li>fire —— {@link ClientGunHandler} 双手各自的开火时刻（取最近，双持时两手同踢）</li>
 *   <li>reload —— 当前渲染堆栈的装填 NBT（{@value GunItem#TAG_RELOAD_START}）</li>
 *   <li>aim —— 玩家正在使用枪械（右键瞄准）</li>
 *   <li>run —— 玩家疾跑</li>
 * </ul>
 *
 * 注意：物品为单例，GeckoLib 控制器状态按<b>物品类型</b>共享——双持两把枪
 * 的动画同步（一手开火两手都做踢枪动作）；动画文件按物品 id 命名，天然一枪一套。
 */
public final class GunItemAnimation {

    // ===== 行为名（英文，即动画文件名/内名后缀） =====
    public static final String FIRE = "fire";
    public static final String RELOAD = "reload";
    public static final String AIM = "aim";
    public static final String RUN = "run";
    public static final String IDLE = "idle";

    /** 连射重播窗口：距上次开火不足该 tick 数时重置当前开火动画（重新起踢） */
    private static final float REPLAY_TICKS = 2.0F;
    /** 开火动画收尾余量（tick）：动画播完后短暂保持末帧再回落 */
    private static final float FIRE_TAIL_TICKS = 2.0F;

    /** 当前渲染中的枪械堆栈（renderByItem 存入，控制器谓词读取；渲染单线程） */
    private static ItemStack currentStack = ItemStack.EMPTY;

    private GunItemAnimation() {
    }

    /** 渲染器每次 renderByItem 开头调用：登记本帧渲染的堆栈（装填等按堆栈判定） */
    static void beginRender(ItemStack stack) {
        currentStack = stack;
    }

    /**
     * 控制器谓词：按优先级选取行为动画。
     * 由 {@link GunItem#registerControllers} 挂到 "behavior" 控制器（仅客户端渲染线程调用）。
     */
    public static PlayState handle(AnimationState<GunItem> state) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        GunItem gun = state.getAnimatable();
        String itemId = BuiltInRegistries.ITEM.getKey(gun).getPath();

        // ── aim：正在右键瞄准（使用枪械），瞄准射击时保持瞄准姿态 ──
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof GunItem
                && resolve(itemId, AIM) != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(itemId + "_" + AIM));
        }

        // ── fire：任一手最近开火且动画尚未播完 ──
        float now = player.tickCount + state.getPartialTick();
        float sinceFire = Math.min(
                ClientGunHandler.ticksSinceFire(InteractionHand.MAIN_HAND, now),
                ClientGunHandler.ticksSinceFire(InteractionHand.OFF_HAND, now));
        Animation fire = resolve(itemId, FIRE);
        if (fire != null && sinceFire >= 0.0F
                && sinceFire <= fire.length() * 20.0F + FIRE_TAIL_TICKS) {
            RawAnimation kick = RawAnimation.begin().thenPlay(itemId + "_" + FIRE);
            // 连射：开火动画还在播又打出新一发时重置重播
            if (sinceFire < REPLAY_TICKS && state.isCurrentAnimation(kick)) {
                state.resetCurrentAnimation();
            }
            return state.setAndContinue(kick);
        }

        // ── reload：当前堆栈处于装填状态（NBT 记录装填开始时刻） ──
        CompoundTag tag = currentStack.getTag();
        if (tag != null && tag.contains(GunItem.TAG_RELOAD_START) && resolve(itemId, RELOAD) != null) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(itemId + "_" + RELOAD));
        }

        // ── run：疾跑（控制器过渡自动平滑进出） ──
        if (player.isSprinting() && resolve(itemId, RUN) != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(itemId + "_" + RUN));
        }

        // ── idle：兜底 ──
        if (resolve(itemId, IDLE) != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(itemId + "_" + IDLE));
        }
        return PlayState.STOP;
    }

    /**
     * 解析行为动画：委托 {@link GunGeoModel#resolveAnimation} 双形式解析
     * （单文件 {@code <物品id>.animation.json} 优先，逐行为文件兼容）。
     * 缺文件返回 null = 该行为未提供。
     */
    @Nullable
    public static Animation resolve(String itemId, String behavior) {
        return GunGeoModel.resolveAnimation(itemId, behavior);
    }

    /** 是否处于双持举枪（主手单手枪 + 副手可用枪）：主手模型切举枪变体 */
    public static boolean isDualWield(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(stack.getItem() instanceof GunItem gun)) return false;
        if (stack != player.getMainHandItem()) return false; // 仅主手举枪（与旧版 dual 谓词一致）
        if (gun.getStats().handedness() != GunStats.Handedness.ONE_HANDED) return false;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof GunItem offGun
                && GunItem.isUsableInHand(offGun, InteractionHand.OFF_HAND);
    }
}
