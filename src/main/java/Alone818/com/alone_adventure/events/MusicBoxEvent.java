package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 诡异八音盒（eerie_music_box）—— 护盾伤害池与死亡计时。
 *
 * 状态全部记录在玩家持久化 NBT（跨登录、跨维度有效，随死亡清零）：
 *   {@value #TAG_SHIELD}      剩余护盾点数（1:1 抵消最终伤害）
 *   {@value #TAG_DEATH_AT}     死亡时刻（游戏刻）
 *   {@value #TAG_DEATH_START}  诅咒开始的时刻（用于进度条）
 *
 * - 护盾在 LivingDamageEvent（伤害已经过护甲/药水减免的最终值）中结算，
 *   与 ImmunityEvent/ShieldEvent 同样对 /kill、虚空等绕过无敌的伤害放行；
 * - 死亡时刻一到即以 genericKill 处决：无视无敌帧、不死图腾与一切护盾，
 *   本模组的致命伤害免疫饰品（求生渴望/龙胤之力）对此同样放行；
 * - 诅咒期间在屏幕顶部显示紫色 Boss 血条（服务端权威，自动同步），
 *   展示剩余护盾点数与剩余秒数；每 20 tick 刷新一次。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class MusicBoxEvent {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 使用获得的护盾点数 */
    public static double SHIELD_AMOUNT = 90.0D;
    /** 使用后距离死亡的时间：60 秒（600 tick） */
    public static int DEATH_DELAY_TICKS = 600;

    public static final String TAG_SHIELD = "MusicBoxShield";
    public static final String TAG_DEATH_AT = "MusicBoxDeathAt";
    public static final String TAG_DEATH_START = "MusicBoxDeathStart";

    /** 最后通牒：剩余秒数达到该值时警告一次（10 秒） */
    private static final int WARN_SECONDS_LEFT = 10;

    /** 每个被诅咒玩家的 Boss 血条（UUID → 血条） */
    private static final Map<UUID, ServerBossEvent> CURSE_BARS = new HashMap<>();

    // ===== 护盾：伤害池结算 =====

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // 已被其他处理器（如结晶心脏护盾）取消的伤害不再处理
        if (event.isCanceled()) return;
        // /kill（genericKill）和虚空坠落等无视无敌保护的伤害不能被护盾豁免，必须放行
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        CompoundTag data = player.getPersistentData();
        double shield = data.getDouble(TAG_SHIELD);
        if (shield <= 0) return;

        float damage = event.getAmount();
        if (damage <= 0) return;

        // 1:1 抵消：护盾吃掉多少，伤害就少多少
        double absorbed = Math.min(shield, damage);
        double left = shield - absorbed;
        if (left > 0) {
            data.putDouble(TAG_SHIELD, left);
        } else {
            data.remove(TAG_SHIELD);
        }

        float remaining = damage - (float) absorbed;
        if (remaining <= 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(remaining);
        }
    }

    // ===== 死亡计时 =====

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_DEATH_AT)) return;

        long deathAt = data.getLong(TAG_DEATH_AT);
        long now = player.level().getGameTime();
        long remainTicks = deathAt - now;

        // 旋律停止，生命被收走
        if (remainTicks <= 0) {
            clearCurse(data);
            removeBar(player);
            player.level().playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0F, 0.3F);
            player.level().playSound(null, player, SoundEvents.WARDEN_ROAR,
                    SoundSource.PLAYERS, 0.6F, 0.8F);
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }

        // 最后 10 秒的通牒（离线跨越该时刻则错过，可接受）
        if (remainTicks == WARN_SECONDS_LEFT * 20L) {
            player.level().playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
            player.displayClientMessage(
                    Component.translatable("gui.alone_adventure.eerie_music_box.warning"), true);
        }

        // 每 20 tick 刷新诅咒进度条（护盾点数 + 剩余秒数）
        if (player.tickCount % 20 == 0) {
            long total = Math.max(1, deathAt - data.getLong(TAG_DEATH_START));
            updateBar(player, data.getDouble(TAG_SHIELD), remainTicks, total);
        }
    }

    /** 刷新/创建诅咒 Boss 血条（重复 addPlayer 为幂等操作，重登后自动补挂） */
    private static void updateBar(ServerPlayer player, double shield, long remainTicks, long totalTicks) {
        ServerBossEvent bar = CURSE_BARS.computeIfAbsent(player.getUUID(), id -> new ServerBossEvent(
                Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(Mth.clamp(remainTicks / (float) totalTicks, 0.0F, 1.0F));
        bar.setName(Component.translatable("gui.alone_adventure.eerie_music_box.bar",
                (int) Math.ceil(shield), (remainTicks + 19) / 20));
        bar.addPlayer(player);
    }

    // ===== 清理 =====

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 无论因何而死：诅咒与护盾一并清零
        if (!(event.getEntity() instanceof Player player)) return;
        clearCurse(player.getPersistentData());
        removeBar(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // 下线只移除 Boss 血条（避免持有失效连接），诅咒 NBT 保留
        removeBar(event.getEntity());
    }

    private static void clearCurse(CompoundTag data) {
        data.remove(TAG_SHIELD);
        data.remove(TAG_DEATH_AT);
        data.remove(TAG_DEATH_START);
    }

    private static void removeBar(Player player) {
        ServerBossEvent bar = CURSE_BARS.remove(player.getUUID());
        if (bar != null && player instanceof ServerPlayer serverPlayer) {
            bar.removePlayer(serverPlayer);
        }
    }
}
