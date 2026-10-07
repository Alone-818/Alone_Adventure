package Alone818.com.alone_adventure.Curios.trait;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;

/**
 * 暗影跃迁 - 特质槽位饰品
 *
 * 主动技能（按 R 键触发）：
 * - 瞬移到鼠标指针指向的位置
 * - 距离限制：40 格子
 * - 瞬移后获得 5 秒抗性 I，20% 护甲减免，10% 移速提升
 * - 冷却时间：15 秒
 */
public class shadow_step extends Item implements ICurioItem {

    /** 冷却时长：15 秒 = 300 tick */
    public static int COOLDOWN_TICKS = 300;
    /** 护甲减免等级 */
    public static int ARMOR_REDUCTION = -20; // 20% = -1 armor point * 20%
    /** 移速提升倍率 */
    public static double MOVEMENT_SPEED_BOOST = 0.10;

    /** NBT 键：技能下次可用时的世界时刻（gameTime） */
    public static final String TAG_SKILL_READY_AT = "ShadowStepReadyAt";

    public shadow_step() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    /**
     * 服务端激活技能：瞬移到目标位置并附加增益效果。
     *
     * @param player 施法玩家
     * @param targetX 目标 X 坐标
     * @param targetY 目标 Y 坐标
     * @param targetZ 目标 Z 坐标
     */
    public static void activateSkill(ServerPlayer player, double targetX, double targetY, double targetZ) {
        // 验证玩家是否佩戴暗影跃迁
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.SHADOW_STEP.get());
        if (curioOpt.isEmpty()) return;

        ItemStack stack = curioOpt.get().stack();

        // 验证冷却
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) {
            return;
        }

        // 计算距离（40 格子限制）
        double playerX = player.getX();
        double playerY = player.getY() + player.getEyeHeight();
        double playerZ = player.getZ();

        double dx = targetX - playerX;
        double dy = targetY - playerY;
        double dz = targetZ - playerZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 距离超过限制 40 格子
        if (distance > 40.0) {
            return;
        }

        // 瞬移到目标位置（脚底）
        double finalY = Math.floor(targetY); // 保证脚底在目标方块上方
        player.teleportTo((ServerLevel) player.level(), targetX, finalY, targetZ,
                player.getYRot(), player.getXRot());

        // 应用瞬移增益效果：5 秒抗性 I + 20% 护甲减免 + 10% 移速提升
        int duration = 100; // 5 秒 = 100 tick
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true));

        // 设置冷却
        tag.putLong(TAG_SKILL_READY_AT, now + COOLDOWN_TICKS);
        stack.setTag(tag);

        // 更新槽位
        SlotResult slotResult = curioOpt.get();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        // 播放末影传送音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** 获取技能剩余冷却 tick（0 表示可用） */
    public static long getSkillCooldownRemaining(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SKILL_READY_AT) || level == null) return 0;
        return Math.max(0, tag.getLong(TAG_SKILL_READY_AT) - level.getGameTime());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.skill_desc")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.distance_desc")
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.after_teleport")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.cooldown_desc")
                    .withStyle(ChatFormatting.GRAY));

            // 冷却中实时显示剩余秒数（NBT 由 Curios 同步到客户端）
            if (level != null) {
                long remaining = getSkillCooldownRemaining(stack, level);
                if (remaining > 0) {
                    tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.cooldown_left",
                                    remaining / 20)
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.shadow_step.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("tooltip.alone_adventure.curio_skill")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}