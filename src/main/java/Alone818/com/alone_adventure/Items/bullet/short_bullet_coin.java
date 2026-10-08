package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import net.minecraft.world.item.ItemStack;

public class short_bullet_coin extends AmmoItem {

    public short_bullet_coin() {
        super(new Properties());
    }

    /**
     * 每次开火额外 +6 颗。
     *
     * 基础 1 发：
     * 1 + 6 = 7 发
     * 改为 +7 颗
     */
    @Override
    public int getAdditionalBulletCount(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 7;
    }

    /**
     * 散布变成原来的 2 倍。
     */
    @Override
    public float getSpreadMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 2.0F;
    }

    /**
     * 射程只剩原来的 10%。
     *
     * 等于降低 90%。
     */
    @Override
    public float getRangeMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 0.05F;
    }
    /**
     * 伤害倍率：0.8。
     *
     * 原为 0.32，改为 80% 伤害。
     */
    @Override
    public float getDamageMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 0.8F;
    }
    @Override
    public int getAmmoBoxCost() {
        return 2;
    }
    @Override
    public int getAmmoBoxAmount() {
        return 1;
    }
}