package Alone818.com.alone_adventure.events.factionEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.RaidManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * 派系玩家特殊机制
 *
 * 1. 帝国玩家 - 协同机制：周围有其他帝国玩家时获得护甲/生命加成
 * 2. 恶魔玩家 - 低血量增益：生命低于 50% 时获得力量 II + 抗性 I
 * 3. 部落玩家 - 吸血机制：攻击造成伤害时恢复部分生命
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class FactionPlayerBonusEvent {

    // ========== 帝国协同机制 ==========
    private static final UUID EMPIRE_ARMOR_MODIFIER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID EMPIRE_HEALTH_MODIFIER_ID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    // 帝国协同半径（格）
    private static final double EMPIRE_SYNERGY_RADIUS = 16.0D;
    // 每个附近帝国玩家提供的加成
    private static final float ARMOR_PER_ALLY = 0.05F;  // 5% 护甲
    private static final float HEALTH_PER_ALLY = 0.05F; // 5% 最大生命
    // 最大叠加层数
    private static final int MAX_ALLY_STACKS = 5;

    /**
     * 帝国玩家协同效果检测 - 每 20tick (1 秒) 执行一次
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在 SERVER 端执行
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        MinecraftServer server = event.player.level().getServer();
        if (server == null) return;

        // 帝国协同机制
        updateEmpireSynergy(player, server);

        // 恶魔低血量增益
        updateDemonLowHealthBonus(player);
    }

    /**
     * 更新帝国协同效果
     */
    private static void updateEmpireSynergy(ServerPlayer player, MinecraftServer server) {
        Faction playerFaction = RaidManager.getPlayerFaction(player.getUUID());
        if (playerFaction != ModFactions.EMPIRE) return;

        // 移除旧的修饰符
        removeEmpireModifiers(player);

        // 统计周围的帝国玩家数量（不包括自己）
        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        int allyCount = 0;
        for (ServerPlayer nearby : allPlayers) {
            if (nearby == player) continue;
            if (player.distanceTo(nearby) > EMPIRE_SYNERGY_RADIUS) continue;

            Faction nearbyFaction = RaidManager.getPlayerFaction(nearby.getUUID());
            if (nearbyFaction == ModFactions.EMPIRE) {
                allyCount++;
            }
        }

        // 限制最大叠加
        allyCount = Math.min(allyCount, MAX_ALLY_STACKS);

        if (allyCount > 0) {
            // 应用护甲加成
            double armorBonus = allyCount * ARMOR_PER_ALLY;
            player.getAttribute(Attributes.ARMOR).addTransientModifier(
                new AttributeModifier(EMPIRE_ARMOR_MODIFIER_ID,
                    "Empire Synergy", armorBonus, AttributeModifier.Operation.MULTIPLY_TOTAL)
            );

            // 应用最大生命加成
            double healthBonus = allyCount * HEALTH_PER_ALLY;
            player.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(
                new AttributeModifier(EMPIRE_HEALTH_MODIFIER_ID,
                    "Empire Synergy", healthBonus, AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
    }

    /**
     * 移除帝国修饰符
     */
    private static void removeEmpireModifiers(Player player) {
        player.getAttribute(Attributes.ARMOR).removeModifier(EMPIRE_ARMOR_MODIFIER_ID);
        player.getAttribute(Attributes.MAX_HEALTH).removeModifier(EMPIRE_HEALTH_MODIFIER_ID);
    }

    // ========== 恶魔低血量增益 ==========
    private static final int DEMON_STRENGTH_LEVEL = 1;  // 力量 II (level 1 = II)
    private static final int DEMON_RESISTANCE_LEVEL = 0; // 抗性 I (level 0 = I)
    private static final int EFFECT_DURATION = 600; // 30 秒，持续刷新

    /**
     * 更新恶魔低血量增益效果
     */
    private static void updateDemonLowHealthBonus(Player player) {
        Faction playerFaction = RaidManager.getPlayerFaction(player.getUUID());
        if (playerFaction != ModFactions.DEMON) return;

        float healthPercent = player.getHealth() / player.getMaxHealth();

        if (healthPercent < 0.5F) {
            // 生命低于 50%：力量 II + 抗性 I
            player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                EFFECT_DURATION,
                DEMON_STRENGTH_LEVEL,
                false,
                false
            ));
            player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                EFFECT_DURATION,
                DEMON_RESISTANCE_LEVEL,
                false,
                false
            ));
        }
    }

    // ========== 部落吸血机制 ==========
    /**
     * 部落玩家攻击命中时吸血
     * 恢复造成伤害的 15% 作为生命值
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        Faction attackerFaction = RaidManager.getPlayerFaction(attacker.getUUID());
        if (attackerFaction != ModFactions.TRIBE) return;

        float damage = event.getAmount();
        if (damage > 0) {
            // 恢复造成伤害的 15%
            float healAmount = damage * 0.15F;
            attacker.heal(healAmount);
        }
    }
}
