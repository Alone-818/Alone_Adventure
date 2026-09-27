package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * 邪恶契约 - 以村民之血换取黑暗之力。
 *
 * <b>合成配方</b>：
 * <pre>
 * {
 *   "type": "minecraft:crafting_shaped",
 *   "pattern": [
 *     " D ",
 *     "VNV",
 *     " D "
 *   ],
 *   "key": {
 *     "D": {"item": "minecraft:diamond_block"},
 *     "V": {"item": "minecraft:ravager"},
 *     "N": {"item": "minecraft:nether_star"}
 *   },
 *   "result": {
 *     "item": "alone_adventure:evil_contract"
 *   }
 * }</pre>
 *
 * <b>完成条件</b>：
 * <ul>
 *   <li>击杀 3 个村民</li>
 *   <li>提交 4 个金块</li>
 *   <li>提交 4 个钻石块</li>
 *   <li>提交 16 个骨块</li>
 *   <li>提交 128 个腐肉</li>
 * </ul>
 *
 * 完成所有任务后，才能作为合成材料使用。
 */
public class evil_contract extends QuestItem {

    public evil_contract() {
        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC),
                // 任务列表：击杀村民 x3 + 提交各种材料
                QuestItem.KillTask.of("kill_villager", EntityType.VILLAGER, 3),
                QuestItem.CollectTask.of("collect_gold_block", Items.GOLD_BLOCK, 4),
                QuestItem.CollectTask.of("collect_diamond_block", Items.DIAMOND_BLOCK, 4),
                QuestItem.CollectTask.of("collect_bone_block", Items.BONE_BLOCK, 16),
                QuestItem.CollectTask.of("collect_rotten_flesh", Items.ROTTEN_FLESH, 128)
        );
    }
}
