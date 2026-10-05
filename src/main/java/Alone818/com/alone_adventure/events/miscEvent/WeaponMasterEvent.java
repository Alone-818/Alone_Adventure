package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 习武之人 - 效果处理
 *
 * 1. 攻击速度 +10%
 * 2. 伤害 +20%
 * 3. 二连击机制：
 *    - 基础概率 10%
 *    - 每次未触发时 +5%
 *    - 触发后重置到 10%
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class WeaponMasterEvent {

    // 每个玩家的双击概率状态
    private static final Map<UUID, Float> DOUBLE_ATTACK_CHANCES = new HashMap<>();

    private static final float BASE_CHANCE = 0.10F;
    private static final float INCREMENT = 0.05F;
    private static final float MAX_CHANCE = 0.95F;

    /**
     * 判断玩家是否佩戴习武之人
     */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.WEAPON_MASTER.get())
                .isPresent();
    }

    /**
     * 攻击事件：处理二连击
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.level().isClientSide()) {
                return;
            }
            if (!isWearing(attacker)) {
                return;
            }

            UUID uuid = attacker.getUUID();
            float currentChance = DOUBLE_ATTACK_CHANCES.getOrDefault(uuid, BASE_CHANCE);

            // 随机判定
            if (attacker.getRandom().nextFloat() < currentChance) {
                // 触发二连击：重置概率并执行第二次攻击
                DOUBLE_ATTACK_CHANCES.put(uuid, BASE_CHANCE);
                executeDoubleHit(attacker, target, event.getAmount());
            } else {
                // 未触发：增加概率
                float newChance = Math.min(MAX_CHANCE, currentChance + INCREMENT);
                DOUBLE_ATTACK_CHANCES.put(uuid, newChance);
            }
        }
    }

    /**
     * 执行第二次攻击
     */
    private static void executeDoubleHit(Player attacker, LivingEntity target, float originalDamage) {
        // 二连击：以相同伤害进行一次额外攻击
        target.hurt(attacker.damageSources().playerAttack(attacker), originalDamage);
    }
}
