package Alone818.com.alone_adventure.Curios.gun;

import Alone818.com.alone_adventure.Items.gun.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.GunStatTooltipBuilder;
import Alone818.com.alone_adventure.Items.gun.IGunStatTooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * 特质：盛大开场
 *
 * 命中生命值为 100% ~ 95% 的生物时：
 *
 * 1. 额外造成 1200% 伤害
 * 2. 引发一次爆炸（TNT 范围，不破坏方块）
 * 3. 随后 20 秒内，自身枪械伤害基础值 -35%
 *
 * 弱化窗口结束时间存储在玩家持久化 NBT 中。
 */
public class GrandOpening
        extends GunTraitItem
        implements IGunStatTooltip {

    /** 触发血量比例下限：95% */
    public static final float HEALTH_RATIO = 0.95F;

    /** 触发血量比例上限：100% */
    public static final float MAX_HEALTH_RATIO = 1.00F;

    /** 额外伤害倍率：1200% */
    public static final float EXTRA_DAMAGE_MULTIPLIER = 12.0F;

    /** 爆炸强度：TNT */
    public static final float EXPLOSION_POWER = 4.0F;

    /** 弱化窗口时长：20 秒 */
    public static final int DEBUFF_TICKS = 400;

    /** 弱化期间伤害倍率：-35% */
    public static final float DEBUFF_MULTIPLIER = 0.65F;

    /** 玩家持久化 NBT 键：弱化结束时间（gameTime） */
    private static final String TAG_DEBUFF_UNTIL =
            "AloneAdventure_GrandOpeningDebuffUntil";

    public GrandOpening() {
        super(
                new Properties()
                        .stacksTo(1)
        );
    }

    /**
     * 判断玩家是否佩戴了这个特质。
     */
    public static boolean isWearing(Player player) {

        return CuriosApi
                .getCuriosInventory(player)
                .resolve()
                .map(handler ->
                        handler.findFirstCurio(
                                stack ->
                                        stack.getItem()
                                                instanceof GrandOpening
                        ).isPresent()
                )
                .orElse(false);
    }

    /**
     * 获取当前生效的伤害倍率。
     *
     * 弱化窗口内返回 0.65，
     * 否则返回 1.0。
     */
    public static float getDamageMultiplier(
            Player player
    ) {

        if (!isWearing(player)) {
            return 1.0F;
        }

        long until =
                player.getPersistentData()
                        .getLong(
                                TAG_DEBUFF_UNTIL
                        );

        if (player.level()
                .getGameTime() < until) {

            return DEBUFF_MULTIPLIER;
        }

        return 1.0F;
    }

    /**
     * 判断这次命中能否触发盛大开场。
     *
     * 必须在子弹主伤害结算之前调用，
     * 因为判断的是生物被击中前的血量。
     */
    public static boolean canTrigger(
            BulletProjectile bullet,
            Entity target
    ) {

        if (!(target instanceof LivingEntity living)) {
            return false;
        }

        if (living.getMaxHealth() <= 0.0F) {
            return false;
        }

        if (!(bullet.getOwner()
                instanceof ServerPlayer shooter)) {

            return false;
        }

        if (!isWearing(shooter)) {
            return false;
        }

        float ratio =
                living.getHealth()
                        / living.getMaxHealth();

        /*
         * 100% ~ 95%
         */
        return ratio >= HEALTH_RATIO
                && ratio <= MAX_HEALTH_RATIO + 0.0001F;
    }

    /**
     * 触发盛大开场：
     *
     * 1. 额外造成 1200% 伤害
     * 2. TNT 范围爆炸（不破坏方块）
     * 3. 刷新 20 秒弱化窗口
     */
    public static void onTrigger(
            ServerLevel level,
            BulletProjectile bullet,
            LivingEntity target,
            float damage
    ) {

        if (!(bullet.getOwner()
                instanceof ServerPlayer shooter)) {

            return;
        }

        /*
         * 1. 额外造成 1200% 伤害。
         *
         * 主伤害刚刚把目标打入无敌帧，
         * 清除无敌时间保证额外伤害完整生效。
         */
        target.invulnerableTime = 0;

        target.hurt(
                bullet.damageSources()
                        .thrown(
                                bullet,
                                shooter
                        ),
                damage * EXTRA_DAMAGE_MULTIPLIER
        );

        /*
         * 2. 爆炸。
         *
         * 强度 4.0 = TNT 范围。
         * ExplosionInteraction.NONE：
         * 不破坏方块，只对生物生效。
         */
        Vec3 pos =
                bullet.position();

        level.explode(
                shooter,
                pos.x,
                pos.y,
                pos.z,
                EXPLOSION_POWER,
                Level.ExplosionInteraction.NONE
        );

        /*
         * 3. 随后 20 秒内伤害基础值 -35%。
         *
         * 再次触发会刷新窗口。
         */
        shooter.getPersistentData()
                .putLong(
                        TAG_DEBUFF_UNTIL,
                        level.getGameTime()
                                + DEBUFF_TICKS
                );

        shooter.displayClientMessage(
                Component.translatable(
                                "trait.alone_adventure.grand_opening.triggered"
                        )
                        .withStyle(
                                ChatFormatting.GOLD
                        ),
                true
        );
    }

    @Override
    public void addGunStatTooltip(
            List<Component> tooltip
    ) {

        tooltip.add(
                Component.translatable(
                                "trait.alone_adventure.grand_opening.description"
                        )
                        .withStyle(
                                ChatFormatting.WHITE
                        )
        );

        tooltip.add(
                Component.translatable(
                                "curios.modifiers.trait"
                        )
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );

        tooltip.add(
                Component.translatable(
                                "trait.alone_adventure.grand_opening.effect"
                        )
                        .withStyle(
                                ChatFormatting.WHITE
                        )
        );

        GunStatTooltipBuilder.addMultiplier(
                tooltip,
                "damage",
                12.0F
        );
    }
}
