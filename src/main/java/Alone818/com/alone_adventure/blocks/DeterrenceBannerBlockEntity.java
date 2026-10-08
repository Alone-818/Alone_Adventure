package Alone818.com.alone_adventure.blocks;

import Alone818.com.alone_adventure.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 威慑战旗方块实体。
 *
 * 用于标记威慑范围，阻止派系生物在周围 5×5 区块内自然生成。
 */
public class DeterrenceBannerBlockEntity extends BlockEntity {

    public DeterrenceBannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DETERRENCE_BANNER.get(), pos, state);
    }

    /**
     * 方块实体更新逻辑。
     *
     * 目前威慑功能通过在 FactionSpawnEvent 中检查威慑标记实现，
     * 此处保留 tick 方法供未来扩展。
     *
     * @param level 关卡
     * @param pos   方块位置
     */
    public void tick(ServerLevel level, BlockPos pos) {
        // 威慑功能由 FactionSpawnEvent.onFinalizeSpawn 统一处理
        // 此处无需额外逻辑
    }
}
