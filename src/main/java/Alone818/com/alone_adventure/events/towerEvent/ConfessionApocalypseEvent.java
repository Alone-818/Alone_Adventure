package Alone818.com.alone_adventure.events.towerEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.towerCurios.confession_apocalypse;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 忏悔默示录 - 罪孽机制事件处理
 *
 * 特性：
 * - 攻击命中实体时 +1 罪孽（单次攻击最多 +1）
 * - 受到伤害时 +1 罪孽
 * - 每层罪孽：输出伤害 +5%、受到伤害 +5%
 * - 罪孽 ≥ 8 层时，饥饿消耗速度 +20%
 * - 死亡保留一半罪孽（由 FactionPlayerDeathEvent 调用）
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ConfessionApocalypseEvent {

    /**
     * 攻击命中实体时 +1 罪孽
     * 由 AttackEntityEvent 处理，确保单次攻击只 +1 罪孽
     * 只有近战（距离 < 7 格）时才增加罪孽层数
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player player = (Player) entity;
        if (player.level().isClientSide()) return;

        Optional<SlotResult> curioOpt = getCurio(player);
        if (curioOpt.isEmpty()) return;

        ItemStack stack = curioOpt.get().stack();
        Entity target = event.getTarget();

        // 只有近战（距离 < 7 格）时才增加罪孽
        if (target != null && target instanceof LivingEntity livingTarget) {
            double distance = player.distanceTo(livingTarget);
            if (distance < 7.0) {
                confession_apocalypse.addSinOnHit(player, stack);
            }
        }
    }

    /**
     * 玩家受到伤害时 +1 罪孽
     * 在 LivingHurtEvent 中处理，因为这是实际受到伤害的事件
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        Optional<SlotResult> curioOpt = getCurio(player);
        if (curioOpt.isEmpty()) return;

        ItemStack stack = curioOpt.get().stack();

        // 应用罪孽带来的伤害增伤（每层 +10%）
        // 只在近战时触发：距离 < 5 格时才增加罪孽层数、才触发增伤
        Entity entity = event.getSource().getDirectEntity();
        if (entity != null && entity instanceof LivingEntity livingAttacker) {
            double distance = player.distanceTo(livingAttacker);
            float sinMultiplier = (float) confession_apocalypse.getSinDamageMultiplier(stack);
            if (sinMultiplier > 0 && distance < 5.0) {
                float newAmount = event.getAmount() * (1.0F + sinMultiplier);
                event.setAmount(newAmount);
            }
        }

        // 增加罪孽
        confession_apocalypse.addSinOnDamage(player, stack, event.getAmount());
    }

    /**
     * 罪孽 ≥ 8 层时，饥饿消耗速度 +20%
     * 由 LivingDamageEvent 处理（伤害已结算后）
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        Optional<SlotResult> curioOpt = getCurio(player);
        if (curioOpt.isEmpty()) return;

        ItemStack stack = curioOpt.get().stack();
        int sin = confession_apocalypse.getSin(stack);

        if (sin >= confession_apocalypse.SIN_HUNGER_THRESHOLD) {
            FoodData food = player.getFoodData();
            // 增加饥饿消耗速度：每次伤害额外消耗 0.2 点饱食度（基础值乘以 1.2）
            // Minecraft 的饥饿系统：每点伤害默认消耗 0.1 饱食度
            // 我们通过直接减少饥饿值来模拟加速消耗
            float extraHunger = event.getAmount() * 0.2F;
            food.setFoodLevel((int) Math.max(0, food.getFoodLevel() - extraHunger));
        }
    }

    /**
     * 查找玩家佩戴的忏悔默示录
     */
    public static Optional<SlotResult> getCurio(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.CONFESSION_APOCALYPSE.get());
    }
}
