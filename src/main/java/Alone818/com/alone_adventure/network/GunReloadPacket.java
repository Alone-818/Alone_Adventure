package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 枪械装填包 —— 客户端按下装填键（默认 R）时发出，
 * 服务端对主副手所有持枪开始装填（校验以服务端为准）。
 * 空载荷：装填哪些枪完全由服务端根据当前手部物品决定。
 */
public class GunReloadPacket {

    public GunReloadPacket() {
    }

    public static void encode(GunReloadPacket packet, FriendlyByteBuf buf) {
        // 无载荷
    }

    public static GunReloadPacket decode(FriendlyByteBuf buf) {
        return new GunReloadPacket();
    }

    public static void handle(GunReloadPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 只有服务端会收到该包；其余方向直接忽略
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                // 主副手所有可用枪同时开始装填（双手枪仅主手）
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (stack.getItem() instanceof GunItem gun) {
                        gun.tryReload(player, hand, stack);
                    }
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}
