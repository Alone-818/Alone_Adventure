package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.entity.AmmoBoxEntity;
import Alone818.com.alone_adventure.init.ModEntities;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

public class AmmoBoxItem extends Item {

    public AmmoBoxItem(Properties properties) {
        super(
                properties.stacksTo(16)
        );
    }

    /**
     * =========================================================
     * 右键方块
     * =========================================================
     *
     * 在点击的方块旁边放置弹药箱。
     */
    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {

        Level level =
                context.getLevel();

        Player player =
                context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        /*
         * 客户端只进行预测。
         */
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        /*
         * 获取放置位置。
         */
        BlockPos pos =
                context.getClickedPos()
                        .relative(
                                context.getClickedFace()
                        );

        double x =
                pos.getX() + 0.5D;

        double y =
                pos.getY();

        double z =
                pos.getZ() + 0.5D;

        /*
         * =====================================================
         * 碰撞检查
         * =====================================================
         */
        AABB box =
                new AABB(
                        x - 0.4D,
                        y,
                        z - 0.4D,
                        x + 0.4D,
                        y + 0.5D,
                        z + 0.4D
                );

        if (!level.noCollision(box)) {
            return InteractionResult.FAIL;
        }

        /*
         * =====================================================
         * 创建实体
         * =====================================================
         */
        AmmoBoxEntity entity =
                new AmmoBoxEntity(
                        ModEntities.AMMO_BOX.get(),
                        level
                );

        /*
         * 设置实体位置。
         */
        entity.setPos(
                x,
                y,
                z
        );

        /*
         * =====================================================
         * 设置生命周期开始时间
         * =====================================================
         *
         * 由 AmmoBoxEntity 自己负责计算剩余时间。
         */
        entity.setSpawnGameTime(
                level.getGameTime()
        );

        /*
         * 加入世界。
         */
        level.addFreshEntity(entity);

        /*
         * =====================================================
         * 消耗物品
         * =====================================================
         */
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * =========================================================
     * 对空气右键
     * =========================================================
     *
     * 在玩家前方放置弹药箱。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        /*
         * 客户端只预测。
         */
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        /*
         * =====================================================
         * 玩家视线方向
         * =====================================================
         */
        var look =
                player.getLookAngle();

        /*
         * 在玩家前方约 1.5 格生成。
         */
        double x =
                player.getX()
                        + look.x * 1.5D;

        double y =
                player.getEyeY()
                        - 0.35D
                        + look.y * 1.5D;

        double z =
                player.getZ()
                        + look.z * 1.5D;

        /*
         * =====================================================
         * 碰撞检查
         * =====================================================
         */
        AABB box =
                new AABB(
                        x - 0.4D,
                        y,
                        z - 0.4D,
                        x + 0.4D,
                        y + 0.5D,
                        z + 0.4D
                );

        if (!level.noCollision(box)) {
            return InteractionResultHolder.fail(stack);
        }

        /*
         * =====================================================
         * 创建弹药箱
         * =====================================================
         */
        AmmoBoxEntity entity =
                new AmmoBoxEntity(
                        ModEntities.AMMO_BOX.get(),
                        level
                );

        /*
         * 设置位置。
         */
        entity.setPos(
                x,
                y,
                z
        );

        /*
         * =====================================================
         * 设置生命周期开始时间
         * =====================================================
         */
        entity.setSpawnGameTime(
                level.getGameTime()
        );

        /*
         * 加入世界。
         */
        level.addFreshEntity(entity);

        /*
         * =====================================================
         * 消耗弹药箱物品
         * =====================================================
         */
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }
}