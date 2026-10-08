package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.blocks.DeterrenceBannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Alone_adventure.MODID);

    // 威慑战旗方块实体
    public static final RegistryObject<BlockEntityType<DeterrenceBannerBlockEntity>> DETERRENCE_BANNER =
            BLOCK_ENTITIES.register("deterrence_banner", () ->
                    BlockEntityType.Builder.of(
                            DeterrenceBannerBlockEntity::new,
                            ModBlocks.DETERRENCE_BANNER.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
