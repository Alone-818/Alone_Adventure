package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
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
import java.util.UUID;

/**
 * 通用仇恨/安息符 - 对仇恨值最高的派系增加/减少仇恨
 *
 * 通用仇恨符：对仇恨值最高的派系增加 100 仇恨
 * 通用安息符：对仇恨值最高的派系减少 100 仇恨
 */
public class universal_talisman extends Item {

    /** 增仇（true）还是减仇（false） */
    private final boolean increase;

    /** 仇恨增减幅度 */
    private static final int AMOUNT = 100;

    public universal_talisman(boolean increase) {
        super(
                new Properties()
                        .stacksTo(16)
        );

        this.increase = increase;
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

        UUID playerId = serverPlayer.getUUID();

        // 突袭进行中：仇恨冻结，符不生效
        if (RaidManager.getActiveRaidFaction(playerId) != null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("talisman.alone_adventure.frozen")
            );
            return consume(serverPlayer, stack);
        }

        // 找出仇恨值最高的派系
        Faction highestFaction = null;
        int highestHatred = -1;

        for (Faction faction : FactionManager.getAllFactions()) {
            int hatred = RaidManager.getHatred(playerId, faction);
            if (hatred > highestHatred) {
                highestHatred = hatred;
                highestFaction = faction;
            }
        }

        if (highestFaction == null || highestHatred <= 0) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("talisman.alone_adventure.no_hostile")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return consume(serverPlayer, stack);
        }

        int amount = increase ? AMOUNT : -AMOUNT;
        RaidManager.addHatred(playerId, highestFaction, amount);

        int hatred = RaidManager.getHatred(playerId, highestFaction);

        serverPlayer.sendSystemMessage(
                Component.translatable(
                        increase
                                ? "talisman.alone_adventure.raised_universal"
                                : "talisman.alone_adventure.lowered_universal",
                        highestFaction.getDisplayName(),
                        hatred,
                        RaidManager.HATRED_CAP
                )
        );

        level.playSound(
                null,
                serverPlayer.blockPosition(),
                increase
                        ? SoundEvents.RAID_HORN.value()
                        : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                increase ? 0.8F : 1.0F,
                increase ? 0.7F : 1.4F
        );

        return consume(serverPlayer, stack);
    }

    /**
     * 消耗一个（创造模式不消耗）。
     */
    private static InteractionResultHolder<ItemStack> consume(
            ServerPlayer player,
            ItemStack stack
    ) {

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

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
                        "item.alone_adventure.universal_talisman.tooltip.desc"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        increase
                                ? "item.alone_adventure.universal_talisman.tooltip.up"
                                : "item.alone_adventure.universal_talisman.tooltip.down",
                        AMOUNT
                ).withStyle(
                        increase
                                ? ChatFormatting.RED
                                : ChatFormatting.AQUA
                )
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.universal_talisman.tooltip.use"
                ).withStyle(ChatFormatting.YELLOW)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.universal_talisman.tooltip.freeze"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
