package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 枪械开火包 —— 客户端拦截到左键（攻击键）且主手持枪时发出，
 * 服务端校验后调用 {@link GunItem#tryFire}。
 * 所有开火判定（冷却/装填中/弹药）均以服务端为准，客户端不做预测。
 */
public class GunFirePacket {

    private final InteractionHand hand;

    public GunFirePacket(InteractionHand hand) {
        this.hand = hand;
    }

    public static void encode(GunFirePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
    }

    public static GunFirePacket decode(FriendlyByteBuf buf) {
        return new GunFirePacket(buf.readEnum(InteractionHand.class));
    }

    public static void handle(GunFirePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 只有服务端会收到该包；其余方向直接忽略
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                ItemStack stack = player.getItemInHand(packet.hand);
                if (stack.getItem() instanceof GunItem gun) {
                    gun.tryFire(player, packet.hand, stack);
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}
