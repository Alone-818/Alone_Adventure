package Alone818.com.alone_adventure.init;


import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.blocks.DeterrenceBannerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    // 创建一个方块注册器。
    // 第一个参数指定注册类型（方块），
    // 第二个参数指定本模组的 MODID。
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Alone_adventure.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Alone_adventure.MODID);

    // 威慑战旗方块：阻止周围 5×5 区块内派系生物刷新
    public static final RegistryObject<Block> DETERRENCE_BANNER =
            BLOCKS.register("deterrence_banner", DeterrenceBannerBlock::new);

    // 威慑战旗物品（方块物品）
    public static final RegistryObject<Item> DETERRENCE_BANNER_ITEM =
            ITEMS.register(
                    "deterrence_banner",
                    () -> new BlockItem(DETERRENCE_BANNER.get(), new Item.Properties())
            );

    // 将本类中的注册器挂载到 Mod 事件总线。
    // 只有调用此方法后，方块才会在加载阶段被真正注册到游戏中。
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

}