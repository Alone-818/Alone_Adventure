package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.client.GunGeoRenderer;
import Alone818.com.alone_adventure.client.GunItemAnimation;
import Alone818.com.alone_adventure.init.ModEntities;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用枪械模板 —— 所有枪械物品的基类。
 *
 * <b>操作方式</b>：
 * <ul>
 *   <li>左键 —— 开火（客户端拦截攻击键并发包 {@code GunFirePacket}，服务端结算）；
 *       双持两把枪时按住左键 —— 交替开火（射速每把枪独立）</li>
 *   <li>右键按住 —— 瞄准（使用物品，客户端按 {@link GunStats#aimZoom()} 做 FOV 缩放，散布减半）</li>
 *   <li>R 键 / 潜行 + 右键 —— 装填（主副手所有持枪同时开始装填）</li>
 *   <li>G 键 —— 切换弹药类型（主手枪支持 ≥2 种弹药时循环切换）</li>
 * </ul>
 *
 * <b>弹药</b>：弹夹内弹药数存 {@value #TAG_AMMO}，<b>已装填的弹药种类</b>存
 * {@value #TAG_AMMO_TYPE}（注册名），<b>选中的弹药种类</b>存 {@value #TAG_AMMO_INDEX}：
 * 弹夹内还有弹时开火/续装使用已装填的种类；打空后装填使用选中的种类
 * （G 切换只影响下一轮装填，不吞没已装填的弹药）。
 * 弹药消耗品为 {@link AmmoItem} 子类（长子弹/霰弹/短子弹/弩箭弹药），
 * 特种弹药经 {@link AmmoItem#onFired} 等钩子扩展。
 *
 * <b>属性</b>：全部由构造期给定的 {@link GunStats} 定义；开火/装填时叠加
 * {@link PlayerGunStats}（玩家加成接口，默认无加成）。
 *
 * <b>射速冷却</b>：记在枪械自身 NBT（{@value #TAG_NEXT_FIRE}，游戏刻绝对时刻），
 * <b>每把枪独立计算</b>——双持两把同款枪也可交替开火（原版物品冷却按物品类型共享，无法胜任）。
 *
 * 新枪械接入：继承本类并在构造器传入 {@link GunStats#builder()} 构建的属性即可
 * （参考 rifle / shotgun / pistol）。
 *
 * <b>模型与动画（GeckoLib）</b>：枪械渲染走 Blockbench（基岩格式）模型管线——
 * 几何模型 {@code geo/gun/<注册名>.geo.json}、贴图 {@code textures/gun/<注册名>.png}、
 * 动画按行为逐文件 {@code animations/gun/<注册名>_<行为>.animation.json}
 * （fire / reload / aim / run / idle，缺文件自动降级），详见
 * {@link Alone818.com.alone_adventure.client.GunGeoModel}。
 */
public class GunItem extends Item implements GeoItem {

    /** GeckoLib 动画实例缓存（每枪一个；物品为单例，客户端渲染期使用） */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** NBT 标签：枪内当前弹药数 */
    public static final String TAG_AMMO = "GunAmmo";
    /** NBT 标签：已装填的弹药种类（注册名，如 alone_adventure:long_bullet） */
    public static final String TAG_AMMO_TYPE = "GunAmmoType";
    /** NBT 标签：选中的弹药种类（索引，对应 GunStats 的弹药列表） */
    public static final String TAG_AMMO_INDEX = "GunAmmoIndex";
    /** NBT 标签：装填开始的游戏刻 */
    public static final String TAG_RELOAD_START = "GunReloadStart";
    /** NBT 标签：下一次可开火的游戏刻（射速冷却，每把枪独立） */
    public static final String TAG_NEXT_FIRE = "GunNextFire";

    /** 瞄准状态下的散布倍率（半偏移） */
    private static final float AIM_SPREAD_FACTOR = 0.5F;

    /** 所有已构造的枪械物品（客户端显示属性注册期遍历用） */
    private static final List<GunItem> REGISTERED_GUNS = new ArrayList<>();

    protected final GunStats stats;

    public GunItem(Properties properties, GunStats stats) {
        super(properties.stacksTo(1));
        this.stats = stats;
        REGISTERED_GUNS.add(this);
    }

    /** 已注册的全部枪械（客户端显示属性注册期遍历用） */
    public static List<GunItem> getRegisteredGuns() {
        return REGISTERED_GUNS;
    }

    public GunStats getStats() {
        return stats;
    }

    /** 该枪械在指定手上是否可用（双手枪仅主手；单手枪任意手） */
    public static boolean isUsableInHand(GunItem gun, InteractionHand hand) {
        return gun.getStats().handedness() == GunStats.Handedness.ONE_HANDED
                || hand == InteractionHand.MAIN_HAND;
    }

    // ===== GeckoLib：模型渲染与动画 =====

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * 行为控制器：aim &gt; fire &gt; reload &gt; run &gt; idle 按优先级选取
     * 行为动画（单文件/逐行为文件双形式解析，缺文件降级）。
     * 谓词体（GunItemAnimation）仅客户端渲染线程执行。
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "behavior", 5,
                state -> GunItemAnimation.handle(state)));
    }

    /** 客户端：注册 GeckoLib 渲染器（Blockbench 模型管线接管枪械渲染） */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(GunGeoRenderer.extensions(this));
    }

    // ===== 事件钩子（子类按需覆写） =====

    /** 开火钩子：服务端开火成功后调用（子弹已生成、弹药与冷却已结算） */
    protected void onFire(ServerPlayer shooter, ItemStack stack) {
    }

    /** 开始装填钩子：服务端装填校验通过并开始计时后调用 */
    protected void onReloadStart(Player player, ItemStack stack) {
    }

    /**
     * 击中钩子：本枪子弹命中实体时调用（伤害结算前，damage 为经射程衰减后的值）。
     * 由 {@link BulletProjectile} 直接回调，因此为 public；子类覆写即可。
     */
    public void onBulletHitEntity(ServerLevel level, BulletProjectile bullet,
                                  Entity hit, float damage) {
    }

    // ===== 弹药种类 =====

    /** 当前选中的弹药类型索引（脏数据回环到 0） */
    public int getSelectedAmmoIndex(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_AMMO_INDEX) : 0;
    }

    /** 当前选中的弹药类型 */
    public Item getSelectedAmmoItem(ItemStack stack) {
        return stats.ammoType(getSelectedAmmoIndex(stack));
    }

    /**
     * 实际生效的弹药类型：弹夹内有弹时用已装填的种类
     * （{@value #TAG_AMMO_TYPE}），否则用选中的种类。
     * 客户端亦可调用（NBT 随持枪物品同步）。
     */
    public Item getEffectiveAmmoItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getInt(TAG_AMMO) > 0 && tag.contains(TAG_AMMO_TYPE)) {
            Item loaded = BuiltInRegistries.ITEM.get(
                    new ResourceLocation(tag.getString(TAG_AMMO_TYPE)));
            if (loaded != net.minecraft.world.item.Items.AIR) {
                return loaded;
            }
        }
        return getSelectedAmmoItem(stack);
    }

    /**
     * 切换弹药类型（仅服务端，G 键经 {@code GunSwitchAmmoPacket} 调用）：
     * 支持的种类间循环切换；只影响下一轮装填，弹夹内已装填的弹药不受影响。
     */
    public void switchAmmo(ServerPlayer player, ItemStack stack) {
        if (stats.ammoTypeCount() <= 1) return; // 单一弹药类型无需切换

        int next = (getSelectedAmmoIndex(stack) + 1) % stats.ammoTypeCount();
        stack.getOrCreateTag().putInt(TAG_AMMO_INDEX, next);

        player.level().playSound(null, player, SoundEvents.CROSSBOW_LOADING_MIDDLE,
                SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(Component.translatable(
                        "gui.alone_adventure.gun.ammo_switched",
                        stats.ammoType(next).getDescription())
                .withStyle(ChatFormatting.AQUA), true);
    }

    // ===== 右键：瞄准 / 潜行右键：装填 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 潜行 + 右键：装填
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                tryReload(player, hand, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 双手枪在副手不发挥作用：直接跳过（无法瞄准）
        if (!isUsableInHand(this, hand)) {
            return InteractionResultHolder.pass(stack);
        }

        // 右键按住：举枪瞄准（客户端由 ClientGunHandler 按 GunStats.aimZoom 做 FOV 缩放）
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    /**
     * 使用动画取 NONE：第一人称照常应用手臂基座变换
     * （{@code applyItemArmTransform}），枪械保持在正常持枪位。
     * <b>不可用 SPYGLASS</b>——原版 switch 对其无分支（望远镜走
     * {@code isScoping} 全屏渲染），会导致瞄准中的枪跳到相机原点
     * 怼脸渲染（单/双持瞄准时模型巨大偏移的根因）。
     * 瞄准姿态由 aim 行为动画（{@code animations/gun/}）负责。
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // ===== 装填 =====

    /**
     * 开始装填（仅服务端）：校验手别/弹药/状态后记下开始时刻，完成结算在 inventoryTick。
     * 入口有两个：R 键（{@code GunReloadPacket}，主副手所有可用枪）与潜行 + 右键（本枪）。
     */
    public void tryReload(Player player, InteractionHand hand, ItemStack stack) {
        if (!isUsableInHand(this, hand)) return; // 双手枪在副手不发挥作用
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_RELOAD_START)) return;                        // 已在装填
        if (tag.getInt(TAG_AMMO) >= stats.magazineSize()) return;            // 已满
        if (!player.getAbilities().instabuild
                && countAmmo(player, getEffectiveAmmoItem(stack)) <= 0) {  // 背包无弹药
            player.displayClientMessage(
                    Component.translatable("gui.alone_adventure.gun.no_ammo")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        tag.putLong(TAG_RELOAD_START, player.level().getGameTime());
        player.level().playSound(null, player, SoundEvents.CROSSBOW_LOADING_START,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("gui.alone_adventure.gun.reloading")
                .withStyle(ChatFormatting.YELLOW), true);

        onReloadStart(player, stack);
    }

    /** 服务端背包 tick：装填计时结束的结算（弹夹装满 / 逐发装一发） */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity,
                              int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_RELOAD_START)) return;

        PlayerGunStats bonuses = GunModItems.collectAll(player);
        int reloadTicks = Math.max(1, Math.round(
                stats.reloadTicks() / Math.max(0.1F, bonuses.reloadSpeedMultiplier)));
        if (level.getGameTime() - tag.getLong(TAG_RELOAD_START) < reloadTicks) return;

        // 装填种类：弹夹内还有弹时续装同种，否则装当前选中种类
        Item ammoType = getEffectiveAmmoItem(stack);
        int current = tag.getInt(TAG_AMMO);
        if (stats.reloadType() == GunStats.ReloadType.MAGAZINE) {
            // 弹夹装填：一次性装满
            int taken = takeAmmo(player, ammoType, stats.magazineSize() - current);
            if (taken > 0) {
                tag.putInt(TAG_AMMO, current + taken);
                tag.putString(TAG_AMMO_TYPE, BuiltInRegistries.ITEM.getKey(ammoType).toString());
                level.playSound(null, player, SoundEvents.CROSSBOW_LOADING_END,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            tag.remove(TAG_RELOAD_START);
        } else {
            // 逐发装填：一轮只装一发，需要再次触发装填继续
            if (current < stats.magazineSize() && takeAmmo(player, ammoType, 1) > 0) {
                tag.putInt(TAG_AMMO, current + 1);
                tag.putString(TAG_AMMO_TYPE, BuiltInRegistries.ITEM.getKey(ammoType).toString());
                level.playSound(null, player, SoundEvents.CROSSBOW_LOADING_END,
                        SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            tag.remove(TAG_RELOAD_START);
        }
    }

    /** 统计背包内指定弹药的数量 */
    private static int countAmmo(Player player, Item ammoItem) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.is(ammoItem)) {
                count += slot.getCount();
            }
        }
        return count;
    }

    /** 从背包消耗指定数量弹药，返回实际消耗数（创造模式不消耗但视为成功） */
    private static int takeAmmo(Player player, Item ammoItem, int amount) {
        if (amount <= 0) return 0;
        if (player.getAbilities().instabuild) return amount;

        int taken = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && taken < amount; ++i) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.is(ammoItem)) continue;
            int canTake = Math.min(slot.getCount(), amount - taken);
            slot.shrink(canTake);
            taken += canTake;
        }
        return taken;
    }

    // ===== 开火（服务端，由 GunFirePacket 调用） =====

    /**
     * 服务端开火入口：校验手别（双手枪仅主手）/射速冷却（每枪独立 NBT）/装填/弹药后，
     * 按 {@link GunStats} + {@link PlayerGunStats} 生成子弹并反馈后坐力。
     * 子弹携带弹药种类（渲染为对应弹药）并触发弹药激发钩子（{@link AmmoItem#onFired}）。
     *
     * @return 是否成功开火（失败时客户端不会有任何表现，正确预期）
     */
    public boolean tryFire(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!isUsableInHand(this, hand)) return false; // 双手枪在副手不发挥作用
        Level level = player.level();
        long now = level.getGameTime();
        CompoundTag tag = stack.getOrCreateTag();

        // 射速冷却：记在枪械自身 NBT，每把枪独立（双持可交替开火）
        if (now < tag.getLong(TAG_NEXT_FIRE)) return false;
        if (tag.contains(TAG_RELOAD_START)) return false; // 装填中
        if (!player.getAbilities().instabuild && tag.getInt(TAG_AMMO) <= 0) return false; // 弹尽

        PlayerGunStats bonuses = GunModItems.collectAll(player);
        // 实际生效的弹药（弹夹内已装填的种类）及其激发钩子
        Item ammoType = getEffectiveAmmoItem(stack);
        AmmoItem ammo = ammoType instanceof AmmoItem ammoItem ? ammoItem : null;

        // 散布：纵向/横向偏移 × 玩家减免；瞄准（正在使用任意枪械）时再减半
        float spreadMul = 1.0F - Mth.clamp(bonuses.spreadReduction, 0.0F, 1.0F);
        boolean aiming = player.isUsingItem()
                && player.getUseItem().getItem() instanceof GunItem;
        if (aiming) {
            spreadMul *= AIM_SPREAD_FACTOR;
        }
        float speed = Math.max(0.5F, stats.bulletSpeed() + bonuses.bulletSpeedBonus);

        // 生成子弹：多弹丸各自独立随机偏移
        for (int i = 0; i < stats.bulletCount(); ++i) {
            BulletProjectile bullet = new BulletProjectile(
                    ModEntities.BULLET.get(), player, level);
            float yaw = player.getYRot()
                    + (level.random.nextFloat() - 0.5F) * 2.0F * stats.horizontalOffset() * spreadMul;
            float pitch = player.getXRot()
                    + (level.random.nextFloat() - 0.5F) * 2.0F * stats.verticalOffset() * spreadMul;
            bullet.shootFromRotation(player, pitch, yaw, 0.0F, speed, 0.0F);
            bullet.configure(this, ammoType,
                    stats.damage() + bonuses.damageBonus,
                    stats.penetration() + bonuses.penetrationBonus,
                    stats.ricochet() + bonuses.ricochetBonus,
                    stats.knockback() + bonuses.knockbackBonus,
                    stats.effectiveRange() + bonuses.rangeBonus);
            // 弹体渲染为对应弹药物品
            bullet.setItem(new ItemStack(ammoType));
            // 子弹激发钩子：特种弹药在此改写弹体（写入弹体 NBT 等）
            if (ammo != null) {
                ammo.onFired(player, stack, bullet);
            }
            level.addFreshEntity(bullet);
        }

        // 消耗弹药（创造不消耗）
        if (!player.getAbilities().instabuild) {
            tag.putInt(TAG_AMMO, tag.getInt(TAG_AMMO) - 1);
        }

        // 射速冷却：写入枪械自身 NBT（每把枪独立，玩家射速倍率加快）
        int interval = Math.max(1, Math.round(
                stats.fireRateTicks() / Math.max(0.1F, bonuses.fireRateMultiplier)));
        tag.putLong(TAG_NEXT_FIRE, now + interval);

        // 枪口反馈：音效 + 枪口烟/火花
        level.playSound(null, player, SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS, 0.9F, 0.55F);
        // 第三人称开火反馈：服务端挥手广播给其他玩家（ClientboundAnimatePacket
        // 不回发本人，第一人称不甩枪；踢枪动画由 fire 行为动画负责）
        player.swing(hand);
        if (level instanceof ServerLevel server) {
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = player.getEyePosition().add(look.scale(0.8D));
            server.sendParticles(ParticleTypes.SMOKE,
                    muzzle.x, muzzle.y - 0.1D, muzzle.z, 6, 0.02D, 0.02D, 0.02D, 0.01D);
            server.sendParticles(ParticleTypes.FLAME,
                    muzzle.x, muzzle.y - 0.1D, muzzle.z, 2, 0.01D, 0.01D, 0.01D, 0.0D);
        }

        // 后坐力：枪口上抬角度发给射手客户端（本地即时生效）
        float recoil = stats.recoil() * (1.0F - Mth.clamp(bonuses.recoilReduction, 0.0F, 1.0F));
        if (recoil > 0.0F) {
            Alone_adventure.NETWORK.sendTo(new GunRecoilPacket(recoil),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }

        onFire(player, stack);
        return true;
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        int ammo = tag != null ? tag.getInt(TAG_AMMO) : 0;
        int typeCount = stats.ammoTypeCount();

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.gun.tooltip.usage")
                    .withStyle(ChatFormatting.GRAY));
            // 双持提示仅单手枪显示（双手枪不构成双持）
            if (stats.handedness() == GunStats.Handedness.ONE_HANDED) {
                tooltip.add(Component.translatable("item.alone_adventure.gun.tooltip.dual")
                        .withStyle(ChatFormatting.AQUA));
            }
            tooltip.add(Component.translatable("gun.alone_adventure.stat.line1",
                            fmt(stats.damage()), fmt(stats.bulletSpeed()), stats.bulletCount())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("gun.alone_adventure.stat.line2",
                            fireModeName(stats.fireMode()),
                            String.format("%.2f", stats.fireRateTicks() / 20.0D),
                            fmt(stats.recoil()),
                            fmt(stats.horizontalOffset()), fmt(stats.verticalOffset()))
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("gun.alone_adventure.stat.line3",
                            stats.magazineSize(), reloadTypeName(stats.reloadType()),
                            String.format("%.1f", stats.reloadTicks() / 20.0D),
                            stats.ammoItem().getDescription())
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("gun.alone_adventure.stat.line4",
                            stats.penetration(), stats.ricochet(), fmt(stats.knockback()),
                            fmt(stats.effectiveRange()), fmt(stats.aimZoom()))
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("gun.alone_adventure.stat.line5", stats.modSlots())
                    .withStyle(ChatFormatting.GREEN));
            // 持枪方式（单手可双持 / 双手仅主手）
            tooltip.add(handednessName(stats.handedness())
                    .withStyle(ChatFormatting.AQUA));
            if (typeCount > 1) {
                tooltip.add(Component.translatable("gun.alone_adventure.stat.ammo_types",
                                getSelectedAmmoItem(stack).getDescription(), typeCount)
                        .withStyle(ChatFormatting.BLUE));
            }
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.gun.tooltip.usage")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        // 当前弹药常显（数量 + 实际生效的弹药种类）
        tooltip.add(Component.translatable("gun.alone_adventure.stat.ammo",
                        ammo, stats.magazineSize(), getEffectiveAmmoItem(stack).getDescription())
                .withStyle(ammo <= 0 ? ChatFormatting.RED : ChatFormatting.WHITE));
    }

    private static String fmt(float value) {
        return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static Component fireModeName(GunStats.FireMode mode) {
        return switch (mode) {
            case MANUAL -> Component.translatable("gun.alone_adventure.fire_mode.manual");
            case SEMI_AUTO -> Component.translatable("gun.alone_adventure.fire_mode.semi_auto");
            case FULL_AUTO -> Component.translatable("gun.alone_adventure.fire_mode.full_auto");
        };
    }

    private static Component reloadTypeName(GunStats.ReloadType type) {
        return switch (type) {
            case PER_BULLET -> Component.translatable("gun.alone_adventure.reload_type.per_bullet");
            case MAGAZINE -> Component.translatable("gun.alone_adventure.reload_type.magazine");
        };
    }

    /** 持枪方式名（单手可双持 / 双手仅主手） */
    private static MutableComponent handednessName(GunStats.Handedness handedness) {
        return switch (handedness) {
            case ONE_HANDED -> Component.translatable("gun.alone_adventure.handedness.one_handed");
            case TWO_HANDED -> Component.translatable("gun.alone_adventure.handedness.two_handed");
        };
    }
}
