package Alone818.com.alone_adventure.entity;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AmmoBoxEntity extends Entity {

    // =========================================================
    // 基础设置
    // =========================================================

    public static final int DEFAULT_AMMO = 32;

    /**
     * 弹药箱存在时间：
     *
     * 60 秒
     * 60 × 20 tick = 1200 tick
     */
    public static final int LIFETIME_TICKS = 60 * 20;

    private static final EntityDataAccessor<Integer> AMMO =
            SynchedEntityData.defineId(
                    AmmoBoxEntity.class,
                    EntityDataSerializers.INT
            );

    /**
     * 实体生成时的世界时间。
     */
    private long spawnGameTime;

    /**
     * 上一次更新时间。
     *
     * 用来避免每 tick 都重新设置 CustomName。
     */
    private long lastDisplaySecond = -1L;

    public AmmoBoxEntity(
            EntityType<? extends AmmoBoxEntity> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );

        this.spawnGameTime =
                level.getGameTime();

        this.setNoGravity(true);

        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {

        this.entityData.define(
                AMMO,
                DEFAULT_AMMO
        );
    }

    // =========================================================
    // 生命周期
    // =========================================================

    @Override
    public void tick() {

        super.tick();

        /*
         * =====================================================
         * 服务端生命周期判定
         * =====================================================
         *
         * 客户端绝对不负责删除实体。
         */
        if (!level().isClientSide) {

            /*
             * -------------------------------------------------
             * 超时消失
             * -------------------------------------------------
             */
            if (
                    level().getGameTime()
                            - spawnGameTime
                            >= LIFETIME_TICKS
            ) {

                discard();

                return;
            }

            /*
             * -------------------------------------------------
             * 弹药耗尽
             * -------------------------------------------------
             */
            if (getAmmo() <= 0) {

                discard();

                return;
            }

            /*
             * -------------------------------------------------
             * 更新显示文字
             * -------------------------------------------------
             */
            updateDisplayName();
        }
    }

    // =========================================================
    // 显示名称
    // =========================================================

    /**
     * 更新弹药箱头顶显示。
     *
     * 最终显示效果：
     *
     * 弹药箱 32/32 | 60s
     *
     * 例如：
     *
     * 弹药箱 18/32 | 37s
     */
    private void updateDisplayName() {

        long remainingSeconds = getRemainingSeconds();

        /*
         * =====================================================
         * 第一行
         * =====================================================
         *
         * 弹药箱
         */
        Component title = Component.translatable(
                "entity.alone_adventure.ammo_box"
        );

        /*
         * =====================================================
         * 第二行
         * =====================================================
         *
         * 弹药：32/32
         */
        Component ammoText = Component.translatable(
                "entity.alone_adventure.ammo_box_ammo",
                getAmmo(),
                DEFAULT_AMMO
        );

        /*
         * =====================================================
         * 第三行
         * =====================================================
         *
         * 剩余时间：60s
         */
        Component timeText = Component.translatable(
                "entity.alone_adventure.ammo_box_time",
                remainingSeconds
        );

        /*
         * =====================================================
         * 拼接三行
         * =====================================================
         */
        Component displayName = Component.literal("")
                .append(title)
                .append("\n")
                .append(ammoText)
                .append("\n")
                .append(timeText);

        this.setCustomName(displayName);
        this.setCustomNameVisible(true);

        /*
         * 记录当前显示状态
         */
        lastDisplaySecond = remainingSeconds;
    }

    // =========================================================
    // 生命周期时间
    // =========================================================

    /**
     * 设置实体生成时间。
     */
    public void setSpawnGameTime(
            long gameTime
    ) {

        this.spawnGameTime =
                gameTime;

        /*
         * 时间改变后强制下一次重新更新显示。
         */
        this.lastDisplaySecond = -1L;
    }

    /**
     * 获取实体生成时间。
     */
    public long getSpawnGameTime() {

        return spawnGameTime;
    }

    /**
     * 获取剩余生命周期 tick。
     *
     * 例如：
     *
     * 刚生成：
     * 1200
     *
     * 30 秒后：
     * 600
     *
     * 60 秒后：
     * 0
     */
    public long getRemainingLifetime() {

        if (level() == null) {
            return LIFETIME_TICKS;
        }

        long elapsed =
                level().getGameTime()
                        - spawnGameTime;

        return Math.max(
                0L,
                LIFETIME_TICKS - elapsed
        );
    }

    /**
     * 获取剩余秒数。
     *
     * 例如：
     *
     * 1200 tick → 60 秒
     * 600 tick  → 30 秒
     * 20 tick   → 1 秒
     * 0 tick    → 0 秒
     */
    public long getRemainingSeconds() {

        return Math.max(
                0L,
                (getRemainingLifetime() + 19L) / 20L
        );
    }

    // =========================================================
    // 弹药
    // =========================================================

    public int getAmmo() {

        return Math.max(
                0,
                this.entityData.get(AMMO)
        );
    }

    public void setAmmo(
            int amount
    ) {

        this.entityData.set(
                AMMO,
                Math.max(
                        0,
                        Math.min(
                                DEFAULT_AMMO,
                                amount
                        )
                )
        );

        /*
         * 弹药数量发生变化，
         * 强制下一次更新显示。
         */
        this.lastDisplaySecond = -1L;
    }

    public void consumeAmmo(
            int amount
    ) {

        if (amount <= 0) {
            return;
        }

        int remaining =
                Math.max(
                        0,
                        getAmmo() - amount
                );

        setAmmo(remaining);

        if (remaining <= 0) {
            discard();
        }
    }

    // =========================================================
    // 玩家交互
    // =========================================================

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {

        /*
         * 客户端只负责交互预测。
         */
        if (level().isClientSide) {

            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {

            return InteractionResult.PASS;
        }

        ItemStack heldStack =
                player.getItemInHand(hand);

        /*
         * 必须手持枪械。
         */
        if (
                !(heldStack.getItem()
                        instanceof GunItem gun)
        ) {

            return InteractionResult.PASS;
        }

        /*
         * 获取枪械对应的弹药类型。
         */
        Item ammoType =
                gun.getAmmoBoxAmmo(
                        heldStack
                );

        if (
                ammoType == null
                        || ammoType
                        == net.minecraft.world.item.Items.AIR
        ) {

            return InteractionResult.PASS;
        }

        /*
         * 获取一次补充所需要的弹药数量。
         */
        int cost =
                Math.max(
                        1,
                        gun.getAmmoBoxCost(
                                heldStack
                        )
                );

        /*
         * =====================================================
         * 弹药不足
         * =====================================================
         */
        if (getAmmo() < cost) {

            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "gui.alone_adventure.ammo_box.empty"
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        /*
         * 创建弹药。
         */
        ItemStack ammoStack =
                new ItemStack(
                        ammoType,
                        cost
                );

        /*
         * =====================================================
         * 背包放不下
         * =====================================================
         */
        if (
                !serverPlayer
                        .getInventory()
                        .add(ammoStack)
        ) {

            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "gui.alone_adventure.ammo_box.inventory_full"
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        /*
         * =====================================================
         * 消耗弹药箱弹药
         * =====================================================
         */
        consumeAmmo(cost);

        /*
         * =====================================================
         * 拾取音效
         * =====================================================
         */
        level().playSound(
                null,
                blockPosition(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );

        /*
         * =====================================================
         * 提示
         * =====================================================
         */
        serverPlayer.displayClientMessage(
                Component.translatable(
                        "gui.alone_adventure.ammo_box.refilled",
                        ammoStack.getHoverName(),
                        cost,
                        getAmmo()
                ),
                true
        );

        return InteractionResult.CONSUME;
    }

    // =========================================================
    // NBT 保存
    // =========================================================

    @Override
    protected void readAdditionalSaveData(
            net.minecraft.nbt.CompoundTag tag
    ) {

        setAmmo(
                tag.getInt("Ammo")
        );

        if (
                tag.contains(
                        "SpawnGameTime"
                )
        ) {

            spawnGameTime =
                    tag.getLong(
                            "SpawnGameTime"
                    );

        } else {

            spawnGameTime =
                    level().getGameTime();
        }

        /*
         * 读档后强制刷新显示。
         */
        lastDisplaySecond = -1L;
    }

    @Override
    protected void addAdditionalSaveData(
            net.minecraft.nbt.CompoundTag tag
    ) {

        tag.putInt(
                "Ammo",
                getAmmo()
        );

        tag.putLong(
                "SpawnGameTime",
                spawnGameTime
        );
    }

    // =========================================================
    // 实体属性
    // =========================================================

    @Override
    public boolean isPickable() {

        return true;
    }

    @Override
    public boolean isPushable() {

        return false;
    }

    @Override
    public boolean canBeCollidedWith() {

        return true;
    }

    @Override
    protected boolean canAddPassenger(
            Entity passenger
    ) {

        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {

        return true;
    }

    // =========================================================
    // 显示名称
    // =========================================================

    @Override
    public Component getDisplayName() {

        return Component.translatable(
                "entity.alone_adventure.ammo_box"
        );
    }
}