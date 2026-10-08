package Alone818.com.alone_adventure.blocks;

import Alone818.com.alone_adventure.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 威慑战旗方块。
 *
 * 作用：以当前区块为中心，周围 5×5 的区块（共 25 个区块）内
 * 无法刷新派系生物。
 *
 * 合成配方：
 * ┌─────────────────────────────┐
 * │ 骨头 │ 骷髅头 │ 骨头       │
 * │      │        │            │
 * │ 凋零骷髅头 │ 等级 3 掉落物 │ 凋零骷髅头 │
 * │      │        │            │
 * │ 骨头 │ 骷髅头 │ 骨头       │
 * └─────────────────────────────┘
 */
public class DeterrenceBannerBlock extends net.minecraft.world.level.block.Block implements net.minecraft.world.level.block.EntityBlock {

    public DeterrenceBannerBlock() {
        super(Properties.of()
                .strength(5.0F)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeterrenceBannerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.DETERRENCE_BANNER.get());
    }

    @Nullable
    private static <T extends BlockEntity> BlockEntityTicker<T> createTickerHelper(
            BlockEntityType<T> type,
            BlockEntityType<?> expectedType
    ) {
        if (expectedType != type) {
            return null;
        }
        return (level, pos, state, blockEntity) -> {
            if (blockEntity instanceof DeterrenceBannerBlockEntity banner) {
                banner.tick((ServerLevel) level, pos);
            }
        };
    }
}
