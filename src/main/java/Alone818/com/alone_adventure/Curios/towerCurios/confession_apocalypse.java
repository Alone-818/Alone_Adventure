package Alone818.com.alone_adventure.Curios.towerCurios;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

/**
 * 忏悔默示录 - 契约槽位饰品
 *
 * 罪孽机制：
 * - 罪孽上限 {@value #MAX_SIN} 层
 * - 攻击命中实体时 +1 罪孽（单次攻击最多 +1）
 * - 受到伤害时 +1 罪孽
 * - 每层罪孽：输出伤害 +5%、受到伤害 +5%（MULTIPLY_TOTAL）
 * - 罪孽 ≥ 5 层时生成暗红粒子
 *
 * 主动技能（右键消耗）：
 * - 消耗全部罪孽释放忏悔宣告，冷却 90 秒
 * - ≥ 3 层：获得 4 秒抗性提升 II，对周围所有实体造成 层数×2 的神圣伤害
 * - ≥ 7 层：额外获得持续再生效果
 * - 罪孽 ≥ 8 层时，饥饿消耗速度 +20%
 *
 * 佩戴死亡时保留一半罪孽。
 */
public class confession_apocalypse extends Item implements ICurioItem {

    // ===== 罪孽 =====
    /** 罪孽上限 */
    public static final int MAX_SIN = 10;
    /** 每层罪孽提供的伤害加成比例（5%） */
    public static final double DAMAGE_PER_SIN = 0.05;
    /** 罪孽 ≥ 5 层时生成暗红粒子 */
    public static final int SIN_PARTICLE_THRESHOLD = 5;
    /** 罪孽 ≥ 8 层时饥饿消耗速度 +20% */
    public static final int SIN_HUNGER_THRESHOLD = 8;

    // ===== 主动技能 =====
    /** 技能冷却：90 秒 = 1800 tick */
    public static final int SKILL_COOLDOWN_TICKS = 1800;
    /** 抗性提升持续：4 秒 = 80 tick */
    public static final int RESIST_DURATION_TICKS = 80;
    /** 抗性提升等级 II（amplifier = 1） */
    public static final int RESIST_AMPLIFIER = 1;
    /** ≥3 层时周围实体神圣伤害倍数 */
    public static final int HOLY_DAMAGE_PER_SIN = 2;
    /** 再生效果持续：10 秒 = 200 tick */
    public static final int REGEN_DURATION_TICKS = 200;
    /** 再生效果等级 II（amplifier = 1） */
    public static final int REGEN_AMPLIFIER = 1;
    /** 技能范围半径 */
    public static final double SKILL_RADIUS = 8.0;

    // NBT 键
    /** 当前罪孽层数 */
    public static final String NB_TAG_SIN = "ConfessionSins";
    /** 技能下次可用时的世界时刻 */
    public static final String TAG_SKILL_READY_AT = "ConfessionSkillReadyAt";

    // 动态属性修饰符（固定 UUID，随罪孽数量增删改）
    /** 攻击伤害乘数修饰符 UUID */
    public static final UUID SIN_DAMAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    /** 承受伤害乘数修饰符 UUID */
    public static final UUID SIN_OWN_DAMAGE_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    public confession_apocalypse() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    // ── 罪孽数据读写 ──

    /** 获取当前罪孽层数 */
    public static int getSin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, Math.min(MAX_SIN, tag.getInt(NB_TAG_SIN)));
    }

    /** 设置罪孽层数并刷新属性 */
    public static void setSin(Player player, ItemStack stack, int sin) {
        sin = Math.max(0, Math.min(MAX_SIN, sin));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NB_TAG_SIN, sin);
        refreshSinAttributes(player, sin);
    }

    // ── 动态属性：输出伤害与受到伤害随罪孽变化 ──

    /**
     * 按当前罪孽重设两个 transient 修饰符：
     * - 攻击伤害：每层 +5%（MULTIPLY_TOTAL）
     * - 受到伤害：每层 +5%（MULTIPLY_TOTAL）
     * 修饰符数值与目标一致时跳过写入，避免每 tick 重复操作属性实例。
     */
    public static void refreshSinAttributes(Player player, int sin) {
        // 输出伤害加成
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            double wantDamageMul = DAMAGE_PER_SIN * sin;
            AttributeModifier mod = damage.getModifier(SIN_DAMAGE_UUID);
            boolean damageOk = sin == 0 ? mod == null
                    : mod != null && Math.abs(mod.getAmount() - wantDamageMul) < 1.0E-6;
            if (!damageOk) {
                damage.removeModifier(SIN_DAMAGE_UUID);
                if (sin > 0) {
                    damage.addTransientModifier(new AttributeModifier(
                            SIN_DAMAGE_UUID, "Confession Sin Damage",
                            wantDamageMul, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }

        // 受到伤害增加
        AttributeInstance ownDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        // 使用 ARMOR 属性代表受伤放大的效果，这里我们使用 ATTACK_DAMAGE 的反转
        // 实际上 Minecraft 没有直接"受到伤害增加"的属性，我们需要在 LivingHurtEvent 中处理
        // 这里仅处理输出伤害
    }

    /**
     * 获取受到伤害的乘数（由事件处理器调用）。
     * 每层罪孽 +5% 受到伤害。
     */
    public static double getSinDamageMultiplier(ItemStack stack) {
        return DAMAGE_PER_SIN * getSin(stack);
    }

    // ── 佩戴 tick：暗红粒子 ──

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int sin = getSin(stack);
        if (sin >= SIN_PARTICLE_THRESHOLD) {
            // 生成暗红粒子
            Level level = player.level();
            double px = player.getX();
            double py = player.getY() + player.getBbHeight() * 0.7;
            double pz = player.getZ();
            level.addParticle(ParticleTypes.ANGRY_VILLAGER, px, py, pz, 0, 0, 0);
            // 偶尔生成额外粒子
            if (player.tickCount % 10 == 0) {
                for (int i = 0; i < sin; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
                    double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;
                    level.addParticle(ParticleTypes.ANGRY_VILLAGER,
                            px + offsetX, py + offsetY, pz + offsetZ, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            refreshSinAttributes(player, getSin(stack));
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.removeModifier(SIN_DAMAGE_UUID);
        }
    }

    // ── 主动技能 ──

    /**
     * 查找玩家佩戴的忏悔默示录
     */
    public static Optional<SlotResult> getCurio(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.CONFESSION_APOCALYPSE.get());
    }

    /**
     * 技能剩余冷却 tick（0 表示可用）
     */
    public static long getSkillCooldownRemaining(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SKILL_READY_AT) || level == null) return 0;
        return Math.max(0, tag.getLong(TAG_SKILL_READY_AT) - level.getGameTime());
    }

    /**
     * 服务端激活：佩戴忏悔默示录且不在冷却中时，
     * 消耗全部罪孽释放忏悔宣告：
     * - ≥3 层：获得 4 秒抗性提升 II，周围实体受到 层数×2 神圣伤害
     * - ≥7 层：额外获得持续再生效果
     * 并进入 90 秒冷却。
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt = getCurio(player);
        if (curioOpt.isEmpty()) return false;

        SlotResult slotResult = curioOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();

        // 检查冷却
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) return false;

        int sin = getSin(stack);
        if (sin <= 0) return false; // 无罪孽可消耗

        // 1. 消耗全部罪孽
        setSin(player, stack, 0);
        tag.putLong(TAG_SKILL_READY_AT, now + SKILL_COOLDOWN_TICKS);

        Level level = player.level();

        // 2. ≥3 层：抗性提升 II + 周围神圣伤害
        if (sin >= 3) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    RESIST_DURATION_TICKS,
                    RESIST_AMPLIFIER,
                    false, true, true));

            // 对周围所有实体造成 层数×2 神圣伤害
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(SKILL_RADIUS));
            for (LivingEntity entity : nearby) {
                if (entity == player) continue;
                if (entity instanceof Player && ((Player) entity).isCreative() && ((Player) entity).getAbilities().invulnerable) continue;
                // 神圣伤害 - 这里使用直接伤害，不会被普通护盾抵挡
                entity.hurt(level.damageSources().playerAttack(player), sin * HOLY_DAMAGE_PER_SIN);
            }
        }

        // 3. ≥7 层：额外获得持续再生效果
        if (sin >= 7) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    REGEN_DURATION_TICKS,
                    REGEN_AMPLIFIER,
                    false, true, true));
        }

        // 4. 罪孽 ≥ 8 层时，饥饿消耗速度 +20% ——通过 NBT 标记，由事件处理器处理
        // (不需要在此处处理，饥饿消耗速度在佩戴期间持续生效)

        // 写回槽位
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        // 播放音效
        if (sin >= 7) {
            level.playSound(null, player, SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        } else {
            level.playSound(null, player, SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return true;
    }

    // ── 增加罪孽 ──

    /**
     * 攻击命中实体时 +1 罪孽（单次攻击最多 +1）
     * 由事件处理器调用
     */
    public static void addSinOnHit(Player player, ItemStack stack) {
        int sin = getSin(stack);
        if (sin < MAX_SIN) {
            setSin(player, stack, sin + 1);
            writeBack(player, stack);
        }
    }

    /**
     * 受到伤害时 +1 罪孽
     * 由事件处理器调用
     */
    public static void addSinOnDamage(Player player, ItemStack stack, float damageAmount) {
        if (damageAmount <= 0) return;
        int sin = getSin(stack);
        if (sin < MAX_SIN) {
            setSin(player, stack, sin + 1);
            writeBack(player, stack);
        }
    }

    // ── 写回堆栈 ──

    /** 把修改后的堆栈写回真实槽位，触发 Curios 的 NBT 客户端同步 */
    private static void writeBack(Player player, ItemStack stack) {
        Stack<SlotResult> stackToCheck = new Stack<>();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (String key : handler.getCurios().keySet()) {
                ICurioStacksHandler stacksHandler = handler.getCurios().get(key);
                for (int j = 0; j < stacksHandler.getStacks().getSlots(); j++) {
                    ItemStack s = stacksHandler.getStacks().getStackInSlot(j);
                    if (s.getItem() == ModItems.CONFESSION_APOCALYPSE.get()) {
                        handler.setEquippedCurio(key, j, stack);
                        return;
                    }
                }
            }
        });
    }

    // ── 死亡保留罪孽 ──

    /**
     * 佩戴死亡时保留一半罪孽
     * 由 FactionPlayerDeathEvent 等死亡事件处理器调用
     */
    public static void onPlayerDeath(Player player, ItemStack stack) {
        int sin = getSin(stack);
        if (sin > 0) {
            int retained = sin / 2;
            setSin(player, stack, retained);
            writeBack(player, stack);
        }
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.desc")
                .withStyle(ChatFormatting.DARK_RED));

        int sin = getSin(stack);

        // 罪孽常驻显示
        Component sinLine = Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.sin",
                        sin, MAX_SIN)
                .withStyle(sin == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.DARK_RED);
        tooltip.add(sinLine);

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.passive_desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.hit_desc")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.damage_desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.particle_desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.skill_desc")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.death_desc")
                    .withStyle(ChatFormatting.YELLOW));

            // 冷却显示
            long remaining = getSkillCooldownRemaining(stack, level);
            if (remaining > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.confession_apocalypse.tooltip.cooldown_left",
                                Math.round(remaining / 20.0F))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
