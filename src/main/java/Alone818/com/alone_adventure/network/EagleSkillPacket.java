package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Curios.imperial_eagle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 帝国天鹰主动技能包 —— 客户端按键触发后发往服务端，
 * 服务端验证佩戴与冷却后施加效果。
 */
public class EagleSkillPacket {

    public static void encode(EagleSkillPacket packet, FriendlyByteBuf buf) {
    }

    public static EagleSkillPacket decode(FriendlyByteBuf buf) {
        return new EagleSkillPacket();
    }

    public static void handle(EagleSkillPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                imperial_eagle.activateSkill(sender);
            }
        });
        ctx.setPacketHandled(true);
    }
}
