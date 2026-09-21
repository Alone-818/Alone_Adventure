package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.events.MusicBoxEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * 诡异八音盒 - 诅咒类消耗道具（仅作用于自身）
 *
 * 右键使用：立即获得 {@value MusicBoxEvent#SHIELD_AMOUNT} 点护盾
 * （伤害池护盾，1:1 抵消受到的最终伤害，持续到耗尽或死亡），
 * 但 60 秒后旋律停止，使用者将<b>立刻死亡</b>——
 * 伤害走 genericKill，无视无敌与不死图腾，无法被本模组的
 * 致命伤害免疫饰品（求生渴望/龙胤之力）与各类护盾豁免。
 *
 * 死亡时限与护盾记录在玩家持久化 NBT 中（{@link MusicBoxEvent}）：
 * 重登/换维度无法逃避，但随死亡一并清零；
 * 重复使用可刷新护盾，但死亡时限以第一次使用起算，无法推迟。
 * 消耗类道具：使用后消耗 1 个，可堆叠 16。
 */
public class eerie_music_box extends Item {

    public eerie_music_box() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag data = player.getPersistentData();

            // 护盾：刷新至上限（已有更高不减少）
            double shield = data.getDouble(MusicBoxEvent.TAG_SHIELD);
            data.putDouble(MusicBoxEvent.TAG_SHIELD,
                    Math.max(shield, MusicBoxEvent.SHIELD_AMOUNT));

            // 诅咒：只有第一次使用写下死亡时刻，重复使用无法推迟
            if (!data.contains(MusicBoxEvent.TAG_DEATH_AT)) {
                long now = level.getGameTime();
                data.putLong(MusicBoxEvent.TAG_DEATH_AT, now + MusicBoxEvent.DEATH_DELAY_TICKS);
                data.putLong(MusicBoxEvent.TAG_DEATH_START, now);
            }

            level.playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
            player.displayClientMessage(
                    Component.translatable("gui.alone_adventure.eerie_music_box.used"), true);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.eerie_music_box.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.eerie_music_box.tooltip.use_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.eerie_music_box.tooltip.curse_desc")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.eerie_music_box.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
