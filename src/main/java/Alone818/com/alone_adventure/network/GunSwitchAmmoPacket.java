package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 枪械切换弹药包 —— 客户端按下切换弹药键（默认 G）且主手持枪时发出，
 * 服务端对该主手枪循环切换选中的弹药类型（校验以服务端为准）。
 * 空载荷：切换哪把枪完全由服务端根据主手物品决定。
 */
public class GunSwitchAmmoPacket {

    public GunSwitchAmmoPacket() {
    }

    public static void encode(GunSwitchAmmoPacket packet, FriendlyByteBuf buf) {
        // 无载荷
    }

    public static GunSwitchAmmoPacket decode(FriendlyByteBuf buf) {
        return new GunSwitchAmmoPacket();
    }

    public static void handle(GunSwitchAmmoPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 只有服务端会收到该包；其余方向直接忽略
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (stack.getItem() instanceof GunItem gun) {
                    gun.switchAmmo(player, stack);
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}
