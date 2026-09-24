package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.client.GunGeoRenderer;
import Alone818.com.alone_adventure.client.GunItemAnimation;
import Alone818.com.alone_adventure.init.ModEntities;
import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.network.GunRecoilPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.NetworkDirection;

import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用枪械模板 —— 所有枪械物品的基类。
 *
 * 操作方式：
 * 左键 —— 开火
 * 右键按住 —— 瞄准
 * R 键 / 潜行 + 右键 —— 装填
 * G 键 —— 切换弹药类型
 */
public class GunItem extends Item implements GeoItem {

    /** GeckoLib 动画实例缓存 */
    private final AnimatableInstanceCache geoCache =
            GeckoLibUtil.createInstanceCache(this);

    /** NBT：枪内当前弹药数 */
    public static final String TAG_AMMO = "GunAmmo";

    /** NBT：已装填弹药种类 */
    public static final String TAG_AMMO_TYPE = "GunAmmoType";

    /** NBT：选中的弹药种类 */
    public static final String TAG_AMMO_INDEX = "GunAmmoIndex";

    /** NBT：装填开始时间 */
    public static final String TAG_RELOAD_START = "GunReloadStart";

    /** NBT：下一次可以开火的时间 */
    public static final String TAG_NEXT_FIRE = "GunNextFire";

    /** 瞄准状态下散布减半 */
    private static final float AIM_SPREAD_FACTOR = 0.5F;

    /** 所有已注册枪械 */
    private static final List<GunItem> REGISTERED_GUNS =
            new ArrayList<>();

    protected final GunStats stats;

    public GunItem(Properties properties, GunStats stats) {
        super(properties.stacksTo(1));

        this.stats = stats;

        REGISTERED_GUNS.add(this);

        // 注册为 GeckoLib 可同步 / 可服务器触发动画的物品
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    /** 获取所有已经注册的枪械 */
    public static List<GunItem> getRegisteredGuns() {
        return REGISTERED_GUNS;
    }

    public GunStats getStats() {
        return stats;
    }

    /**
     * 判断指定手是否可以使用这把枪。
     *
     * 单手枪：主手、副手都可以。
     * 双手枪：只能主手。
     */
    public static boolean isUsableInHand(
            GunItem gun,
            InteractionHand hand
    ) {
        return gun.getStats().handedness()
                == GunStats.Handedness.ONE_HANDED
                || hand == InteractionHand.MAIN_HAND;
    }

    // =========================================================
    // GeckoLib
    // =========================================================

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * 禁止 Minecraft 原版物品挥手动画。
     *
     * 枪械的开火动作完全由 GeckoLib fire 动画负责。
     *
     * 如果不拦截这里：
     *
     * 左键
     *  ↓
     * 原版 swing
     *  +
     * GeckoLib fire
     *
     * 第一人称就会出现多余的挥手动作。
     */
    @Override
    public boolean onEntitySwing(
            ItemStack stack,
            LivingEntity entity
    ) {
        return true;
    }

    /**
     * GeckoLib 行为控制器：
     *
     * fire / reload / aim / run / idle
     */
    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar registrar
    ) {
        registrar.add(
                new AnimationController<>(
                        this,
                        "behavior",
                        0,
                        state -> GunItemAnimation.handle(state)
                )
                        .setAnimationSpeed(0.8D)
                        .triggerableAnim(
                                "fire",
                                RawAnimation.begin()
                                        .thenPlay("fire")
                        )
        );
    }

    /** 客户端注册 GeckoLib 渲染器 */
    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {
        consumer.accept(
                GunGeoRenderer.extensions(this)
        );
    }

    // =========================================================
    // 事件钩子
    // =========================================================

    /** 开火成功后调用 */
    protected void onFire(
            ServerPlayer shooter,
            ItemStack stack
    ) {
    }

    /** 开始装填后调用 */
    protected void onReloadStart(
            Player player,
            ItemStack stack
    ) {
    }

    /** 子弹击中实体 */
    public void onBulletHitEntity(
            ServerLevel level,
            BulletProjectile bullet,
            Entity hit,
            float damage
    ) {
    }

    // =========================================================
    // 弹药
    // =========================================================

    /** 获取当前选中的弹药索引 */
    public int getSelectedAmmoIndex(
            ItemStack stack
    ) {
        CompoundTag tag = stack.getTag();

        return tag != null
                ? tag.getInt(TAG_AMMO_INDEX)
                : 0;
    }

    /** 获取当前选中的弹药 */
    public Item getSelectedAmmoItem(
            ItemStack stack
    ) {
        return stats.ammoType(
                getSelectedAmmoIndex(stack)
        );
    }

    /**
     * 获取实际生效的弹药。
     *
     * 弹夹有弹：
     * 使用已经装填的弹药。
     *
     * 弹夹没弹：
     * 使用当前选中的弹药。
     */
    public Item getEffectiveAmmoItem(
            ItemStack stack
    ) {

        CompoundTag tag = stack.getTag();

        if (tag != null
                && tag.getInt(TAG_AMMO) > 0
                && tag.contains(TAG_AMMO_TYPE)) {

            Item loaded =
                    BuiltInRegistries.ITEM.get(
                            new ResourceLocation(
                                    tag.getString(TAG_AMMO_TYPE)
                            )
                    );

            if (loaded != net.minecraft.world.item.Items.AIR) {
                return loaded;
            }
        }

        return getSelectedAmmoItem(stack);
    }
    /**
     * 切换弹药类型。
     *
     * 切换逻辑：
     *
     * 1. 取消当前装填
     * 2. 将当前弹匣剩余弹药全部退回玩家背包
     * 3. 清空当前弹匣
     * 4. 切换到下一种弹药
     * 5. 开始正常装填流程
     *
     * 注意：
     * 切换后不会直接把新弹药塞进弹匣。
     * 而是使用正常的 reloadTicks / ReloadType 装填逻辑。
     */
    public void switchAmmo(
            ServerPlayer player,
            ItemStack stack
    ) {

        // =========================================================
        // 只有一种弹药，不需要切换
        // =========================================================

        if (stats.ammoTypeCount() <= 1) {
            return;
        }

        CompoundTag tag =
                stack.getOrCreateTag();

        // =========================================================
        // 取消当前装填
        // =========================================================

        tag.remove(TAG_RELOAD_START);

        // =========================================================
        // 退回当前弹匣剩余弹药
        // =========================================================

        int currentAmmo =
                tag.getInt(TAG_AMMO);

        if (currentAmmo > 0
                && tag.contains(TAG_AMMO_TYPE)) {

            String ammoId =
                    tag.getString(TAG_AMMO_TYPE);

            Item loadedAmmo =
                    BuiltInRegistries.ITEM.get(
                            new ResourceLocation(ammoId)
                    );

            // 确认弹药有效
            if (loadedAmmo
                    != net.minecraft.world.item.Items.AIR) {

                /*
                 * 创造模式：
                 *
                 * 不需要把弹药退回背包。
                 *
                 * 否则切换一次就会凭空增加物品。
                 */
                if (!player.getAbilities().instabuild) {

                    ItemStack returnedAmmo =
                            new ItemStack(
                                    loadedAmmo,
                                    currentAmmo
                            );

                    // 优先放回背包
                    boolean added =
                            player.getInventory()
                                    .add(returnedAmmo);

                    // 背包放不下就掉落
                    if (!added
                            && !returnedAmmo.isEmpty()) {

                        player.drop(
                                returnedAmmo,
                                false
                        );
                    }
                }
            }
        }

        // =========================================================
        // 清空旧弹匣
        // =========================================================

        tag.putInt(
                TAG_AMMO,
                0
        );

        tag.remove(TAG_AMMO_TYPE);

        // =========================================================
        // 切换到下一种弹药
        // =========================================================

        int next =
                (
                        getSelectedAmmoIndex(stack)
                                + 1
                )
                        % stats.ammoTypeCount();

        tag.putInt(
                TAG_AMMO_INDEX,
                next
        );

        Item newAmmoType =
                stats.ammoType(next);

        // =========================================================
        // 开始新的装填
        // =========================================================
        //
        // 这里不直接 takeAmmo()。
        //
        // 而是让 inventoryTick() 接管装填，
        // 这样会正常使用：
        //
        // reloadTicks
        // reloadSpeedMultiplier
        // ReloadType
        // =========================================================

        if (player.getAbilities().instabuild
                || countAmmo(player, newAmmoType) > 0) {

            tag.putLong(
                    TAG_RELOAD_START,
                    player.level().getGameTime()
            );

            // 装填开始音效
            player.level().playSound(
                    null,
                    player,
                    SoundEvents.CROSSBOW_LOADING_START,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            // 装填开始钩子
            onReloadStart(
                    player,
                    stack
            );

        } else {

            /*
             * 没有新弹药。
             *
             * 保持空弹匣，
             * 不进入装填状态。
             */
            tag.remove(TAG_RELOAD_START);
        }

        // =========================================================
        // 提示玩家
        // =========================================================

        player.displayClientMessage(
                Component.translatable(
                        "gui.alone_adventure.gun.ammo_switched",
                        newAmmoType.getDescription()
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                true
        );
    }
    // =========================================================
    // 右键：瞄准 / 装填
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        // 潜行 + 右键：装填
        if (player.isShiftKeyDown()) {

            if (!level.isClientSide) {
                tryReload(
                        player,
                        hand,
                        stack
                );
            }

            return InteractionResultHolder.sidedSuccess(
                    stack,
                    level.isClientSide()
            );
        }

        // 双手枪副手不能使用
        if (!isUsableInHand(this, hand)) {
            return InteractionResultHolder.pass(stack);
        }

        // 右键瞄准
        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(
            ItemStack stack
    ) {
        return 72000;
    }

    /**
     * 不使用原版 SPYGLASS / BOW 等动画。
     *
     * 枪械瞄准姿态完全由 GeckoLib aim 动画负责。
     */
    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.NONE;
    }

    // =========================================================
    // 装填
    // =========================================================

    /**
     * 开始装填。
     */
    public void tryReload(
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {

        if (!isUsableInHand(this, hand)) {
            return;
        }

        CompoundTag tag =
                stack.getOrCreateTag();

        // 已经在装填
        if (tag.contains(TAG_RELOAD_START)) {
            return;
        }

        // 弹夹已满
        if (tag.getInt(TAG_AMMO)
                >= stats.magazineSize()) {
            return;
        }

        // 没有弹药
        if (!player.getAbilities().instabuild
                && countAmmo(
                player,
                getEffectiveAmmoItem(stack)
        ) <= 0) {

            player.displayClientMessage(
                    Component.translatable(
                            "gui.alone_adventure.gun.no_ammo"
                    ).withStyle(ChatFormatting.RED),
                    true
            );

            return;
        }

        tag.putLong(
                TAG_RELOAD_START,
                player.level().getGameTime()
        );

        player.level().playSound(
                null,
                player,
                SoundEvents.CROSSBOW_LOADING_START,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        player.displayClientMessage(
                Component.translatable(
                        "gui.alone_adventure.gun.reloading"
                ).withStyle(ChatFormatting.YELLOW),
                true
        );

        onReloadStart(
                player,
                stack
        );
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected
    ) {
        // 只在服务端处理装填
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        CompoundTag tag = stack.getTag();

        // 当前没有进行装填
        if (tag == null || !tag.contains(TAG_RELOAD_START)) {
            return;
        }

        PlayerGunStats bonuses =
                GunModItems.collectAll(player);

        /*
         * 每发子弹所需的装填时间。
         *
         * 例如：
         * reloadTicks = 20
         * 就是每装一发需要 1 秒。
         */
        int reloadTicks = Math.max(
                1,
                Math.round(
                        stats.reloadTicks()
                                / Math.max(
                                0.1F,
                                bonuses.reloadSpeedMultiplier
                        )
                )
        );

        long reloadStart =
                tag.getLong(TAG_RELOAD_START);

        /*
         * 还没有到这一发的装填完成时间。
         */
        if (level.getGameTime() - reloadStart < reloadTicks) {
            return;
        }

        // =========================================================
        // 获取当前状态
        // =========================================================

        int current =
                tag.getInt(TAG_AMMO);

        int magazineSize =
                stats.magazineSize();

        /*
         * 已经满弹。
         */
        if (current >= magazineSize) {

            tag.remove(TAG_RELOAD_START);

            return;
        }

        /*
         * 获取实际装填的弹药类型。
         *
         * 弹夹里已有弹药：
         * 继续使用当前弹夹中的弹药类型。
         *
         * 空弹夹：
         * 使用玩家当前选择的弹药类型。
         */
        Item ammoType =
                getEffectiveAmmoItem(stack);

        // =========================================================
        // 弹夹式装填
        // =========================================================

        if (stats.reloadType()
                == GunStats.ReloadType.MAGAZINE) {

            int needed =
                    magazineSize - current;

            int taken =
                    takeAmmo(
                            player,
                            ammoType,
                            needed
                    );

            if (taken > 0) {

                tag.putInt(
                        TAG_AMMO,
                        current + taken
                );

                tag.putString(
                        TAG_AMMO_TYPE,
                        BuiltInRegistries.ITEM
                                .getKey(ammoType)
                                .toString()
                );

                level.playSound(
                        null,
                        player,
                        SoundEvents.CROSSBOW_LOADING_END,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }

            /*
             * 弹夹式一次装完，
             * 所以这里结束装填。
             */
            tag.remove(TAG_RELOAD_START);

            return;
        }

        // =========================================================
        // 逐发装填
        // =========================================================

        /*
         * 一次只装一发。
         */
        int taken =
                takeAmmo(
                        player,
                        ammoType,
                        1
                );

        if (taken > 0) {

            current++;

            tag.putInt(
                    TAG_AMMO,
                    current
            );

            tag.putString(
                    TAG_AMMO_TYPE,
                    BuiltInRegistries.ITEM
                            .getKey(ammoType)
                            .toString()
            );

            level.playSound(
                    null,
                    player,
                    SoundEvents.CROSSBOW_LOADING_END,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.2F
            );

            /*
             * 这一发装好了。
             *
             * 如果弹夹已经满：
             * 停止装填。
             */
            if (current >= magazineSize) {

                tag.remove(TAG_RELOAD_START);

                return;
            }

            /*
             * 还没有装满：
             *
             * 不删除 TAG_RELOAD_START。
             *
             * 直接重新记录下一发的开始时间，
             * 让下一发继续自动装填。
             */
            tag.putLong(
                    TAG_RELOAD_START,
                    level.getGameTime()
            );

            return;
        }

        /*
         * 没有备用弹药了。
         *
         * 自动停止装填。
         */
        tag.remove(TAG_RELOAD_START);
    }
    /** 统计指定弹药数量 */
    private static int countAmmo(
            Player player,
            Item ammoItem
    ) {

        int count = 0;

        for (
                int i = 0;
                i < player.getInventory().getContainerSize();
                ++i
        ) {

            ItemStack slot =
                    player.getInventory().getItem(i);

            if (slot.is(ammoItem)) {
                count += slot.getCount();
            }
        }

        return count;
    }

    /** 消耗指定弹药 */
    private static int takeAmmo(
            Player player,
            Item ammoItem,
            int amount
    ) {

        if (amount <= 0) {
            return 0;
        }

        if (player.getAbilities().instabuild) {
            return amount;
        }

        int taken = 0;

        for (
                int i = 0;
                i < player.getInventory().getContainerSize()
                        && taken < amount;
                ++i
        ) {

            ItemStack slot =
                    player.getInventory().getItem(i);

            if (!slot.is(ammoItem)) {
                continue;
            }

            int canTake =
                    Math.min(
                            slot.getCount(),
                            amount - taken
                    );

            slot.shrink(canTake);

            taken += canTake;
        }

        return taken;
    }

    /**
     * 服务端开火入口。
     *
     * 注意：
     *
     * 这里绝对不调用 player.swing(hand)。
     *
     * GeckoLib fire 动画负责枪械开火动作。
     *
     * 特殊弹药支持：
     *
     * getAdditionalBulletCount()
     * getSpreadMultiplier()
     * getDamageMultiplier()
     * getRangeMultiplier()
     * onFired()
     */
    public boolean tryFire(
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack
    ) {

        // =====================================================
        // 双手枪只能主手
        // =====================================================

        if (!isUsableInHand(this, hand)) {
            return false;
        }

        Level level =
                player.level();

        long now =
                level.getGameTime();

        CompoundTag tag =
                stack.getOrCreateTag();

        // =====================================================
        // 射速冷却
        // =====================================================

        if (now < tag.getLong(TAG_NEXT_FIRE)) {
            return false;
        }

        // =====================================================
        // 装填中不能开火
        // =====================================================

        if (tag.contains(TAG_RELOAD_START)) {
            return false;
        }

        // =====================================================
        // 弹药耗尽
        // =====================================================

        if (!player.getAbilities().instabuild
                && tag.getInt(TAG_AMMO) <= 0) {

            return false;
        }

        // =====================================================
        // 玩家枪械属性加成
        // =====================================================

        PlayerGunStats bonuses =
                GunModItems.collectAll(player);

        // =====================================================
        // 获取实际生效弹药
        // =====================================================

        Item ammoType =
                getEffectiveAmmoItem(stack);

        AmmoItem ammo =
                ammoType instanceof AmmoItem ammoItem
                        ? ammoItem
                        : null;

        // =====================================================
        // 特殊弹药属性
        //
        // 普通弹药：
        //
        // additionalBulletCount = 0
        // spreadMultiplier      = 1.0
        // damageMultiplier      = 1.0
        // rangeMultiplier       = 1.0
        //
        // 因此不会改变普通弹药行为。
        // =====================================================

        int additionalBulletCount =
                0;

        float ammoSpreadMultiplier =
                1.0F;

        float ammoDamageMultiplier =
                1.0F;

        float ammoRangeMultiplier =
                1.0F;

        if (ammo != null) {

            additionalBulletCount =
                    Math.max(
                            0,
                            ammo.getAdditionalBulletCount(
                                    this,
                                    stack
                            )
                    );

            ammoSpreadMultiplier =
                    Math.max(
                            0.0F,
                            ammo.getSpreadMultiplier(
                                    this,
                                    stack
                            )
                    );

            ammoDamageMultiplier =
                    Math.max(
                            0.0F,
                            ammo.getDamageMultiplier(
                                    this,
                                    stack
                            )
                    );

            ammoRangeMultiplier =
                    Math.max(
                            0.0F,
                            ammo.getRangeMultiplier(
                                    this,
                                    stack
                            )
                    );
        }

        // =====================================================
        // 最终子弹数量
        //
        // 例如：
        //
        // 普通枪：
        // bulletCount = 1
        //
        // 金币弹：
        // getAdditionalBulletCount() = 9
        //
        // 最终：
        // 1 + 9 = 10 发
        // =====================================================

        int bulletCount =
                Math.max(
                        1,
                        stats.bulletCount()
                                + additionalBulletCount
                );

        // =====================================================
        // 散布
        // =====================================================

        float spreadMul =
                1.0F
                        - Mth.clamp(
                        bonuses.spreadReduction,
                        0.0F,
                        1.0F
                );

        // =====================================================
        // 瞄准状态
        // =====================================================

        boolean aiming =
                player.isUsingItem()
                        && player.getUseItem()
                        .getItem() instanceof GunItem;

        if (aiming) {
            spreadMul *= AIM_SPREAD_FACTOR;
        }

        // =====================================================
        // 应用特殊弹药散布倍率
        //
        // 例如金币弹：
        //
        // 2.0F = 散布扩大 2 倍
        // =====================================================

        spreadMul *= ammoSpreadMultiplier;

        // =====================================================
        // 子弹速度
        // =====================================================

        float speed =
                Math.max(
                        0.5F,
                        stats.bulletSpeed()
                                + bonuses.bulletSpeedBonus
                );

        // =====================================================
        // 最终伤害
        //
        // 特殊弹药倍率作用于最终枪械伤害。
        //
        // 例如毒弹：
        //
        // getDamageMultiplier() = 0.3F
        //
        // 最终：
        //
        // (基础伤害 + 玩家加成) * 0.3
        // =====================================================

        float bulletDamage =
                (
                        stats.damage()
                                + bonuses.damageBonus
                )
                        * ammoDamageMultiplier;

        // =====================================================
        // 最终射程
        //
        // 特殊弹药倍率作用于最终射程。
        // =====================================================

        float bulletRange =
                (
                        stats.effectiveRange()
                                + bonuses.rangeBonus
                )
                        * ammoRangeMultiplier;

        // =====================================================
        // 生成子弹
        // =====================================================

        for (
                int i = 0;
                i < bulletCount;
                ++i
        ) {

            BulletProjectile bullet =
                    new BulletProjectile(
                            ModEntities.BULLET.get(),
                            player,
                            level
                    );

            // =================================================
            // 水平散布
            // =================================================

            float yaw =
                    player.getYRot()
                            + (
                            level.random.nextFloat()
                                    - 0.5F
                    )
                            * 2.0F
                            * stats.horizontalOffset()
                            * spreadMul;

            // =================================================
            // 垂直散布
            // =================================================

            float pitch =
                    player.getXRot()
                            + (
                            level.random.nextFloat()
                                    - 0.5F
                    )
                            * 2.0F
                            * stats.verticalOffset()
                            * spreadMul;

            // =================================================
            // 发射
            // =================================================

            bullet.shootFromRotation(
                    player,
                    pitch,
                    yaw,
                    0.0F,
                    speed,
                    0.0F
            );

            // =================================================
            // 配置子弹
            // =================================================

            bullet.configure(
                    this,
                    ammoType,
                    bulletDamage,
                    stats.penetration()
                            + bonuses.penetrationBonus,
                    stats.ricochet()
                            + bonuses.ricochetBonus,
                    stats.knockback()
                            + bonuses.knockbackBonus,
                    bulletRange
            );


// =================================================
// 子弹实体显示
//
// 无论使用什么弹药，飞行中的实体都显示为
// ModItems.BULLET 对应的示例物品。
//
// 特殊弹药的实际效果仍然由 ammoItem + ammoData
// 保存，不影响子弹实体外观。
// =================================================

            bullet.setItem(
                    new ItemStack(
                            ModItems.BULLET.get()
                    )
            );

            // =================================================
            // 特殊弹药开火钩子
            //
            // 必须在 addFreshEntity() 之前执行。
            //
            // 例如爆炸弹会在这里写入：
            //
            // Explosive = true
            //
            // 到 bullet.getAmmoData()。
            // =================================================

            if (ammo != null) {

                ammo.onFired(
                        player,
                        stack,
                        bullet
                );
            }

            // =================================================
            // 加入世界
            // =================================================

            level.addFreshEntity(bullet);
        }

        // =====================================================
        // 消耗弹匣中的一发弹药
        // =====================================================

        if (!player.getAbilities().instabuild) {

            tag.putInt(
                    TAG_AMMO,
                    tag.getInt(TAG_AMMO) - 1
            );
        }

        // =====================================================
        // 射速冷却
        // =====================================================

        int interval =
                Math.max(
                        1,
                        Math.round(
                                stats.fireRateTicks()
                                        / Math.max(
                                        0.1F,
                                        bonuses.fireRateMultiplier
                                )
                        )
                );

        tag.putLong(
                TAG_NEXT_FIRE,
                now + interval
        );

        // =====================================================
        // 开火音效
        // =====================================================

        level.playSound(
                null,
                player,
                SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS,
                0.9F,
                0.55F
        );

        // =====================================================
        // GeckoLib 开火动画
        //
        // 只有真正成功开火才触发。
        // =====================================================

        if (level instanceof ServerLevel server) {

            triggerAnim(
                    player,
                    GeoItem.getOrAssignId(
                            stack,
                            server
                    ),
                    "behavior",
                    "fire"
            );
        }

        // =====================================================
        // 枪口粒子
        //
        // 只发送给其他玩家。
        // 开枪者自己不会收到。
        // =====================================================

        if (level instanceof ServerLevel server) {

            Vec3 look =
                    player.getLookAngle();

            Vec3 muzzle =
                    player.getEyePosition()
                            .add(
                                    look.scale(0.8D)
                            );

            for (ServerPlayer viewer : server.players()) {

                // 不发送给开枪者自己
                if (viewer == player) {
                    continue;
                }

                double dx =
                        viewer.getX() - muzzle.x;

                double dy =
                        viewer.getY() - muzzle.y;

                double dz =
                        viewer.getZ() - muzzle.z;

                double distanceSquared =
                        dx * dx
                                + dy * dy
                                + dz * dz;

                // 只发送给 32 格以内玩家
                if (distanceSquared
                        > 32.0D * 32.0D) {
                    continue;
                }

                // 烟雾
                server.sendParticles(
                        viewer,
                        ParticleTypes.SMOKE,
                        false,
                        muzzle.x,
                        muzzle.y - 0.1D,
                        muzzle.z,
                        3,
                        0.02D,
                        0.02D,
                        0.02D,
                        0.005D
                );

                // 火焰
                server.sendParticles(
                        viewer,
                        ParticleTypes.FLAME,
                        false,
                        muzzle.x,
                        muzzle.y - 0.1D,
                        muzzle.z,
                        1,
                        0.01D,
                        0.01D,
                        0.01D,
                        0.0D
                );
            }
        }

        // =====================================================
        // 后坐力
        // =====================================================

        float recoil =
                stats.recoil()
                        * (
                        1.0F
                                - Mth.clamp(
                                bonuses.recoilReduction,
                                0.0F,
                                1.0F
                        )
                );

        if (recoil > 0.0F) {
            Alone_adventure.NETWORK.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new GunRecoilPacket(recoil)
            );
        }

        // =====================================================
        // 枪械自身开火钩子
        // =====================================================

        onFire(
                player,
                stack
        );

        return true;
    }
    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        CompoundTag tag =
                stack.getTag();

        int ammo =
                tag != null
                        ? tag.getInt(TAG_AMMO)
                        : 0;

        int typeCount =
                stats.ammoTypeCount();

        if (Screen.hasShiftDown()) {

            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.gun.tooltip.usage"
                    ).withStyle(ChatFormatting.GRAY)
            );

            // 双持提示
            if (stats.handedness()
                    == GunStats.Handedness.ONE_HANDED) {

                tooltip.add(
                        Component.translatable(
                                "item.alone_adventure.gun.tooltip.dual"
                        ).withStyle(ChatFormatting.AQUA)
                );
            }

            tooltip.add(
                    Component.translatable(
                            "gun.alone_adventure.stat.line1",
                            fmt(stats.damage()),
                            fmt(stats.bulletSpeed()),
                            stats.bulletCount()
                    ).withStyle(ChatFormatting.RED)
            );

            tooltip.add(
                    Component.translatable(
                            "gun.alone_adventure.stat.line2",
                            fireModeName(stats.fireMode()),
                            String.format(
                                    "%.2f",
                                    stats.fireRateTicks() / 20.0D
                            ),
                            fmt(stats.recoil()),
                            fmt(stats.horizontalOffset()),
                            fmt(stats.verticalOffset())
                    ).withStyle(ChatFormatting.YELLOW)
            );

            tooltip.add(
                    Component.translatable(
                            "gun.alone_adventure.stat.line3",
                            stats.magazineSize(),
                            reloadTypeName(stats.reloadType()),
                            String.format(
                                    "%.1f",
                                    stats.reloadTicks() / 20.0D
                            ),
                            stats.ammoItem().getDescription()
                    ).withStyle(ChatFormatting.BLUE)
            );

            tooltip.add(
                    Component.translatable(
                            "gun.alone_adventure.stat.line4",
                            stats.penetration(),
                            stats.ricochet(),
                            fmt(stats.knockback()),
                            fmt(stats.effectiveRange()),
                            fmt(stats.aimZoom())
                    ).withStyle(ChatFormatting.GREEN)
            );

            tooltip.add(
                    Component.translatable(
                            "gun.alone_adventure.stat.line5",
                            stats.modSlots()
                    ).withStyle(ChatFormatting.GREEN)
            );

            // 持枪方式
            tooltip.add(
                    handednessName(
                            stats.handedness()
                    ).withStyle(ChatFormatting.AQUA)
            );

            // 多种弹药
            if (typeCount > 1) {

                tooltip.add(
                        Component.translatable(
                                "gun.alone_adventure.stat.ammo_types",
                                getSelectedAmmoItem(stack)
                                        .getDescription(),
                                typeCount
                        ).withStyle(ChatFormatting.BLUE)
                );
            }

        } else {

            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.gun.tooltip.usage"
                    ).withStyle(ChatFormatting.GRAY)
            );

            tooltip.add(
                    Component.translatable(
                            "tooltip.alone_adventure.press_shift"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY,
                            ChatFormatting.ITALIC
                    )
            );
        }

        // 当前弹药
        tooltip.add(
                Component.translatable(
                        "gun.alone_adventure.stat.ammo",
                        ammo,
                        stats.magazineSize(),
                        getEffectiveAmmoItem(stack)
                                .getDescription()
                ).withStyle(
                        ammo <= 0
                                ? ChatFormatting.RED
                                : ChatFormatting.WHITE
                )
        );
    }

    private static String fmt(float value) {

        return value == (long) value
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }

    private static Component fireModeName(
            GunStats.FireMode mode
    ) {

        return switch (mode) {

            case MANUAL ->
                    Component.translatable(
                            "gun.alone_adventure.fire_mode.manual"
                    );

            case SEMI_AUTO ->
                    Component.translatable(
                            "gun.alone_adventure.fire_mode.semi_auto"
                    );

            case FULL_AUTO ->
                    Component.translatable(
                            "gun.alone_adventure.fire_mode.full_auto"
                    );
        };
    }

    private static Component reloadTypeName(
            GunStats.ReloadType type
    ) {

        return switch (type) {

            case PER_BULLET ->
                    Component.translatable(
                            "gun.alone_adventure.reload_type.per_bullet"
                    );

            case MAGAZINE ->
                    Component.translatable(
                            "gun.alone_adventure.reload_type.magazine"
                    );
        };
    }

    /** 持枪方式名称 */
    private static MutableComponent handednessName(
            GunStats.Handedness handedness
    ) {

        return switch (handedness) {

            case ONE_HANDED ->
                    Component.translatable(
                            "gun.alone_adventure.handedness.one_handed"
                    );

            case TWO_HANDED ->
                    Component.translatable(
                            "gun.alone_adventure.handedness.two_handed"
                    );
        };
    }
    public Item getAmmoBoxAmmo(ItemStack stack) {
        return getEffectiveAmmoItem(stack);
    }

    /**
     * 获取弹药箱补充一发当前弹药所需要消耗的库存。
     *
     * 普通弹药：
     *     1
     *
     * 特殊弹药：
     *     2
     */
    public int getAmmoBoxCost(ItemStack stack) {

        Item ammoType =
                getAmmoBoxAmmo(stack);

        if (ammoType instanceof AmmoItem ammo) {

            return Math.max(
                    1,
                    ammo.getAmmoBoxCost()
            );
        }

        return 1;
    }
}

