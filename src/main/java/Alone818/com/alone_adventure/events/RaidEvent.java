package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 仇恨-突袭系统的事件接线。
 *
 * - 服务端启动 / 停止：载入 / 释放突袭存档
 * - 每 tick：推进进行中的突袭 + 周期检测仇恨
 * - 玩家上线 / 下线：血条重连 / 移除
 * - 玩家死亡：突袭死亡计数（超过 7 次判负）
 *
 * 仇恨的积累入口在 FactionLevelEvent
 * （玩家击杀派系生物时结算）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class RaidEvent {

    @SubscribeEvent
    public static void onServerStarted(
            ServerStartedEvent event
    ) {

        RaidManager.initServer(
                event.getServer()
        );
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {

        RaidManager.serverStopped();
    }

    /**
     * 服务端每 tick：
     * 推进突袭、周期检测仇恨。
     */
    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        if (server != null) {
            RaidManager.tick(server);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {

        if (event.getEntity()
                instanceof ServerPlayer player) {

            RaidManager.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {

        if (event.getEntity()
                instanceof ServerPlayer player) {

            RaidManager.onPlayerLogout(player);
        }
    }

    /**
     * 玩家死亡：突袭中的死亡计数。
     */
    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {

        if (event.getEntity()
                instanceof ServerPlayer player) {

            RaidManager.onPlayerDeath(player);
        }
    }
}
