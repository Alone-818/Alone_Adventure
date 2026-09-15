package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 墨制刀刃 - 远程投掷武器
 *
 * 右键掷出一枚飞刃（{@link InkBladeProjectile}），命中造成
 * {@value InkBladeProjectile#DAMAGE} 点伤害并施加虚弱 I（{@value InkBladeProjectile#WEAKNESS_DURATION_TICKS} tick）。
 * 投掷冷却 {@value #COOLDOWN_TICKS} tick（0.15 秒），走原版物品冷却系统，遮罩自动同步客户端。
 *
 * 耐久机制：共 {@value #MAX_THROWS} 点耐久，每掷出一把刀刃消耗 1 点；
 * 刀刃由墨凝聚、不会损坏（耐久归零只是暂时无法投掷），
 * 闲置时每 {@value #REGEN_INTERVAL_TICKS} tick（0.5 秒）自动回收 1 点耐久（每秒 2 点）。
 * 回收进度以"下一次回收的游戏刻"写入 NBT，服务端权威、跨存档有效。
 */
public class ink_blade extends Item {

    /** 满耐久可掷出的刀刃数量 */
    public static final int MAX_THROWS = 24;
    /** 投掷冷却：0.15 秒（3 tick） */
    public static final int COOLDOWN_TICKS = 3;
    /** 耐久回收间隔：每 0.5 秒（10 tick）回收 1 点，即每秒 2 点 */
    public static final int REGEN_INTERVAL_TICKS = 10;
    /** 飞刃初速（雪球为 1.5，提升后弹道更快更平） */
    public static final float THROW_SPEED = 2.5F;

    /** NBT 标签：下一次耐久回收的游戏刻（level.getGameTime() 口径） */
    public static final String TAG_NEXT_REGEN = "InkBladeNextRegen";

    public ink_blade() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(MAX_THROWS));
    }

    // ===== 右键投掷 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中（0.15 秒）直接拦截
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        // 耐久耗尽：无法掷出，等待回收
        if (stack.getDamageValue() >= MAX_THROWS) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            InkBladeProjectile blade = new InkBladeProjectile(
                    ModEntities.INK_BLADE.get(), player, level, stack);
            blade.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, THROW_SPEED, 0.0F);
            level.addFreshEntity(blade);
            level.playSound(null, player, SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 1.0F, 0.8F);

            // 掷出一把刀刃：消耗 1 点耐久（手动加伤，永不触发损坏）
            stack.setDamageValue(stack.getDamageValue() + 1);
            // 从本次投掷起 0.5 秒后开始回收
            stack.getOrCreateTag().putLong(TAG_NEXT_REGEN,
                    level.getGameTime() + REGEN_INTERVAL_TICKS);

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(stack);
    }

    // ===== 耐久回收 =====

    /**
     * 玩家背包内的服务端 tick：耐久未满时，每到达成回收时刻便回复 1 点。
     * inventoryTick 在服务端对玩家背包物品每 tick 调用一次，无需额外事件。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity,
                              int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player)) return;
        if (stack.getDamageValue() <= 0) return; // 耐久已满，无需回收

        CompoundTag tag = stack.getTag();
        long next = tag == null ? 0L : tag.getLong(TAG_NEXT_REGEN);
        long now = level.getGameTime();
        if (now < next) return;

        stack.setDamageValue(stack.getDamageValue() - 1);
        stack.getOrCreateTag().putLong(TAG_NEXT_REGEN, now + REGEN_INTERVAL_TICKS);
    }

    /** 耐久条常显（即使满耐久），方便观察剩余刀刃 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    /** 剩余耐久（可投掷次数），供 tooltip 展示 */
    public static int remainingBlades(ItemStack stack) {
        return MAX_THROWS - stack.getDamageValue();
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.ink_blade.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.ink_blade.tooltip.throw_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.ink_blade.tooltip.durability_desc")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.ink_blade.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("item.alone_adventure.ink_blade.tooltip.blades_left",
                        remainingBlades(stack), MAX_THROWS)
                .withStyle(remainingBlades(stack) == 0 ? ChatFormatting.RED : ChatFormatting.WHITE));
    }
}
