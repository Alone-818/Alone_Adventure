package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Curios.crystalline_heart;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = Alone818.com.alone_adventure.Alone_adventure.MODID)
public class ShieldEvent {
    // 回复冷却时间间隔（以tick为单位，1秒=20 tick。例如 10 秒 = 200 tick）
    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    public static int HEAL_INTERVAL_TICKS = 200;
    public static int ARMOR_TO_SHIELD = 5;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        // 只对玩家生效护盾
        if (!(entity instanceof Player player)) {
            return;
        }

        // /kill（genericKill）和虚空坠落等无视无敌保护的伤害不能被护盾免疫，必须放行
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        // 如果玩家已经拥有抗性 5（强度等级为 4）及以上的效果，跳过护盾触发，或者普通伤害直接被抗性免疫
        // 主要是防多次同时触发或冲突
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            MobEffectInstance effect = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (effect != null && effect.getAmplifier() >= 4) {
                // 拥有抗性 V 级以上已经可以免除所有伤害，直接跳过护盾扣除
                return;
            }
        }
        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        Optional<SlotResult> crystalOpt = helper.findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (crystalOpt.isEmpty()) {
            return;
        }

        SlotResult slotResult = crystalOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();

        double currentShield = tag.getDouble(crystalline_heart.NB_TAG_SHIELD);

        if (currentShield <= 0) {
            return;
        }

        float damage = event.getAmount();
        if (damage <= 0) {
            return;
        }

        // 扣除 1 点护盾
        double newShield = currentShield - 1;
        tag.putDouble(crystalline_heart.NB_TAG_SHIELD, newShield);

        // 伤害发生时重置/刷新上次回复时间，防止受伤后立即回盾，或者保持原来的逻辑
        // 我们选择不清除它，或者把它设置为当前时间以开始下一次回复周期的倒计时
        net.minecraft.world.level.Level level = player.level();
        tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, level.getGameTime());

        stack.setTag(tag);

        // 写回真实槽位
        String slotTypeId = slotResult.slotContext().identifier();
        int index = slotResult.slotContext().index();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.setEquippedCurio(slotTypeId, index, stack);
        });

        // 给予玩家 2 秒 (40 tick) 的 抗性 V 效果 (amplifier = 4)
        // 40 tick, amplifier 4 (即抗性 5), 两个 boolean 参数分别表示是否是环境效果、是否显示粒子和图标
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, true, true));

        // 免疫此次伤害
        event.setCanceled(true);

    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务器端且在Tick的END阶段进行计算，避免双倍运行
        net.minecraft.world.level.Level level = event.player.level();
        if (event.phase != TickEvent.Phase.END || level.isClientSide()) {
            return;
        }

        Player player = event.player;

        // 节流：每 5 tick 一次。护盾回复间隔最短也有 40 tick（200 - 韧性×8，
        // 韧性按上限 20 计），修饰符兜底与血量钳制 0.25 秒内生效无感知
        if (player.tickCount % 5 != 0) {
            return;
        }

        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        Optional<SlotResult> crystalOpt = helper.findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (crystalOpt.isEmpty()) {
            return;
        }

        // 兜底：瞬时修饰符不会写入存档，重登录等情况丢失时在此重新应用（幂等，不会叠加）
        crystalline_heart.applyMaxHealthPenalty(player);
        // 持续约束：生命值超过新上限（原最大生命值的 20%）时扣血到上限
        crystalline_heart.clampHealthToMax(player);

        double playerArmor =player.getArmorValue();
        double playerToughness=player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        SlotResult slotResult = crystalOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();

        double maxShield = tag.getDouble(crystalline_heart.NB_TAG_MAX_SHIELD)+playerArmor/ARMOR_TO_SHIELD;
        double currentShield = tag.getDouble(crystalline_heart.NB_TAG_SHIELD);

        // 如果当前护盾已经满了，直接返回。
        // 不在这里写 LastHealTime：受伤扣盾时 onLivingDamage 已记录时间，
        // 每 tick 重复写 NBT 会造成不必要的存档脏标记与客户端同步
        if (currentShield >= maxShield) {
            return;
        }

        long gameTime = level.getGameTime();
        if (!tag.contains(crystalline_heart.NB_TAG_LAST_HEAL)) {
            // 如果不存在上次回复时间，初始化为当前时间
            tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, gameTime);
            stack.setTag(tag);

            // 写回槽位
            String slotTypeId = slotResult.slotContext().identifier();
            int index = slotResult.slotContext().index();
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.setEquippedCurio(slotTypeId, index, stack);
            });
            return;
        }

        long lastHealTime = tag.getLong(crystalline_heart.NB_TAG_LAST_HEAL);
        long elapsedTicks = gameTime - lastHealTime;

        if (elapsedTicks >= HEAL_INTERVAL_TICKS-playerToughness*8) {
            // 经过 HEAL_INTERVAL_TICKS 后回复 1 点护盾
            double newShield = Math.min(maxShield, currentShield + 1);
            tag.putDouble(crystalline_heart.NB_TAG_SHIELD, newShield);
            // 更新回复时间
            tag.putLong(crystalline_heart.NB_TAG_LAST_HEAL, gameTime);
            stack.setTag(tag);

            // 写回槽位
            String slotTypeId = slotResult.slotContext().identifier();
            int index = slotResult.slotContext().index();
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.setEquippedCurio(slotTypeId, index, stack);
            });
        }
    }
}