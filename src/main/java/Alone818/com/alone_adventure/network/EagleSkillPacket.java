package Alone818.com.alone_adventure.network;

import Alone818.com.alone_adventure.Curios.towerCurios.binding_bandage;
import Alone818.com.alone_adventure.Curios.towerCurios.broken_mask;
import Alone818.com.alone_adventure.Curios.towerCurios.charging_core;
import Alone818.com.alone_adventure.Curios.miscCurios.hunter_serum;
import Alone818.com.alone_adventure.Curios.ImperialCurios.imperial_eagle;
import Alone818.com.alone_adventure.Curios.towerCurios.necromancer_ledger;
import Alone818.com.alone_adventure.Curios.towerCurios.sealed_throne;
import Alone818.com.alone_adventure.Curios.trait.shadow_step;
import Alone818.com.alone_adventure.init.ModItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 饰品主动技能包 —— 客户端按键（默认 R）触发后发往服务端，
 * 服务端依次尝试触发佩戴中的主动技能饰品（各自验证佩戴与冷却）：
 * 帝国天鹰 → 破损面具 → 紧缚绷带 → 封印王座 → 充能核心 → 猎人血清 → 亡灵秘典 → 暗影跃迁。
 */
public class EagleSkillPacket {

    public EagleSkillPacket() {}

    public static void encode(EagleSkillPacket packet, FriendlyByteBuf buf) {
        // 暗影跃迁需要额外数据：目标坐标
        // 0 = 普通技能 (帝国天鹰等)
        // 1 = 暗影跃迁 (发送目标坐标)
        buf.writeBoolean(true); // true 表示这是暗影跃迁类型
        buf.writeDouble(0); // targetX (占位，实际由服务端计算视线)
        buf.writeDouble(0); // targetY
        buf.writeDouble(0); // targetZ
    }

    public static EagleSkillPacket decode(FriendlyByteBuf buf) {
        return new EagleSkillPacket();
    }

    public static void handle(EagleSkillPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                // 先尝试普通技能
                if (!imperial_eagle.activateSkill(sender)
                        && !broken_mask.activateSkill(sender)
                        && !binding_bandage.activateSkill(sender)
                        && !sealed_throne.activateSkill(sender)
                        && !charging_core.activateSkill(sender)
                        && !hunter_serum.activateSkill(sender)
                        && !necromancer_ledger.activateSkill(sender)) {
                    // 若普通技能均不可用，尝试暗影跃迁（需要读取当前视线）
                    if (!tryShadowStep(sender)) {
                        // 全部不可用（未佩戴/冷却中）：给本人短促提示音
                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_HARP.get(), SoundSource.PLAYERS, 0.6F, 0.5F);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * 尝试激活暗影跃迁技能。
     * 从玩家视线开始，射程40格，若命中方块则瞬移至方块中心。
     * 冷却15秒，瞬移后获得5秒抗性I + 10%移速提升。
     */
    private static boolean tryShadowStep(ServerPlayer player) {
        // 验证佩戴暗影跃迁
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.SHADOW_STEP.get());
        if (curioOpt.isEmpty()) return false;

        ItemStack stack = curioOpt.get().stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(shadow_step.TAG_SKILL_READY_AT) && now < tag.getLong(shadow_step.TAG_SKILL_READY_AT)) {
            return false;
        }

        // 计算射线
        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = startPos.add(lookVec.scale(40.0));

        ClipContext context = new ClipContext(
                startPos,
                endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );

        BlockHitResult hitResult = player.level().clip(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // 获取指向方块的中心
            var pos = hitResult.getBlockPos();
            double targetX = pos.getX() + 0.5;
            double targetY = pos.getY() + 0.5;
            double targetZ = pos.getZ() + 0.5;

            // 瞬移到目标位置
            double finalY = Math.floor(targetY);
            player.teleportTo((ServerLevel) player.level(), targetX, finalY, targetZ,
                    player.getYRot(), player.getXRot());

            // 应用增益效果：5秒抗性 I + 5秒速度 I
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, true));

            // 设置冷却（15秒 = 300 tick）
            tag.putLong(shadow_step.TAG_SKILL_READY_AT, now + 300);
            stack.setTag(tag);

            // 更新槽位
            SlotResult slotResult = curioOpt.get();
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.setEquippedCurio(slotResult.slotContext().identifier(),
                            slotResult.slotContext().index(), stack));

            // 播放音效
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

            return true;
        }
        return false;
    }
}