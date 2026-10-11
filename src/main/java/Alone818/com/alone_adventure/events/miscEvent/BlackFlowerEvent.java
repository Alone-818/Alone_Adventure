package Alone818.com.alone_adventure.events.miscEvent;
import net.minecraft.world.item.ItemStack;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.trait.black_flower;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 黑色花朵 - 效果处理
 *
 * 效果：
 * - 攻击时获得灵魂 (内置 cd)，灵魂层数上限为 7
 * - 损失 (灵魂数量×10%) 的生命值
 * - 按下 R 获得力量 (灵魂 -2) 等级，时长 (灵魂×5 秒)
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class BlackFlowerEvent {

    /** 灵魂获取冷却（tick）：20 tick = 1 秒 */
    private static final int SOUL_COOLDOWN_TICKS = 20;

    /**
     * 判断玩家是否佩戴黑色花朵
     */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.BLACK_FLOWER.get())
                .isPresent();
    }

    /**
     * 攻击时获得灵魂并扣除生命值
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!isWearing(player)) return;

        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(serverPlayer, ModItems.BLACK_FLOWER.get());
        if (curioOpt.isEmpty()) return;

        ItemStack stack = curioOpt.get().stack();
        CompoundTag tag = stack.getOrCreateTag();

        // 检查冷却
        long now = serverPlayer.level().getGameTime();
        if (tag.contains(black_flower.TAG_SOUL_COOLDOWN_END) && now < tag.getLong(black_flower.TAG_SOUL_COOLDOWN_END)) {
            return; // 冷却中
        }

        // 获取当前灵魂层数
        int soulCount = tag.getInt(black_flower.TAG_SOUL_COUNT);

        // 增加灵魂层数（上限 7）
        if (soulCount < black_flower.MAX_SOUL) {
            soulCount++;
            tag.putInt(black_flower.TAG_SOUL_COUNT, soulCount);

            // 设置冷却
            tag.putLong(black_flower.TAG_SOUL_COOLDOWN_END, now + SOUL_COOLDOWN_TICKS);


            // 更新 Curios 槽位
            SlotResult slotResult = curioOpt.get();
            CuriosApi.getCuriosInventory(serverPlayer).ifPresent(handler ->
                    handler.setEquippedCurio(slotResult.slotContext().identifier(),
                            slotResult.slotContext().index(), stack));
        }
    }

    /**
     * R 键触发：消耗灵魂获得力量效果
     * 由 EagleSkillPacket 调用
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.BLACK_FLOWER.get());
        if (curioOpt.isEmpty()) return false;

        ItemStack stack = curioOpt.get().stack();
        CompoundTag tag = stack.getOrCreateTag();

        int soulCount = tag.getInt(black_flower.TAG_SOUL_COUNT);

        // 需要至少 2 层灵魂才能触发
        if (soulCount < 2) {
            return false;
        }

        // 计算技能生命损失：灵魂点数 × 2
        int skillHealthLoss = soulCount * black_flower.SKILL_HEALTH_LOSS_PER_SOUL;

        // 扣血（直接设置健康值）
        float currentHealth = player.getHealth();
        float newHealth = Math.max(1.0F, currentHealth - skillHealthLoss);
        player.setHealth(newHealth);

        // 计算力量等级和时长
        int strengthLevel = soulCount - 3; // 力量 (灵魂 -2) 等级
        int durationTicks = soulCount * black_flower.STRENGTH_DURATION_MULTIPLIER; // 灵魂×5 秒 = 灵魂×100 tick

        // 应用力量效果 (DAMAGE_BOOST = 伤害提升)
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                durationTicks,
                strengthLevel,
                false,
                true
        ));

        // 清空灵魂
        tag.putInt(black_flower.TAG_SOUL_COUNT, 0);
        stack.setTag(tag);

        // 更新 Curios 槽位
        SlotResult slotResult = curioOpt.get();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        return true;
    }
}
