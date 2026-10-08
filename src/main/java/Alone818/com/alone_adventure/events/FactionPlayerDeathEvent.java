package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.RaidManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 派系玩家死亡事件处理。
 *
 * 亡灵派系玩家：死亡时触发复活机制（类似亡灵派系生物）
 * - 冷却时间 360 秒（7200 tick）
 * - 白天可复活，夜晚需要等待冷却
 * - 复活时满血恢复并播放特效
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class FactionPlayerDeathEvent {

    /** 亡灵重生冷却时间（360 秒 = 7200 tick） */
    private static final int UNDEAD_RESURRECTION_COOLDOWN = 7200;

    /**
     * 玩家死亡事件：处理亡灵派系玩家复活。
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }

        // 获取玩家派系
        Faction playerFaction = RaidManager.getPlayerFaction(player.getUUID());

        if (playerFaction != ModFactions.UNDEAD) {
            return; // 不是亡灵派系，不处理
        }

        // 检查是否可以复活
        long currentTime = server.getGameTime();
        long cooldownEnd = getResurrectionCooldownEnd(player);

        if (cooldownEnd != -1 && currentTime < cooldownEnd) {
            return; // 还在冷却中，无法复活
        }

        // 检查是否为白天（可以在天空看到）
        if (!server.isDay() || !server.canSeeSky(player.blockPosition())) {
            // 夜晚也可以复活，只是需要等待冷却结束
            // 这里放宽条件，允许任何时候复活只要冷却结束
        }

        // 可以复活：阻止死亡并恢复生命
        event.setCanceled(true);

        // 设置冷却时间
        setResurrectionCooldownEnd(player, currentTime + UNDEAD_RESURRECTION_COOLDOWN);

        // 播放复活特效
        server.sendParticles(
                ParticleTypes.ENCHANT,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                30,
                0.3D,
                0.3D,
                0.3D,
                0.1D
        );

        server.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                20,
                0.2D,
                0.2D,
                0.2D,
                0.05D
        );

        // 播放声音
        player.playSound(SoundEvents.PLAYER_LEVELUP, 1.5F, 1.0F);

        // 恢复满血
        player.setHealth(player.getMaxHealth());

        // 发送消息
        player.sendSystemMessage(
                net.minecraft.network.chat.Component.translatable(
                        "faction.alone_adventure.undead_resurrect"
                ).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE)
        );
    }

    /**
     * 获取玩家的复活冷却结束时间。
     */
    private static long getResurrectionCooldownEnd(Player player) {
        return player.getPersistentData().getLong("UndeadResurrectionCooldownEnd");
    }

    /**
     * 设置玩家的复活冷却结束时间。
     */
    private static void setResurrectionCooldownEnd(Player player, long endTime) {
        player.getPersistentData().putLong("UndeadResurrectionCooldownEnd", endTime);
    }
}
