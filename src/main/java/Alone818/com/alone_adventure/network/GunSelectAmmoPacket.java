package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Items.gun.GunItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public class GunSelectAmmoPacket {


    private final int index;


    public GunSelectAmmoPacket(int index) {
        this.index = index;
    }


    public void encode(
            FriendlyByteBuf buf
    ) {
        buf.writeInt(index);
    }


    public static GunSelectAmmoPacket decode(
            FriendlyByteBuf buf
    ) {
        return new GunSelectAmmoPacket(
                buf.readInt()
        );
    }


    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {

        NetworkEvent.Context ctx =
                supplier.get();


        ctx.enqueueWork(() -> {

            ServerPlayer player =
                    ctx.getSender();


            if (player == null) {
                return;
            }


            ItemStack stack =
                    player.getItemInHand(
                            InteractionHand.MAIN_HAND
                    );


            if (!(stack.getItem()
                    instanceof GunItem gun)) {

                return;
            }


            /*
             * 服务端验证索引。
             *
             * 防止客户端发送非法 index
             */
            if (index < 0
                    || index >= gun.getStats(stack)
                    .ammoTypeCount()) {

                return;
            }


            gun.selectAmmo(
                    player,
                    stack,
                    index
            );

        });


        ctx.setPacketHandled(true);
    }
}