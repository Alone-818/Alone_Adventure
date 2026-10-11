package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.miscCurios.strake_challenge_gauntlet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

/**
 * 斯特拉克的挑战护手 - 效果处理
 *
 * 动态属性切换（每 tick 同步）：
 * - 生命值 >50%：+20% 最大生命 / -20% 移速
 * - 生命值 ≤50%：-20% 护甲 / +20% 移速 / +10% 攻速
 *
 * 主动技能（由 StrakeChallengeGauntlet.activateSkill 在 R 键按下时触发）：
 * - 生命值 >50%：扣血至 50% 并获得力量 III/速度 II 15 秒
 * - 生命值 ≤50%：回血至 50% 并获得虚弱 II/迟缓 I/再生 III 10 秒 + 最大生命 50% 的黄心
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class StrakeChallengeGauntletEvent {

    /** 高血量阈值 */
    public static final double HIGH_HP_THRESHOLD = 0.50;

    private StrakeChallengeGauntletEvent() {}

    /** 判断玩家是否佩戴斯特拉克的挑战护手 */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, Alone818.com.alone_adventure.init.ModItems.STRAKE_CHALLENGE_GAUNTLET.get())
                .isPresent();
    }

    private static Optional<ItemStack> getCurioStack(ServerPlayer player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, Alone818.com.alone_adventure.init.ModItems.STRAKE_CHALLENGE_GAUNTLET.get())
                .map(slotResult -> slotResult.stack());
    }

    /** 检查玩家当前是否处于高血量状态 */
    public static boolean isHighHp(ServerPlayer player) {
        return player.getHealth() > player.getMaxHealth() * HIGH_HP_THRESHOLD;
    }

    // ===== 动态属性切换 =====

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;

        // 节流：每 5 tick 同步一次
        if (player.tickCount % 5 != 0) return;

        boolean highHp = isHighHp(player);

        // 更新最大生命值属性
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            AttributeModifier existing = healthAttr.getModifier(strake_challenge_gauntlet.MAX_HEALTH_HIGH_UUID);
            if (highHp) {
                // 高血量：+20% 最大生命
                if (existing == null || existing.getAmount() != strake_challenge_gauntlet.MAX_HEALTH_BONUS_HIGH) {
                    healthAttr.removeModifier(strake_challenge_gauntlet.MAX_HEALTH_HIGH_UUID);
                    healthAttr.addTransientModifier(new AttributeModifier(
                            strake_challenge_gauntlet.MAX_HEALTH_HIGH_UUID,
                            "Strake High HP Max Health",
                            strake_challenge_gauntlet.MAX_HEALTH_BONUS_HIGH,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            } else {
                // 低血量：移除最大生命加成
                if (existing != null) {
                    healthAttr.removeModifier(strake_challenge_gauntlet.MAX_HEALTH_HIGH_UUID);
                }
            }
        }

        // 更新移动速度属性
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            AttributeModifier existing = speedAttr.getModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_HIGH_UUID);
            if (highHp) {
                // 高血量：-20% 移速
                if (existing == null || existing.getAmount() != strake_challenge_gauntlet.MOVEMENT_SPEED_PENALTY_HIGH) {
                    speedAttr.removeModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_HIGH_UUID);
                    speedAttr.addTransientModifier(new AttributeModifier(
                            strake_challenge_gauntlet.MOVEMENT_SPEED_HIGH_UUID,
                            "Strake High HP Movement Speed",
                            strake_challenge_gauntlet.MOVEMENT_SPEED_PENALTY_HIGH,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                // 移除低血量移速加成
                if (speedAttr.getModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_LOW_UUID) != null) {
                    speedAttr.removeModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_LOW_UUID);
                }
            } else {
                // 低血量：+20% 移速
                if (existing != null) {
                    speedAttr.removeModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_HIGH_UUID);
                }
                AttributeModifier lowExisting = speedAttr.getModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_LOW_UUID);
                if (lowExisting == null || lowExisting.getAmount() != strake_challenge_gauntlet.MOVEMENT_SPEED_BONUS_LOW) {
                    speedAttr.removeModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_LOW_UUID);
                    speedAttr.addTransientModifier(new AttributeModifier(
                            strake_challenge_gauntlet.MOVEMENT_SPEED_LOW_UUID,
                            "Strake Low HP Movement Speed",
                            strake_challenge_gauntlet.MOVEMENT_SPEED_BONUS_LOW,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }

        // 更新护甲属性
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            AttributeModifier existing = armorAttr.getModifier(strake_challenge_gauntlet.ARMOR_LOW_UUID);
            if (highHp) {
                // 高血量：移除护甲减益
                if (existing != null) {
                    armorAttr.removeModifier(strake_challenge_gauntlet.ARMOR_LOW_UUID);
                }
            } else {
                // 低血量：-20% 护甲
                if (existing == null || existing.getAmount() != strake_challenge_gauntlet.ARMOR_PENALTY_LOW) {
                    armorAttr.removeModifier(strake_challenge_gauntlet.ARMOR_LOW_UUID);
                    armorAttr.addTransientModifier(new AttributeModifier(
                            strake_challenge_gauntlet.ARMOR_LOW_UUID,
                            "Strake Low HP Armor",
                            strake_challenge_gauntlet.ARMOR_PENALTY_LOW,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }

        // 更新攻击速度属性
        AttributeInstance atkSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeedAttr != null) {
            AttributeModifier existing = atkSpeedAttr.getModifier(strake_challenge_gauntlet.ATTACK_SPEED_LOW_UUID);
            if (highHp) {
                // 高血量：移除攻速加成
                if (existing != null) {
                    atkSpeedAttr.removeModifier(strake_challenge_gauntlet.ATTACK_SPEED_LOW_UUID);
                }
            } else {
                // 低血量：+10% 攻速
                if (existing == null || existing.getAmount() != strake_challenge_gauntlet.ATTACK_SPEED_BONUS_LOW) {
                    atkSpeedAttr.removeModifier(strake_challenge_gauntlet.ATTACK_SPEED_LOW_UUID);
                    atkSpeedAttr.addTransientModifier(new AttributeModifier(
                            strake_challenge_gauntlet.ATTACK_SPEED_LOW_UUID,
                            "Strake Low HP Attack Speed",
                            strake_challenge_gauntlet.ATTACK_SPEED_BONUS_LOW,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
    }

    /**
     * 移除玩家身上的斯特拉克挑战护手属性修饰符。
     * 当玩家脱下饰品时调用。
     */
    public static void removeModifiers(ServerPlayer player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(strake_challenge_gauntlet.MAX_HEALTH_HIGH_UUID);
        }
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_HIGH_UUID);
            speedAttr.removeModifier(strake_challenge_gauntlet.MOVEMENT_SPEED_LOW_UUID);
        }
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(strake_challenge_gauntlet.ARMOR_LOW_UUID);
        }
        AttributeInstance atkSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeedAttr != null) {
            atkSpeedAttr.removeModifier(strake_challenge_gauntlet.ATTACK_SPEED_LOW_UUID);
        }
    }

    // ===== 主动技能 =====

    /**
     * R 键触发主动技能。
     * - 生命值 >50%：扣血至 50% 并获得力量 III/速度 II 15 秒
     * - 生命值 ≤50%：回血至 50% 并获得虚弱 II/迟缓 I/再生 III 10 秒 + 最大生命 50% 的黄心
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<top.theillusivec4.curios.api.SlotResult> curioOpt =
                CuriosApi.getCuriosHelper().findFirstCurio(
                        player,
                        Alone818.com.alone_adventure.init.ModItems.STRAKE_CHALLENGE_GAUNTLET.get());
        if (curioOpt.isEmpty()) return false;

        ItemStack stack = curioOpt.get().stack();

        // 检查冷却
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(strake_challenge_gauntlet.TAG_SKILL_READY_AT)
                && now < tag.getLong(strake_challenge_gauntlet.TAG_SKILL_READY_AT)) {
            return false;
        }

        boolean highHp = isHighHp(player);

        if (highHp) {
            // 扣血至 50%
            float targetHealth = player.getMaxHealth() * (float) HIGH_HP_THRESHOLD;
            float damageToDeal = player.getHealth() - targetHealth;
            if (damageToDeal > 0) {
                player.hurt(player.damageSources().magic(), damageToDeal);
            }

            // 获得力量 III/速度 II，15 秒
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    strake_challenge_gauntlet.HIGH_HP_DURATION_TICKS,
                    strake_challenge_gauntlet.STRENGTH_LEVEL_HIGH,
                    false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    strake_challenge_gauntlet.HIGH_HP_DURATION_TICKS,
                    strake_challenge_gauntlet.SPEED_LEVEL_HIGH,
                    false, true));
        } else {
            // 回血至 50%
            float targetHealth = player.getMaxHealth() * (float) HIGH_HP_THRESHOLD;
            float healAmount = targetHealth - player.getHealth();
            if (healAmount > 0) {
                player.heal(healAmount);
            }

            // 获得虚弱 III/迟缓 III/再生 III，10 秒
            player.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    strake_challenge_gauntlet.LOW_HP_DURATION_TICKS,
                    strake_challenge_gauntlet.WEAKNESS_LEVEL_LOW,
                    false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    strake_challenge_gauntlet.LOW_HP_DURATION_TICKS,
                    strake_challenge_gauntlet.SLOW_LEVEL_LOW,
                    false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    strake_challenge_gauntlet.LOW_HP_DURATION_TICKS,
                    strake_challenge_gauntlet.REGENERATION_LEVEL_LOW,
                    false, true));

            // 获得基于最大生命值 50% 的黄心（伤害吸收）
            float absorptionAmount = player.getMaxHealth() * 0.5f;
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), absorptionAmount));
        }

        // 设置冷却
        tag.putLong(strake_challenge_gauntlet.TAG_SKILL_READY_AT, now + strake_challenge_gauntlet.SKILL_COOLDOWN_TICKS);
        stack.setTag(tag);

        // 更新槽位
        top.theillusivec4.curios.api.SlotResult slotResult = curioOpt.get();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        return true;
    }
}
