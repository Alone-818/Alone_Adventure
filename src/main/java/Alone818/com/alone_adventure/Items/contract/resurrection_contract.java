package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * 复生契约 - 以黄金供物换取复活之约。
 *
 * <b>合成配方</b>：
 * <pre>
 * {
 *   "type": "minecraft:crafting_shaped",
 *   "pattern": [
 *     "GDG",
 *     "AEA",
 *     "GDG"
 *   ],
 *   "key": {
 *     "G": {"item": "minecraft:diamond"},
 *     "D": {"item": "minecraft:golden_apple"},
 *     "E": {"item": "minecraft:ender_eye"},
 *     "A": {"item": "minecraft:gold_block"}
 *   },
 *   "result": {
 *     "item": "alone_adventure:resurrection_contract"
 *   }
 * }</pre>
 *
 * <b>完成条件</b>：
 * <ul>
 *   <li>提交 4 个金块</li>
 *   <li>提交 16 个金苹果</li>
 *   <li>提交 2 个不死图腾</li>
 * </ul>
 *
 * 完成所有任务后，才能作为合成材料使用。
 */
public class resurrection_contract extends QuestItem {

    public resurrection_contract() {
        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE),
                // 任务列表：提交金块 x4 + 提交金苹果 x16 + 提交不死图腾 x2
                QuestItem.CollectTask.of("collect_golden_ingots",Items.GOLD_BLOCK,4),
                QuestItem.CollectTask.of("collect_golden_apple", Items.GOLDEN_APPLE, 16),
                QuestItem.CollectTask.of("collect_totem", Items.TOTEM_OF_UNDYING, 2)
        );
    }
}