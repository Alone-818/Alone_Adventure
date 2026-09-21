package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 连队团旗的旗子实体。
 *
 * 由 {@link regiment_banner} 在原地插下，存在 {@value #LIFETIME_TICKS} tick
 * （默认 30 秒）后自然收旗消散。
 * 存续期间每 {@value #AURA_REFRESH_TICKS} tick 刷新一次光环：
 * 半径 {@value #AURA_RADIUS} 格内的玩家获得抗性 II 与耐力 I
 * （离开范围后效果最多残留 {@value #EFFECT_DURATION_TICKS} tick）；
 * 每名玩家首次进入该旗范围时补足 {@value #ABSORPTION_AMOUNT} 点黄心
 * （每个旗子对每名玩家只生效一次，已更高则不减少）。
 * 黄心直接写入吸收值（同亡灵秘典），无持续时间、被伤害消耗为止。
 *
 * 军旗不可被攻击/推动/碰撞（/kill 等绕过无敌的来源除外）。
 */
public class RegimentBannerEntity extends Entity {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 存在时长：30 秒（600 tick） */
    public static int LIFETIME_TICKS = 600;
    /** 光环半径（格） */
    public static double AURA_RADIUS = 8.0;
    /** 首入范围补足的黄心点数 */
    public static float ABSORPTION_AMOUNT = 10.0F;

    /** 光环刷新间隔：每 1 秒（20 tick） */
    private static final int AURA_REFRESH_TICKS = 20;
    /** 光环效果的持续时间余量：离开范围后最多残留 3 秒 */
    private static final int EFFECT_DURATION_TICKS = 60;

    /** NBT 标签：剩余存在时长 */
    private static final String TAG_LIFE_TICKS = "LifeTicks";
    /** NBT 标签：已领过黄心的玩家 UUID 列表 */
    private static final String TAG_GRANTED = "GrantedAbsorption";

    private final Set<UUID> grantedAbsorption = new HashSet<>();
    private int lifeTicks = LIFETIME_TICKS;

    public RegimentBannerEntity(EntityType<? extends RegimentBannerEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        // 静态军旗不需要同步数据
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // 计时收旗
        if (--lifeTicks <= 0) {
            expire();
            return;
        }
        // 插旗当刻立即生效一次，之后每 20 tick 刷新
        if (this.tickCount == 1 || this.tickCount % AURA_REFRESH_TICKS == 0) {
            applyAura();
        }
    }

    /** 光环结算：范围内玩家刷新抗性 II / 耐力 I，首入范围补足黄心 */
    private void applyAura() {
        AABB area = getBoundingBox().inflate(AURA_RADIUS, AURA_RADIUS, AURA_RADIUS);
        for (Player player : level().getEntitiesOfClass(Player.class, area)) {
            if (player.isSpectator()) continue;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    EFFECT_DURATION_TICKS, 1, true, true, true));
            player.addEffect(new MobEffectInstance(ModEffects.ENDURANCE.get(),
                    EFFECT_DURATION_TICKS, 0, true, true, true));
            // 10 点黄心只给一次：领过就不再补
            if (grantedAbsorption.add(player.getUUID())) {
                if (player.getAbsorptionAmount() < ABSORPTION_AMOUNT) {
                    player.setAbsorptionAmount(ABSORPTION_AMOUNT);
                }
            }
        }
    }

    /** 存在时长耗尽：布屑消散 */
    private void expire() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + 0.8D, getZ(), 8, 0.3D, 0.4D, 0.3D, 0.02D);
        }
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
        discard();
    }

    // ===== 军旗不可交互 =====

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 免疫一切攻击；仅放行 /kill 等绕过无敌的来源
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    // ===== 存档 =====

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(TAG_LIFE_TICKS, lifeTicks);
        ListTag list = new ListTag();
        for (UUID id : grantedAbsorption) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put(TAG_GRANTED, list);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTicks = Math.max(1, tag.getInt(TAG_LIFE_TICKS));
        this.grantedAbsorption.clear();
        ListTag list = tag.getList(TAG_GRANTED, Tag.TAG_STRING);
        for (Tag entry : list) {
            try {
                grantedAbsorption.add(UUID.fromString(entry.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // 旧档损坏的 UUID 直接跳过
            }
        }
    }
}
