package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.events.ImmunityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * 求生渴望 - 致命伤害时传送回家
 * 当玩家受到致命伤害时：
 * - 免疫此次伤害
 * - 传送回重生点（家），没有床时回到世界出生点
 * - 3 分钟冷却
 *
 * 伤害免疫、冷却计时与写回槽位由 {@link ImmunityEvent} 统一处理，
 * 本类只提供冷却常量与触发成功后的效果（传送回家）。
 */
public class survival_whimper extends Item implements ICurioItem {

    // 冷却时长：3 分钟 = 180 秒 = 3600 tick
    public static final int COOLDOWN_TICKS = 3600;

    public survival_whimper() {
        super(new Properties().stacksTo(1));
    }

    /**
     * 免疫致命伤害后的效果：传送回家。
     * 优先传送到玩家重生点（床/重生锚，重生点在其他维度时跨维度传送），
     * 没有可用重生点时退回到当前世界的出生点。
     */
    public static void teleportHome(ServerPlayer player) {
        BlockPos home = player.getRespawnPosition();
        if (home != null) {
            ServerLevel homeLevel = player.server.getLevel(player.getRespawnDimension());
            if (homeLevel != null) {
                double y = home.getY() + 1.0;
                if (homeLevel == player.level()) {
                    // 同维度：普通传送即可（1.20.1 中同维度也必须走 teleport(ServerLevel,...) 才可靠）
                    player.teleportTo(home.getX() + 0.5, y, home.getZ() + 0.5);
                } else {
                    // 跨维度：changeDimension 签名的 teleport 重载
                    player.teleportTo(homeLevel, home.getX() + 0.5, y, home.getZ() + 0.5,
                            player.getYRot(), player.getXRot());
                }
                return;
            }
        }

        // 无重生点：传送到当前世界出生点
        Vec3 spawn = player.serverLevel().getSharedSpawnPos().getCenter();
        player.teleportTo(spawn.x, player.serverLevel().getSharedSpawnPos().getY() + 1.0, spawn.z);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.survival_whimper.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.alone_adventure.survival_whimper.tooltip.immunity")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.survival_whimper.tooltip.teleport")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.alone_adventure.survival_whimper.tooltip.cooldown")
                    .withStyle(ChatFormatting.AQUA));

            // 冷却中时实时显示剩余时间（NBT 由 Curios 同步到客户端）
            if (level != null) {
                long remaining = ImmunityEvent.getRemainingCooldown(stack, level.getGameTime());
                if (remaining > 0) {
                    tooltip.add(Component.translatable("item.alone_adventure.survival_whimper.tooltip.cooldown_left",
                                    Math.round(remaining / 20.0F))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.survival_whimper.tooltip.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
