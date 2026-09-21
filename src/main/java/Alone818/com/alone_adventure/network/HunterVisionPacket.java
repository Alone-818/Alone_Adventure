package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.client.HunterVisionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 猎人视野包 —— 服务端激活猎人血清后发给施放者本人的客户端，
 * 由 HunterVisionClient 开启去色着色器并高亮范围内的生物/凋落物/容器。
 * 仅客户端处理（与 ReviveEffectPacket 同款模式）。
 */
public class HunterVisionPacket {

    private final int durationTicks;

    public HunterVisionPacket() {
        this(0);
    }

    public HunterVisionPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public static void encode(HunterVisionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.durationTicks);
    }

    public static HunterVisionPacket decode(FriendlyByteBuf buf) {
        return new HunterVisionPacket(buf.readVarInt());
    }

    /** 仅客户端处理：开启猎人视野 */
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(HunterVisionPacket packet) {
        HunterVisionClient.activate(packet.durationTicks);
    }

    public static void handle(HunterVisionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 只有客户端会收到该包；其余方向直接忽略
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> handleClient(packet));
        }
        ctx.setPacketHandled(true);
    }
}
