package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Curios.binding_bandage;
import Alone818.com.alone_adventure.Curios.broken_mask;
import Alone818.com.alone_adventure.Curios.imperial_eagle;
import Alone818.com.alone_adventure.Curios.sealed_throne;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 饰品主动技能包 —— 客户端按键（默认 R）触发后发往服务端，
 * 服务端依次尝试触发佩戴中的主动技能饰品（各自验证佩戴与冷却）：
 * 帝国天鹰 → 破损面具 → 紧缚绷带。
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
                // 一次按键按优先级尝试：未佩戴或冷却中的饰品 activateSkill 返回 false 并继续
                if (!imperial_eagle.activateSkill(sender)
                        && !broken_mask.activateSkill(sender)
                        && !binding_bandage.activateSkill(sender)
                        && !sealed_throne.activateSkill(sender)) {
                    // 全部不可用（未佩戴/冷却中）：给本人短促提示音
                    sender.playNotifySound(SoundEvents.NOTE_BLOCK_HARP.get(), SoundSource.PLAYERS, 0.6F, 0.5F);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
