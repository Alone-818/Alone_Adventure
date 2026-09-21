package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 连队团旗 - 战术支援道具
 *
 * 右键在原地插下一面军旗（{@link RegimentBannerEntity}，默认存在 30 秒）：
 * 旗子半径（默认 8 格）内的玩家持续获得抗性 II 与耐力 I；
 * 每名玩家首次进入该旗范围时补足 10 点黄心（每个旗子只生效一次）。
 *
 * 使用冷却 {@value #COOLDOWN_TICKS} tick（默认 120 秒），
 * 耐久 {@value #MAX_USES} 次，耗尽即损毁；可在铁砧中用金锭修复
 * （每锭恢复 25% 最大耐久，即 2 点）。不可堆叠、无法附魔。
 */
public class regiment_banner extends Item {

    /** 耐久（可插旗次数） */
    public static final int MAX_USES = 8;
    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 使用冷却：120 秒 */
    public static int COOLDOWN_TICKS = 20 * 120;

    public regiment_banner() {
        super(new Properties().stacksTo(1).durability(MAX_USES).rarity(Rarity.UNCOMMON));
    }

    // ===== 右键插旗 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中直接拦截
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            double x = Math.floor(player.getX()) + 0.5D;
            double z = Math.floor(player.getZ()) + 0.5D;
            double y = findGroundY(level, player);

            RegimentBannerEntity banner = new RegimentBannerEntity(
                    ModEntities.REGIMENT_BANNER.get(), level);
            banner.moveTo(x, y, z, player.getYRot(), 0.0F);
            level.addFreshEntity(banner);

            playPlantFeedback(level, x, y, z);

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 找落脚高度：从玩家脚下向下最多找 16 格的承重面
     * （跳跃/悬空时插在正下方地面上）；找不到就悬在原地。
     */
    private static double findGroundY(Level level, Player player) {
        BlockPos feet = player.blockPosition();
        for (int i = 0; i < 16; ++i) {
            BlockPos pos = feet.below(i);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                return pos.above().getY();
            }
        }
        return player.getY();
    }

    /** 插旗反馈：号角声 + 半径提示环（End Rod 一圈，仅插旗瞬间可见） */
    private static void playPlantFeedback(Level level, double x, double y, double z) {
        SoundEvent horn = SoundEvents.GOAT_HORN_SOUND_VARIANTS
                .get(level.getRandom().nextInt(SoundEvents.GOAT_HORN_SOUND_VARIANTS.size()))
                .value();
        level.playSound(null, x, y, z, horn, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (level instanceof ServerLevel server) {
            int points = 32;
            for (int i = 0; i < points; ++i) {
                double angle = Math.PI * 2.0D * i / points;
                server.sendParticles(ParticleTypes.END_ROD,
                        x + Math.cos(angle) * RegimentBannerEntity.AURA_RADIUS,
                        y + 0.2D,
                        z + Math.sin(angle) * RegimentBannerEntity.AURA_RADIUS,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // ── 金锭修复：铁砧中与金锭合成恢复耐久（每锭 2 点） ──

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.GOLD_INGOT);
    }

    // ── 无法附魔：附魔台与附魔书均不可用 ──

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.regiment_banner.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.regiment_banner.tooltip.use_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.regiment_banner.tooltip.absorption_desc")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("item.alone_adventure.regiment_banner.tooltip.repair_desc")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.regiment_banner.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
