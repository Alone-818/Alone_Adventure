package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.RaidManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 派系加入网络包 - 客户端从派系令牌界面选择后发送
 */
public class FactionJoinPacket {

    private final String factionId;

    public FactionJoinPacket(String factionId) {
        this.factionId = factionId;
    }

    public FactionJoinPacket(Faction faction) {
        this.factionId = faction.getId();
    }

    public static void encode(FactionJoinPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.factionId);
    }

    public static FactionJoinPacket decode(FriendlyByteBuf buf) {
        return new FactionJoinPacket(buf.readUtf());
    }

    public static void handle(FactionJoinPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                // 查找对应的派系
                Faction faction = null;
                for (Faction f : Alone818.com.alone_adventure.faction.ModFactions.all()) {
                    if (f.getId().equals(packet.factionId)) {
                        faction = f;
                        break;
                    }
                }

                if (faction == null) {
                    return;
                }

                // 加入派系
                RaidManager.joinFaction(player, faction);

                // 消耗令牌（如果不是创造模式）
                ItemStack stack = player.getMainHandItem();
                if (!player.getAbilities().instabuild && stack.is(Alone818.com.alone_adventure.init.ModItems.FACTION_JOIN_TOKEN.get())) {
                    stack.shrink(1);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
