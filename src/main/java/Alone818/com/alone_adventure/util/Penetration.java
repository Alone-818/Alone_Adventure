package Alone818.com.alone_adventure.util;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;

/**
 * 穿透算法 —— 服务于攻击穿透类物品。
 *
 * <p>
 * 适用于：
 * </p>
 * <ul>
 *     <li>穿透子弹</li>
 *     <li>贯穿长矛</li>
 *     <li>穿透法术</li>
 *     <li>其他需要记录已命中实体的攻击实体</li>
 * </ul>
 *
 * <p>
 * 设计目标：
 * </p>
 * <ul>
 *     <li>不使用 UUID，降低内存占用</li>
 *     <li>不使用 HashSet，避免哈希表额外开销</li>
 *     <li>使用原始 int[] 保存 Entity#getId()</li>
 *     <li>命中数量通常较少，因此使用线性搜索</li>
 *     <li>数组只在容量不足时扩容</li>
 *     <li>无静态状态</li>
 * </ul>
 *
 * <p>
 * Entity#getId() 是当前世界会话中的实体 ID，
 * 对于短生命周期投射物非常适合。
 * </p>
 */
public final class Penetration {

    /**
     * NBT：已命中的实体 ID。
     */
    public static final String TAG_HITS = "PenHits";

    /**
     * 初始容量。
     *
     * <p>
     * 大多数子弹只会穿透少量实体，
     * 8 个槽位已经足够覆盖绝大多数情况。
     * </p>
     */
    private static final int INITIAL_CAPACITY = 8;

    /**
     * 最大安全穿透记录数量。
     *
     * <p>
     * 防止恶意或异常 NBT 导致超大数组分配。
     * </p>
     */
    private static final int MAX_RECORDS = 1024;

    private Penetration() {
    }

    /**
     * 一次穿透攻击的命中记录。
     *
     * <p>
     * 使用原始 int 数组保存实体 ID。
     * </p>
     */
    public static final class Record {

        /**
         * 已命中的实体 ID。
         */
        private int[] hitIds =
                new int[INITIAL_CAPACITY];

        /**
         * 当前有效数量。
         */
        private int size = 0;

        /**
         * 判断实体是否已经被本次攻击命中过。
         *
         * @param entityId 实体 ID
         * @return 是否已经命中
         */
        public boolean hasHit(int entityId) {

            /*
             * 线性搜索。
             *
             * 穿透实体通常只有个位数，
             * 这里比 HashSet 更轻量。
             */
            for (int i = 0; i < size; i++) {

                if (hitIds[i] == entityId) {
                    return true;
                }
            }

            return false;
        }

        /**
         * 记录一次命中。
         *
         * <p>
         * 已经记录过的实体不会重复加入。
         * </p>
         *
         * @param entityId 实体 ID
         */
        public void record(int entityId) {

            /*
             * 防止同一个实体重复记录。
             */
            if (hasHit(entityId)) {
                return;
            }

            /*
             * 防止异常数据无限扩容。
             */
            if (size >= MAX_RECORDS) {
                return;
            }

            /*
             * 容量不足时扩容。
             */
            if (size >= hitIds.length) {

                int newCapacity =
                        Math.min(
                                MAX_RECORDS,
                                hitIds.length << 1
                        );

                /*
                 * 理论上不会发生，
                 * 但防止容量已经达到最大值。
                 */
                if (newCapacity <= hitIds.length) {
                    return;
                }

                hitIds =
                        Arrays.copyOf(
                                hitIds,
                                newCapacity
                        );
            }

            hitIds[size++] =
                    entityId;
        }

        /**
         * 当前已经命中的实体数量。
         *
         * @return 命中数量
         */
        public int count() {
            return size;
        }

        /**
         * 判断穿透能力是否已经耗尽。
         *
         * <p>
         * 语义保持原设计：
         * </p>
         *
         * <pre>
         * maxPenetration = 0
         * 第一个实体命中后 size = 1
         * 1 > 0
         * 因此命中后结束。
         * </pre>
         *
         * <p>
         * 例如：
         * </p>
         *
         * <pre>
         * maxPenetration = 2
         *
         * 第 1 个实体：继续
         * 第 2 个实体：继续
         * 第 3 个实体：结束
         * </pre>
         *
         * @param maxPenetration 最大可穿透实体数量
         * @return 是否已经耗尽
         */
        public boolean exhausted(
                int maxPenetration
        ) {

            return size > maxPenetration;
        }

        /**
         * 计算当前穿透后的伤害倍率。
         *
         * <p>
         * 每命中一个实体，
         * 伤害降低 decayPerHit。
         * </p>
         *
         * @param decayPerHit 每次穿透后的伤害衰减
         * @param minFactor 最低伤害倍率
         * @return 当前伤害倍率
         */
        public float damageFactor(
                float decayPerHit,
                float minFactor
        ) {

            /*
             * 防止传入非法参数。
             */
            decayPerHit =
                    Math.max(
                            0.0F,
                            decayPerHit
                    );

            minFactor =
                    Math.max(
                            0.0F,
                            Math.min(
                                    1.0F,
                                    minFactor
                            )
                    );

            return Math.max(
                    minFactor,
                    1.0F
                            - size * decayPerHit
            );
        }

        /**
         * 清空命中记录。
         *
         * <p>
         * 保留数组容量，避免重新分配内存。
         * </p>
         */
        public void clear() {

            /*
             * 不需要 new 新数组。
             *
             * 只需要清空有效长度即可。
             */
            size = 0;
        }

        /**
         * 将命中记录保存到 NBT。
         */
        public void save(
                CompoundTag tag
        ) {

            /*
             * 没有命中记录时直接保存空数组。
             */
            if (size <= 0) {

                tag.putIntArray(
                        TAG_HITS,
                        new int[0]
                );

                return;
            }

            /*
             * 只保存有效区域，
             * 不保存数组剩余容量。
             */
            tag.putIntArray(
                    TAG_HITS,
                    Arrays.copyOf(
                            hitIds,
                            size
                    )
            );
        }

        /**
         * 从 NBT 恢复命中记录。
         *
         * <p>
         * 会限制最大读取数量，
         * 防止异常 NBT 造成超大内存分配。
         * </p>
         */
        public void load(
                CompoundTag tag
        ) {

            int[] loaded =
                    tag.getIntArray(
                            TAG_HITS
                    );

            /*
             * 没有数据。
             */
            if (loaded.length == 0) {

                size = 0;

                if (hitIds.length
                        != INITIAL_CAPACITY) {

                    hitIds =
                            new int[
                                    INITIAL_CAPACITY
                                    ];
                }

                return;
            }

            /*
             * 限制最大读取数量。
             */
            int loadedSize =
                    Math.min(
                            loaded.length,
                            MAX_RECORDS
                    );

            /*
             * 至少保留初始容量。
             */
            int capacity =
                    Math.max(
                            INITIAL_CAPACITY,
                            loadedSize
                    );

            /*
             * 如果现有数组足够，
             * 直接复用。
             */
            if (hitIds.length < capacity) {

                hitIds =
                        new int[capacity];
            }

            /*
             * 复制有效数据。
             */
            System.arraycopy(
                    loaded,
                    0,
                    hitIds,
                    0,
                    loadedSize
            );

            size =
                    loadedSize;
        }
    }
}