package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.client.ClientGunHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 枪械后坐力包 —— 服务端成功开火后发给射手本人的客户端，
 * 客户端把视角在约 2 tick 内快速上抬且不回落（经典压枪手感，
 * 见 ClientGunHandler）。
 * 仅客户端处理（与 HunterVisionPacket 同款模式）。
 */
public class GunRecoilPacket {

    /** 枪口向上偏移的角度（度） */
    private final float recoilDegrees;

    public GunRecoilPacket(float recoilDegrees) {
        this.recoilDegrees = recoilDegrees;
    }

    public static void encode(GunRecoilPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.recoilDegrees);
    }

    public static GunRecoilPacket decode(FriendlyByteBuf buf) {
        return new GunRecoilPacket(buf.readFloat());
    }

    /** 仅客户端处理：排入平滑后坐力 */
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(GunRecoilPacket packet) {
        ClientGunHandler.enqueueRecoil(packet.recoilDegrees);
    }

    public static void handle(GunRecoilPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 只有客户端会收到该包；其余方向直接忽略
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> handleClient(packet));
        }
        ctx.setPacketHandled(true);
    }
}
