package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Alone818.com.alone_adventure.Alone_adventure.MODID)
public class NightContractEvent {

    // 夜晚露天时的生命恢复间隔（4 秒恢复 1 点）
    private static final int HEAL_INTERVAL_TICKS = 80;
    // 入夜时刻（一天 24000 tick 中夜晚开始的时刻）
    private static final long NIGHTFALL_TICKS = 13000L;
    // 月相亮度：满月(0)为 4，蛾眉/凸月为 3~1，新月(4)为 0，盈亏对称
    private static final int MAX_MOON_BRIGHTNESS = 4;
    // 每级月相亮度提供的攻击伤害/护甲百分比加成
    private static final double BONUS_PER_BRIGHTNESS = 0.10;
    // 新月保底加成（新月仍然有加成）
    private static final double NEW_MOON_BASE_BONUS = 0.10;

    private static final UUID SPEED_UUID = UUID.fromString("e5f6a7b8-c9d0-4e1f-8a2b-3c4d5e6f7a8b");

    /**
     * 月相亮度（0~4）：满月(0)最亮为 4，新月(4)为 0，盈亏对称。
     */
    public static int getMoonBrightness(int phase) {
        return phase <= 4 ? MAX_MOON_BRIGHTNESS - phase : phase - MAX_MOON_BRIGHTNESS;
    }

    /**
     * 月相加成比例：新月保底 +10%，每级亮度 +10%，满月最高 +50%。
     */
    public static double getMoonBonus(int phase) {
        return NEW_MOON_BASE_BONUS + getMoonBrightness(phase) * BONUS_PER_BRIGHTNESS;
    }

    /**
     * 判断玩家是否佩戴暗夜契约。
     */
    private static boolean isWearing(Player player) {
        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        return helper.findFirstCurio(player, ModItems.NIGHT_CONTRACT.get()).isPresent();
    }

    /**
     * 判断玩家是否处于阳光下（白天且头顶无不透明方块）。
     */
    private static boolean inSunlight(Player player, Level level) {
        return level.isDay() && level.canSeeSky(player.blockPosition());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务器端且在Tick的END阶段处理，避免双倍运行
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        Level level = player.level();

        // 节流：每 5 tick 处理一次。内部所有计时（凋零续期 %20、回血 %80、夜视 %20）
        // 都是 5 的倍数，对齐 tickCount 后行为完全不变；
        // 未佩戴时移除月相修饰符的兜底也只需 0.25 秒内生效
        if (player.tickCount % 5 != 0) {
            return;
        }

        if (!isWearing(player)) {
            removeMoonModifiers(player);
            return;
        }

        // 阳光下：持续受到凋零效果
        if (inSunlight(player, level)) {
            if (player.tickCount % 20 == 0) {
                // 每 20 tick 续期一次 2 秒的凋零 I
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, true, true));
            }
            removeMoonModifiers(player);
            return;
        }

        // 黑夜（无论是否有遮挡）：恢复生命 + 月相加成
        if (level.isNight()) {
            if (player.tickCount % HEAL_INTERVAL_TICKS == 0
                    && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0f);
            }
            applyMoonModifiers(player, level.getMoonPhase());
        } else {
            removeMoonModifiers(player);
        }
    }

    /**
     * 阳光下无法恢复生命值：取消一切生命恢复。
     */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isWearing(player)) {
            return;
        }
        if (inSunlight(player, player.level())) {
            event.setCanceled(true);
        }
    }

    /**
     * 佩戴暗夜契约可以在白天睡觉：
     * 把入睡时间检查的结果强制设为 ALLOW（注意此事件是 HasResult 语义，
     * setCanceled 无效，fireSleepingTimeCheck 只读取 getResult()）。
     */
    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (isWearing(event.getEntity())) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    /**
     * 睡醒时间跳转：佩戴暗夜契约者在入夜前入睡时，把跳转目标从
     * “次日清晨”改为“今晚入夜（13000 tick）”，跳过整个白天。
     * 夜晚正常睡觉仍走原版逻辑（跳到次日清晨）。
     */
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // 只有佩戴暗夜契约的玩家参与本次入睡时才生效
        boolean anyWearer = false;
        for (ServerPlayer sleeper : level.players()) {
            if (sleeper.isSleeping() && isWearing(sleeper)) {
                anyWearer = true;
                break;
            }
        }
        if (!anyWearer) {
            return;
        }

        long cur = level.getDayTime();
        long dayStart = cur - Math.floorMod(cur, 24000L);
        long timeOfDay = cur - dayStart;
        if (timeOfDay < NIGHTFALL_TICKS) {
            // 白天（入夜前）入睡：直接跳到今晚入夜时刻
            event.setTimeAddition(dayStart + NIGHTFALL_TICKS);
        }
    }

    // ===== 月相属性修饰符 =====

    /**
     * 暴击伤害加成：原版没有暴击伤害属性，这里在 LivingHurtEvent 中
     * 复刻 Player.attack 的暴击判定条件，判定为暴击时按月相追加额外倍率。
     * 原版暴击为 1.5 倍，满月时额外 +50%，实际为 1.5 × 1.5 = 2.25 倍。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!isWearing(player)) {
            return;
        }
        // 仅在月相加成生效期间（黑夜，无论是否有遮挡）
        if (!player.level().isNight()) {
            return;
        }
        if (!isCriticalHit(player)) {
            return;
        }
        float bonus = (float) getMoonBonus(player.level().getMoonPhase());
        event.setAmount(event.getAmount() * (1.0f + bonus));
    }

    /**
     * 复刻原版 Player.attack 的暴击判定条件
     * （下落中、离地、不在梯子/水中、无失明、非乘骑目标为生物）。
     */
    private static boolean isCriticalHit(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger();
    }

    private static void applyMoonModifiers(Player player, int phase) {
        double bonus = getMoonBonus(phase);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_UUID,
                "Night Contract Speed", bonus);

        // 夜视：每 20 tick 续期一次，时长保持 400 tick 以上可避免原版的"即将过期"闪烁
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
        }
    }

    private static void removeMoonModifiers(Player player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_UUID);
    }

    private static void applyModifier(AttributeInstance attr, UUID uuid, String name, double amount) {
        if (attr == null) {
            return;
        }
        AttributeModifier existing = attr.getModifier(uuid);
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return; // 数值未变化，无需刷新
            }
            attr.removeModifier(uuid);
        }
        if (amount != 0) {
            attr.addTransientModifier(new AttributeModifier(
                    uuid, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeModifier(AttributeInstance attr, UUID uuid) {
        if (attr != null && attr.getModifier(uuid) != null) {
            attr.removeModifier(uuid);
        }
    }
}