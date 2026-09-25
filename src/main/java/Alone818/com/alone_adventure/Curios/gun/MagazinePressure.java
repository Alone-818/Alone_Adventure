package Alone818.com.alone_adventure.Curios.gun;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunStatTooltipBuilder;
import Alone818.com.alone_adventure.Items.gun.GunStats;
import Alone818.com.alone_adventure.Items.gun.IGunStatTooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

public class MagazinePressure
        extends GunTraitItem
        implements IGunStatTooltip {

    /**
     * 最低伤害倍率：
     * 空弹夹 = 60%
     */
    public static final float MIN_MULTIPLIER = 0.60F;

    /**
     * 最高伤害倍率：
     * 满弹夹 = 200%
     */
    public static final float MAX_MULTIPLIER = 2.00F;

    public MagazinePressure() {
        super(
                new Properties()
                        .stacksTo(1)
        );
    }

    /**
     * 判断玩家是否佩戴了这个饰品。
     */
    public static boolean isWearing(Player player) {

        return CuriosApi
                .getCuriosInventory(player)
                .resolve()
                .map(handler ->
                        handler.findFirstCurio(
                                stack ->
                                        stack.getItem()
                                                instanceof MagazinePressure
                        ).isPresent()
                )
                .orElse(false);
    }

    /**
     * 根据当前弹匣子弹数量计算伤害倍率。
     *
     * 0 / 满弹夹 = 0.60
     * 满 / 满弹夹 = 2.00
     *
     * 中间线性变化。
     */
    public static float getDamageMultiplier(
            Player player,
            ItemStack gunStack,
            GunStats stats
    ) {

        if (!isWearing(player)) {
            return 1.0F;
        }

        if (gunStack.isEmpty()) {
            return 1.0F;
        }

        int magazineSize =
                Math.max(
                        1,
                        stats.magazineSize()
                );

        int currentAmmo = 0;

        if (gunStack.hasTag()) {
            currentAmmo =
                    gunStack.getTag()
                            .getInt(GunItem.TAG_AMMO);
        }

        currentAmmo =
                Mth.clamp(
                        currentAmmo,
                        0,
                        magazineSize
                );

        /*
         * 当前弹匣填充比例：
         *
         * 0.0 = 空弹夹
         * 1.0 = 满弹夹
         */
        float ratio =
                (float) currentAmmo
                        / (float) magazineSize;

        /*
         * 0%   -> 0.60
         * 25%  -> 0.95
         * 50%  -> 1.30
         * 75%  -> 1.65
         * 100% -> 2.00
         */
        return
                MIN_MULTIPLIER
                        + ratio
                        * (MAX_MULTIPLIER - MIN_MULTIPLIER);
    }

    @Override
    public void addGunStatTooltip(
            List<Component> tooltip
    ) {

        tooltip.add(
                Component.translatable(
                        "trait.alone_adventure.magazine_pressure.description"
                ).withStyle(
                        ChatFormatting.WHITE
                )
        );

        tooltip.add(
                Component.translatable(
                        "curios.modifiers.trait"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        tooltip.add(
                Component.translatable(
                        "trait.alone_adventure.magazine_pressure.effect"
                ).withStyle(
                        ChatFormatting.WHITE
                )
        );

        GunStatTooltipBuilder.addMultiplier(
                tooltip,
                "damage",
                2.0F
        );
    }
}