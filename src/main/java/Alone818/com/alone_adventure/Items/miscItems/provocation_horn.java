package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 挑衅号角 - 主动挑起派系突袭。
 *
 * 右键吹响：
 *
 * - 在所有仇恨 >= 突袭临界（400）的派系里，
 *   挑仇恨最高的那个，立刻对它发起突袭
 * - 无视同派系冷却（60 分钟）、
 *   全局冷却（20 分钟）与等待队列
 * - 已有突袭进行中时吹不响
 *   （系统同时只允许一场突袭）
 * - 没有仇恨达标的派系时吹不响
 *
 * 号角不消耗：突袭期间仇恨冻结，
 * 吹响也挑不出第二个派系。
 */
public class provocation_horn extends Item {

    public provocation_horn() {
        super(
                new Properties()
                        .stacksTo(1)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        if (level.isClientSide) {

            return InteractionResultHolder.sidedSuccess(
                    stack,
                    true
            );
        }

        ServerPlayer serverPlayer =
                (ServerPlayer) player;

        // 已有突袭：吹不响
        if (RaidManager.getActiveRaidFaction(
                serverPlayer.getUUID()
        ) != null) {

            serverPlayer.sendSystemMessage(
                    Component.translatable(
                            "horn.alone_adventure.raid_active"
                    )
            );

            return InteractionResultHolder.fail(stack);
        }

        // 没有仇恨达标的派系：吹不响
        if (!RaidManager.provokeRaid(serverPlayer)) {

            serverPlayer.sendSystemMessage(
                    Component.translatable(
                            "horn.alone_adventure.no_target",
                            RaidManager.RAID_THRESHOLD
                    )
            );

            return InteractionResultHolder.fail(stack);
        }

        // 吹响声（startRaid 里另有一声突袭号角）
        serverPlayer.level()
                .playSound(
                        null,
                        serverPlayer.blockPosition(),
                        SoundEvents.RAID_HORN.value(),
                        SoundSource.PLAYERS,
                        1.4F,
                        1.2F
                );

        return InteractionResultHolder.sidedSuccess(
                stack,
                false
        );
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.provocation_horn.tooltip.desc"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.provocation_horn.tooltip.use",
                        RaidManager.RAID_THRESHOLD
                ).withStyle(ChatFormatting.AQUA)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.provocation_horn.tooltip.cooldown"
                ).withStyle(ChatFormatting.RED)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.provocation_horn.tooltip.busy"
                ).withStyle(ChatFormatting.RED)
        );
    }
}
