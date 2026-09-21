package Alone818.com.alone_adventure.util;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;

/**
 * 穿透算法 —— 服务于攻击穿透类物品（穿透子弹、贯穿长矛等）。
 *
 * <b>节省内存的设计</b>：
 * <ul>
 *   <li>命中记录用<b>原始 int 数组</b>（实体 ID，每实体 4 字节），
 *       而非 {@code Set<UUID>}（每实体 32+ 字节 UUID 对象 + 哈希表项开销，10 倍以上差距）；</li>
 *   <li>穿透命中的实体数天然很少（个位数），线性查找比哈希更快；数组按需扩容（翻倍）；</li>
 *   <li>无静态状态，记录随宿主实体存在；持久化用单个 IntArray NBT 标签</li>
 * </ul>
 *
 * 注意：实体 ID（{@code Entity#getId()}）是会话内唯一的世界序号，
 * 适合短命投射物；跨存档不保证一致（重载后记录作废，投射物通常活不过一次存档，可忽略）。
 *
 * 典型用法（投射物每 tick）：
 * <pre>{@code
 * // 命中判定：跳过已穿透过的实体
 * if (!hitRecord.hasHit(target.getId())) {
 *     hitRecord.record(target.getId());
 *     float damage = baseDamage * hitRecord.damageFactor(0.2F, 0.3F); // 每穿透衰减
 *     target.hurt(source, damage);
 *     if (hitRecord.exhausted(maxPenetration)) discard();
 * }
 * }</pre>
 */
public final class Penetration {

    /** NBT 键：已命中实体 ID 数组 */
    public static final String TAG_HITS = "PenHits";

    private Penetration() {
    }

    /** 一次穿透攻击的命中记录（原始 int 数组，省内存） */
    public static final class Record {

        /** 已命中实体 ID（容量随需翻倍，初始 8） */
        private int[] hitIds = new int[8];
        /** 有效长度 */
        private int size = 0;

        /** 该实体是否已被本次穿透命中过 */
        public boolean hasHit(int entityId) {
            for (int i = 0; i < size; ++i) {
                if (hitIds[i] == entityId) return true;
            }
            return false;
        }

        /** 记录一次命中（已记录过则忽略） */
        public void record(int entityId) {
            if (hasHit(entityId)) return;
            if (size == hitIds.length) {
                hitIds = Arrays.copyOf(hitIds, size * 2);
            }
            hitIds[size++] = entityId;
        }

        /** 已命中实体数 */
        public int count() {
            return size;
        }

        /**
         * 是否已耗尽穿透能力：已命中实体数超过可穿透实体数
         * （maxPenetration = 0 时首个实体即耗尽，即命中即停）。
         */
        public boolean exhausted(int maxPenetration) {
            return size > maxPenetration;
        }

        /**
         * 穿透伤害衰减：已命中 n 个实体后的伤害倍率。
         * 每穿透一个实体 −decayPerHit（0.2 = 每个目标伤害 -20%），最低保留 minFactor。
         */
        public float damageFactor(float decayPerHit, float minFactor) {
            return Math.max(minFactor, 1.0F - size * decayPerHit);
        }

        /** 持久化到宿主 NBT */
        public void save(CompoundTag tag) {
            tag.putIntArray(TAG_HITS, Arrays.copyOf(hitIds, size));
        }

        /** 从宿主 NBT 恢复（缺键时为空记录） */
        public void load(CompoundTag tag) {
            int[] loaded = tag.getIntArray(TAG_HITS);
            hitIds = Arrays.copyOf(loaded, Math.max(8, loaded.length));
            size = loaded.length;
        }
    }
}
