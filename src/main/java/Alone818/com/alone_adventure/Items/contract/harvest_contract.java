package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.item.Rarity;

/**
 * 收割契约 - 以丰收之种子换取丰收的庆祝。
 *
 * <b>合成配方</b>：
 * <pre>
 * {
 *   "type": "minecraft:crafting_shaped",
 *   "pattern": [
 *     " B ",
 *     " W ",
 *     " F ",
 *     " M "
 *   ],
 *   "key": {
 *     "B": {"item": "minecraft:diamond", "count": 4},
 *     "W": {"item": "minecraft:carrots", "count": 8},
 *     "F": {"item": "minecraft:potatoes", "count": 8},
 *     "M": {"type": "forge:partial_nbt",
 *           "item": "alone_adventure:evil_contract",
 *           "nbt": "{QuestDone:1b}"},
 *     " ": {"item": "minecraft:oak_logs"}
 *   },
 *   "result": {
 *     "item": "alone_adventure:harvest_contract"
 *   }
 * }</pre>
 *
 * <b>完成条件</b>：
 * <ul>
 *   <li>提交 32 个胡萝卜</li>
 *   <li>提交 32 个土豆</li>
 *   <li>提交 16 个小麦</li>
 * </ul>
 *
 * 完成所有任务后，才能作为合成材料使用。
 */
public class harvest_contract extends QuestItem {

    public harvest_contract() {
        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE),
                // 任务列表：提交 32 胡萝卜 + 32 土豆 + 16 小麦
                QuestItem.CollectTask.of("collect_carrots", net.minecraft.world.item.Items.CARROT, 32),
                QuestItem.CollectTask.of("collect_potatoes", net.minecraft.world.item.Items.POTATO, 32),
                QuestItem.CollectTask.of("collect_wheat", net.minecraft.world.item.Items.WHEAT, 16)
        );
    }
}