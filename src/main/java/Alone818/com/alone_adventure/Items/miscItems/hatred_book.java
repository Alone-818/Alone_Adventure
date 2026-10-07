package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * 仇恨之书 - 派系仇恨查询道具
 *
 * - Shift + 右键：按注册顺序切换当前查看的派系
 *   （选择记录在物品 NBT，未记录时默认第一个派系）
 * - 右键：在聊天栏显示该派系对你的
 *   仇恨值、当前态度与突袭可能性
 *
 * 突袭可能性的判定（对应 RaidManager 的突袭规则）：
 *
 * - 该派系突袭进行中 -> 突袭已经发生
 * - 其他派系突袭进行中 -> 仇恨结算冻结中
 * - 仇恨 >= 突袭临界（400）：
 *   - 在等待队列 -> 集结完毕，
 *     显示估算的突袭倒计时
 *     （下一次周期检测与冷却取最晚）
 *   - 同派系 / 全局冷却中 -> 可能性 100%，
 *     显示冷却剩余时间
 *   - 其他 -> 100%，
 *     下一次周期检测（15 分钟一次）即会突袭，
 *     显示距下一次检测的倒计时
 * - 仇恨 < 400：可能性 = 仇恨 / 400 x 100%
 *
 * 纯查询道具：不消耗、不修改任何仇恨数据。
 */
public class hatred_book extends Item {

    /** NBT 键：当前查看的派系 id */
    private static final String TAG_FACTION =
            "HatredBookFaction";

    public hatred_book() {
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

        if (!level.isClientSide) {

            ServerPlayer serverPlayer =
                    (ServerPlayer) player;

            // Shift + 右键：切换查看的派系
            if (serverPlayer.isShiftKeyDown()) {

                Faction next =
                        nextFaction(stack);

                stack.getOrCreateTag()
                        .putString(
                                TAG_FACTION,
                                next.getId()
                        );

                serverPlayer.displayClientMessage(
                        Component.translatable(
                                "book.alone_adventure.switched",
                                next.getDisplayName()
                        ),
                        true
                );

                serverPlayer.playNotifySound(
                        SoundEvents.BOOK_PAGE_TURN,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

            } else {

                // 右键：查看当前派系的仇恨详情
                showFactionStatus(
                        serverPlayer,
                        currentFaction(stack)
                );

                serverPlayer.playNotifySound(
                        SoundEvents.BOOK_PAGE_TURN,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }

    // =========================================================
    // 仇恨详情显示
    // =========================================================

    /**
     * 在聊天栏显示某派系对该玩家的
     * 仇恨值、当前态度与突袭可能性。
     */
    private static void showFactionStatus(
            ServerPlayer player,
            Faction faction
    ) {

        UUID playerId =
                player.getUUID();

        int hatred =
                RaidManager.getHatred(
                        playerId,
                        faction
                );

        // 标题：当前派系
        player.sendSystemMessage(
                Component.translatable(
                        "book.alone_adventure.header",
                        faction.getDisplayName()
                )
        );

        // 仇恨值
        player.sendSystemMessage(
                Component.translatable(
                        "book.alone_adventure.hatred",
                        hatred,
                        RaidManager.HATRED_CAP
                )
        );

        // 当前态度
        player.sendSystemMessage(
                Component.translatable(
                        attitudeKey(
                                faction,
                                hatred
                        )
                )
        );

        // 突袭可能性
        sendRaidStatus(
                player,
                faction,
                hatred
        );
    }

    /**
     * 态度翻译键：
     *
     * - 恒敌对派系（恶魔 / 亡灵）-> 永久敌对
     * - 中立派系：仇恨过敌对线（200）-> 敌对，
     *   否则中立
     */
    private static String attitudeKey(
            Faction faction,
            int hatred
    ) {

        if (faction.isHostileToPlayers()) {
            return "book.alone_adventure.attitude.always";
        }

        return hatred >= RaidManager.HOSTILE_THRESHOLD
                ? "book.alone_adventure.attitude.hostile"
                : "book.alone_adventure.attitude.neutral";
    }

    /**
     * 突袭可能性（对应 RaidManager 的突袭规则），
     * 见类注释的判定表。
     */
    private static void sendRaidStatus(
            ServerPlayer player,
            Faction faction,
            int hatred
    ) {

        UUID playerId =
                player.getUUID();

        Faction raiding =
                RaidManager.getActiveRaidFaction(
                        playerId
                );

        // 该派系突袭进行中
        if (raiding == faction) {

            player.sendSystemMessage(
                    Component.translatable(
                            "book.alone_adventure.raid.active"
                    )
            );

            return;
        }

        // 其他派系突袭进行中：仇恨冻结
        if (raiding != null) {

            player.sendSystemMessage(
                    Component.translatable(
                            "book.alone_adventure.raid.frozen",
                            raiding.getDisplayName()
                    )
            );

            return;
        }

        // 仇恨未过临界：按仇恨占比估算
        if (hatred < RaidManager.RAID_THRESHOLD) {

            int percent =
                    Mth.clamp(
                            hatred * 100
                                    / RaidManager.RAID_THRESHOLD,
                            0,
                            100
                    );

            player.sendSystemMessage(
                    Component.translatable(
                            "book.alone_adventure.raid.chance",
                            percent
                    )
            );

            return;
        }

        // 仇恨已过临界：依次看队列与冷却
        if (RaidManager.isRaidQueued(
                playerId,
                faction
        )) {

            player.sendSystemMessage(
                    Component.translatable(
                            "book.alone_adventure.raid.queued",
                            raidEtaMinutes(
                                    player,
                                    faction
                            )
                    )
            );

            return;
        }

        long factionCooldown =
                RaidManager.getFactionCooldownRemaining(
                        player,
                        faction
                );

        if (factionCooldown > 0L) {

            player.sendSystemMessage(
                    Component.translatable(
                            "book.alone_adventure.raid.faction_cooldown",
                            minutes(factionCooldown)
                    )
            );

            return;
        }

        long globalCooldown =
                RaidManager.getGlobalCooldownRemaining(
                        player
                );

        if (globalCooldown > 0L) {

            player.sendSystemMessage(
                    Component.translatable(
                            "book.alone_adventure.raid.global_cooldown",
                            minutes(globalCooldown)
                    )
            );

            return;
        }

        // 无任何压制：下一次周期检测即会突袭，
        // 显示距下一次检测（即突袭）的倒计时
        player.sendSystemMessage(
                Component.translatable(
                        "book.alone_adventure.raid.imminent",
                        raidEtaMinutes(
                                player,
                                faction
                        )
                )
        );
    }

    /**
     * 估算该派系还有多少分钟突袭（显示用）。
     *
     * 突袭只会在周期检测（15 分钟一次）时判定：
     * - 无冷却压制：下一次检测到点即突袭
     * - 等待队列中：检测与冷却都压着时机，
     *   取最晚结束的那个估算
     */
    private static int raidEtaMinutes(
            ServerPlayer player,
            Faction faction
    ) {

        long eta =
                RaidManager.getNextCheckRemaining(
                        player
                );

        // 等待队列：冷却未结束时检测也只是续排队
        if (RaidManager.isRaidQueued(
                player.getUUID(),
                faction
        )) {

            eta = Math.max(
                    eta,
                    RaidManager.getFactionCooldownRemaining(
                            player,
                            faction
                    )
            );

            eta = Math.max(
                    eta,
                    RaidManager.getGlobalCooldownRemaining(
                            player
                    )
            );
        }

        // 至少显示 1 分钟（剩余 0 读起来像"0 分钟后"）
        return Math.max(
                1,
                minutes(eta)
        );
    }

    /**
     * tick 数换算成分钟（向上取整）。
     */
    private static int minutes(long ticks) {
        return Mth.ceil(
                ticks / 1200.0D
        );
    }

    // =========================================================
    // 派系选择
    // =========================================================

    /**
     * 当前查看的派系
     * （NBT 未记录或已失效时默认第一个）。
     */
    private static Faction currentFaction(
            ItemStack stack
    ) {

        CompoundTag tag =
                stack.getTag();

        if (tag != null
                && tag.contains(TAG_FACTION)) {

            Faction faction =
                    FactionManager.getFaction(
                            tag.getString(TAG_FACTION)
                    );

            if (faction != null) {
                return faction;
            }
        }

        return ModFactions.all()[0];
    }

    /**
     * 按注册顺序切换到下一个派系（循环）。
     */
    private static Faction nextFaction(
            ItemStack stack
    ) {

        Faction[] all =
                ModFactions.all();

        Faction current =
                currentFaction(stack);

        for (int i = 0; i < all.length; i++) {

            if (all[i] == current) {
                return all[(i + 1) % all.length];
            }
        }

        return all[0];
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
                        "item.alone_adventure.hatred_book.tooltip.desc"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_book.tooltip.view"
                ).withStyle(ChatFormatting.AQUA)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_book.tooltip.switch"
                ).withStyle(ChatFormatting.AQUA)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_book.tooltip.usage"
                ).withStyle(ChatFormatting.GRAY)
        );

        // 当前查看的派系
        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_book.tooltip.current",
                        currentFaction(stack)
                                .getDisplayName()
                ).withStyle(ChatFormatting.YELLOW)
        );
    }
}
