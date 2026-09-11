package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 复活动画包 —— 服务端触发免疫复活后发给跟踪该玩家的客户端，
 * 播放类似不死图腾的动画：环绕粒子 + 图腾音效，
 * 并由触发者本人显示"物品激活"动画（展示的是触发复活的饰品，而非图腾）。
 */
public class ReviveEffectPacket {

    private final int entityId;
    private final ItemStack curio;

    public ReviveEffectPacket(int entityId, ItemStack curio) {
        this.entityId = entityId;
        this.curio = curio;
    }

    /**
     * 服务端发送：发给跟踪该玩家实体的所有客户端（包括该玩家本人）。
     * 单例饰品直接序列化整个堆栈，客户端即可展示同一物品。
     */
    public static void sendToTracking(ServerPlayer player, ItemStack curio) {
        Alone_adventure.NETWORK.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ReviveEffectPacket(player.getId(), curio));
    }

    public static void encode(ReviveEffectPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeItem(packet.curio);
    }

    public static ReviveEffectPacket decode(FriendlyByteBuf buf) {
        return new ReviveEffectPacket(buf.readInt(), buf.readItem());
    }

    /** 仅客户端处理：粒子、音效与物品激活动画 */
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ReviveEffectPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        Entity target = level.getEntity(packet.entityId);
        if (target == null) return;

        // 与原版不死图腾动画一致：金色粒子环绕 + 图腾音效
        mc.particleEngine.createTrackingEmitter(target, ParticleTypes.TOTEM_OF_UNDYING, 30);
        level.playLocalSound(target.getX(), target.getY(), target.getZ(),
                SoundEvents.TOTEM_USE, target.getSoundSource(), 1.0F, 1.0F, false);

        // 只有触发复活的玩家本人显示物品激活动画，展示触发的饰品
        if (target == mc.player) {
            mc.gameRenderer.displayItemActivation(packet.curio);
        }
    }

    public static void handle(ReviveEffectPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(packet));
        ctx.setPacketHandled(true);
    }
}
