package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.Items.BulletProjectile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 弹药基类。
 *
 * 特种弹药可以通过覆写以下方法改变枪械行为：
 *
 * getAdditionalBulletCount()
 *     每次开火额外增加的弹丸数量。
 *
 * getSpreadMultiplier()
 *     散布倍率。
 *
 * getDamageMultiplier()
 *     伤害倍率。
 *
 * getRangeMultiplier()
 *     射程倍率。
 *
 * onFired()
 *     子弹生成后调用。
 *
 * onBulletHit()
 *     子弹命中实体后调用。
 *
 * onBulletHitBlock()
 *     子弹命中方块后调用。
 */
public class AmmoItem extends Item {

    public AmmoItem(Properties properties) {
        super(
                properties.stacksTo(64)
        );
    }

    // =========================================================
    // 射击参数修改
    // =========================================================

    /**
     * 每次开火额外增加的弹丸数量。
     *
     * 默认 0。
     */
    public int getAdditionalBulletCount(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 0;
    }

    /**
     * 散布倍率。
     *
     * 1.0 = 正常
     * 2.0 = 两倍散布
     */
    public float getSpreadMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 1.0F;
    }

    /**
     * 伤害倍率。
     *
     * 1.0 = 正常
     * 0.3 = 降低 70%
     */
    public float getDamageMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 1.0F;
    }

    /**
     * 射程倍率。
     *
     * 1.0 = 正常
     * 0.1 = 只剩 10% 射程
     */
    public float getRangeMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 1.0F;
    }

    // =========================================================
    // 开火钩子
    // =========================================================

    /**
     * 子弹生成后调用。
     */
    public void onFired(
            ServerPlayer shooter,
            ItemStack gun,
            BulletProjectile bullet
    ) {
    }

    // =========================================================
    // 实体命中钩子
    // =========================================================

    /**
     * 子弹命中实体后调用。
     *
     * damage 是这颗子弹原本的实际伤害。
     */
    public void onBulletHit(
            ServerLevel level,
            BulletProjectile bullet,
            Entity hit,
            float damage
    ) {
    }

    // =========================================================
    // 方块命中钩子
    // =========================================================

    /**
     * 子弹命中方块后调用。
     *
     * 返回 true：
     *     表示该弹药已经处理了方块命中，
     *     BulletProjectile 不再继续处理反弹 / 消失逻辑。
     *
     * 返回 false：
     *     使用 BulletProjectile 原来的反弹 / 消失逻辑。
     */
    public boolean onBulletHitBlock(
            ServerLevel level,
            BulletProjectile bullet,
            BlockHitResult result
    ) {
        return false;
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
                        "item.alone_adventure.ammo.tooltip.desc"
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );
    }
    // =========================================================
// 弹药箱
// =========================================================

    /**
     * 使用弹药箱补充一发该弹药需要消耗的弹药箱库存。
     *
     * 普通弹药：
     *     1
     *
     * 特殊弹药：
     *     可以覆写为 2。
     */
    public int getAmmoBoxCost() {
        return 1;
    }
}