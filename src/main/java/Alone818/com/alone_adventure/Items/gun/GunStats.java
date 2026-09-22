package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 通用枪械属性 —— 一把枪的全部数值（不可变，由具体枪械在注册期定义）。
 *
 * 属性一览：
 * <ul>
 *   <li>{@link #damage 子弹伤害} —— 单发子弹伤害</li>
 *   <li>{@link #bulletSpeed 子弹速度} —— 子弹飞行速度（格/tick）</li>
 *   <li>{@link #bulletCount 子弹数量} —— 一次发射射出的子弹数（霰弹 &gt; 1）</li>
 *   <li>{@link #ammo 子弹类型} —— 可装填的弹药物品</li>
 *   <li>{@link #magazineSize 弹夹容量} —— 枪内可容纳的子弹数</li>
 *   <li>{@link #horizontalOffset 子弹横向偏移} —— 射出后横向角度偏移（度，±随机）</li>
 *   <li>{@link #verticalOffset 子弹纵向偏移} —— 射出后纵向角度偏移（度，±随机）</li>
 *   <li>{@link #fireMode 开火模式} —— 手动 / 半自动</li>
 *   <li>{@link #fireRateTicks 射速} —— 两次开火的最小间隔（tick）</li>
 *   <li>{@link #recoil 后坐力} —— 每次开火枪口向上偏移的角度（度）</li>
 *   <li>{@link #reloadTicks 装填时间} —— 装填一次需要的时间（tick）</li>
 *   <li>{@link #reloadType 装填方式} —— 逐发装填 / 弹夹装填</li>
 *   <li>{@link #penetration 穿透能力} —— 子弹可以穿透的实体数（0 = 命中即停）</li>
 *   <li>{@link #ricochet 反弹能力} —— 子弹命中方块后可反弹的次数（0 = 命中即碎）</li>
 *   <li>{@link #knockback 击退能力} —— 子弹命中后的击退力度</li>
 *   <li>{@link #effectiveRange 有效射程} —— 超过该距离后子弹开始线性威力衰减（最低保留 25%）</li>
 *   <li>{@link #modSlots 改装槽位} —— 枪械改装槽位数量（供后续配件系统使用）</li>
 * </ul>
 *
 * 玩家侧加成（饰品等）不在这里，见 {@link PlayerGunStats}：开火/装填时叠加。
 */
public final class GunStats {

    /** 开火模式 */
    public enum FireMode {
        /** 手动：每次开火后需要重新上膛（射速间隔即上膛时间，通常很长） */
        MANUAL,
        /** 半自动：每按一次左键开火一次，受射速间隔限制 */
        SEMI_AUTO,
        /** 全自动：持续按住左键自动连续开火（受射速间隔限制） */
        FULL_AUTO,
    }

    /** 持枪方式 */
    public enum Handedness {
        /** 单手：主副手都可持用并开火（可双持） */
        ONE_HANDED,
        /** 双手：只在拿在主手时发挥作用（开火/瞄准/装填均不可在副手） */
        TWO_HANDED,
    }

    /** 装填方式 */
    public enum ReloadType {
        /** 逐发装填：一次装填动作只装填 1 发，装填时间即单发装填时间 */
        PER_BULLET,
        /** 弹夹装填：一次装填动作填满整个弹夹 */
        MAGAZINE,
    }

    /** 子弹伤害（点） */
    private final float damage;
    /** 子弹飞行速度（格/tick） */
    private final float bulletSpeed;
    /** 一次发射射出的子弹数量 */
    private final int bulletCount;
    /** 可装填的弹药物品列表（惰性供应，注册期未解析也安全）；≥2 种时可用 G 键切换 */
    private final java.util.List<Supplier<Item>> ammoTypes;
    /** 弹夹容量 */
    private final int magazineSize;
    /** 子弹横向角度偏移（度） */
    private final float horizontalOffset;
    /** 子弹纵向角度偏移（度） */
    private final float verticalOffset;
    /** 开火模式 */
    private final FireMode fireMode;
    /** 射速：两次开火最小间隔（tick） */
    private final int fireRateTicks;
    /** 后坐力：每次开火枪口向上偏移的角度（度） */
    private final float recoil;
    /** 装填时间（tick） */
    private final int reloadTicks;
    /** 装填方式 */
    private final ReloadType reloadType;
    /** 穿透能力：子弹可穿透的实体数 */
    private final int penetration;
    /** 反弹能力：子弹命中方块后可反弹的次数 */
    private final int ricochet;
    /** 击退能力 */
    private final float knockback;
    /** 有效射程（格）：超出后开始线性威力衰减 */
    private final float effectiveRange;
    /** 改装槽位数量 */
    private final int modSlots;
    /** 持枪方式：单手 / 双手 */
    private final Handedness handedness;
    /** 瞄准倍率：1.0 = 无缩放，2.0 = 2 倍（FOV ÷ 该值） */
    private final float aimZoom;

    private GunStats(Builder builder) {
        this.damage = builder.damage;
        this.bulletSpeed = builder.bulletSpeed;
        this.bulletCount = builder.bulletCount;
        this.ammoTypes = builder.ammoTypes;
        this.magazineSize = builder.magazineSize;
        this.horizontalOffset = builder.horizontalOffset;
        this.verticalOffset = builder.verticalOffset;
        this.fireMode = builder.fireMode;
        this.fireRateTicks = builder.fireRateTicks;
        this.recoil = builder.recoil;
        this.reloadTicks = builder.reloadTicks;
        this.reloadType = builder.reloadType;
        this.penetration = builder.penetration;
        this.ricochet = builder.ricochet;
        this.knockback = builder.knockback;
        this.effectiveRange = builder.effectiveRange;
        this.modSlots = builder.modSlots;
        this.handedness = builder.handedness;
        this.aimZoom = builder.aimZoom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float damage() {
        return damage;
    }

    public float bulletSpeed() {
        return bulletSpeed;
    }

    public int bulletCount() {
        return bulletCount;
    }

    /** 弹药物品（运行期解析，注册期调用方不应缓存） */
    public Item ammoItem() {
        return ammoType(0);
    }

    /** 支持的弹药类型数（≥2 时可用 G 键切换） */
    public int ammoTypeCount() {
        return ammoTypes.size();
    }

    /** 指定序号的弹药类型（越界回环到 0，防御旧档脏数据） */
    public Item ammoType(int index) {
        if (ammoTypes.isEmpty()) {
            return net.minecraft.world.item.Items.ARROW;
        }
        int safe = ((index % ammoTypes.size()) + ammoTypes.size()) % ammoTypes.size();
        return ammoTypes.get(safe).get();
    }

    public int magazineSize() {
        return magazineSize;
    }

    public float horizontalOffset() {
        return horizontalOffset;
    }

    public float verticalOffset() {
        return verticalOffset;
    }

    public FireMode fireMode() {
        return fireMode;
    }

    public int fireRateTicks() {
        return fireRateTicks;
    }

    public float recoil() {
        return recoil;
    }

    public int reloadTicks() {
        return reloadTicks;
    }

    public ReloadType reloadType() {
        return reloadType;
    }

    public int penetration() {
        return penetration;
    }

    /** 反弹能力：子弹命中方块后可反弹的次数 */
    public int ricochet() {
        return ricochet;
    }

    public float knockback() {
        return knockback;
    }

    public float effectiveRange() {
        return effectiveRange;
    }

    public int modSlots() {
        return modSlots;
    }

    /** 持枪方式：单手 / 双手 */
    public Handedness handedness() {
        return handedness;
    }

    /** 瞄准倍率（FOV = 基础 FOV ÷ 该值；1.0 = 无缩放） */
    public float aimZoom() {
        return aimZoom;
    }

    /** 枪械属性建造者：未设置的属性使用默认值 */
    public static final class Builder {
        private float damage = 1.0F;
        private float bulletSpeed = 3.0F;
        private int bulletCount = 1;
        private final java.util.List<Supplier<Item>> ammoTypes = new java.util.ArrayList<>();
        private int magazineSize = 1;
        private float horizontalOffset = 0.0F;
        private float verticalOffset = 0.0F;
        private FireMode fireMode = FireMode.SEMI_AUTO;
        private int fireRateTicks = 10;
        private float recoil = 1.0F;
        private int reloadTicks = 40;
        private ReloadType reloadType = ReloadType.MAGAZINE;
        private int penetration = 0;
        private int ricochet = 0;
        private float knockback = 0.5F;
        private float effectiveRange = 30.0F;
        private int modSlots = 0;
        private Handedness handedness = Handedness.ONE_HANDED;
        private float aimZoom = 2.0F;

        private Builder() {
        }

        public Builder damage(float damage) {
            this.damage = Math.max(0.0F, damage);
            return this;
        }

        public Builder bulletSpeed(float bulletSpeed) {
            this.bulletSpeed = Math.max(0.5F, bulletSpeed);
            return this;
        }

        public Builder bulletCount(int bulletCount) {
            this.bulletCount = Math.max(1, bulletCount);
            return this;
        }

        /**
         * 子弹类型：可装填的弹药物品（惰性供应）。
         * 传多个即支持多种弹药（G 键切换选择）：如步枪
         * {@code .ammo(ModItems.LONG_BULLET, ModItems.CROSSBOW_BOLT)}
         */
        @SafeVarargs
        public final Builder ammo(Supplier<Item>... types) {
            ammoTypes.clear();
            for (Supplier<Item> type : types) {
                ammoTypes.add(type);
            }
            return this;
        }

        public Builder magazineSize(int magazineSize) {
            this.magazineSize = Math.max(1, magazineSize);
            return this;
        }

        /** 子弹横向偏移（度） */
        public Builder horizontalOffset(float degrees) {
            this.horizontalOffset = Math.max(0.0F, degrees);
            return this;
        }

        /** 子弹纵向偏移（度） */
        public Builder verticalOffset(float degrees) {
            this.verticalOffset = Math.max(0.0F, degrees);
            return this;
        }

        public Builder fireMode(FireMode fireMode) {
            this.fireMode = fireMode;
            return this;
        }

        /** 射速：两次开火最小间隔（tick） */
        public Builder fireRateTicks(int ticks) {
            this.fireRateTicks = Math.max(1, ticks);
            return this;
        }

        /** 后坐力：每次开火枪口向上偏移的角度（度） */
        public Builder recoil(float degrees) {
            this.recoil = Math.max(0.0F, degrees);
            return this;
        }

        /** 装填时间（tick） */
        public Builder reloadTicks(int ticks) {
            this.reloadTicks = Math.max(1, ticks);
            return this;
        }

        public Builder reloadType(ReloadType reloadType) {
            this.reloadType = reloadType;
            return this;
        }

        /** 穿透能力：可穿透的实体数 */
        public Builder penetration(int entities) {
            this.penetration = Math.max(0, entities);
            return this;
        }

        /** 反弹能力：命中方块后可反弹的次数（0 = 命中即碎） */
        public Builder ricochet(int bounces) {
            this.ricochet = Math.max(0, bounces);
            return this;
        }

        /** 击退能力 */
        public Builder knockback(float knockback) {
            this.knockback = Math.max(0.0F, knockback);
            return this;
        }

        /** 有效射程（格） */
        public Builder effectiveRange(float range) {
            this.effectiveRange = Math.max(1.0F, range);
            return this;
        }

        /** 改装槽位数量 */
        public Builder modSlots(int slots) {
            this.modSlots = Math.max(0, slots);
            return this;
        }

        /** 持枪方式：单手（默认）/ 双手（仅主手可用） */
        public Builder handedness(Handedness handedness) {
            this.handedness = handedness;
            return this;
        }

        /** 瞄准倍率（≥1.0；2.0 = 瞄准时 FOV 减半，即 2 倍放大） */
        public Builder aimZoom(float zoom) {
            this.aimZoom = Math.max(1.0F, zoom);
            return this;
        }

        public GunStats build() {
            return new GunStats(this);
        }
    }
}
