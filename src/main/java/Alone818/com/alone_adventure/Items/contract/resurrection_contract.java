package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * 复生契约 - 以唤魔者（Evoker）之力换取复活之约。
 *
 * <b>合成配方</b>：
 * <pre>
 * {
 *   "type": "minecraft:crafting_shaped",
 *   "pattern": [
 *     "AGA",
 *     "GDG",
 *     "AGA"
 *   ],
 *   "key": {
 *     "A": {"item": "minecraft:golden_apple"},
 *     "D": {"item": "minecraft:diamond"},
 *     "G": {"item": "minecraft:ender_eye"}
 *   },
 *   "result": {
 *     "item": "alone_adventure:revive_contract"
 *   }
 * }</pre>
 *
 * <b>完成条件</b>：
 * <ul>
 *   <li>提交 16 个金苹果</li>
 *   <li>提交 2 个不死图腾</li>
 *   <li>击杀 2 个唤魔者</li>
 * </ul>
 *
 * 完成所有任务后，才能作为合成材料使用。
 */
public class resurrection_contract extends QuestItem {

    public resurrection_contract() {
        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE),
                // 任务列表：击杀唤魔者 x2 + 提交金苹果 x16 + 提交不死图腾 x2
                QuestItem.KillTask.of("kill_evoker", EntityType.EVOKER, 2),
                QuestItem.CollectTask.of("collect_golden_apple", Items.GOLDEN_APPLE, 16),
                QuestItem.CollectTask.of("collect_totem", Items.TOTEM_OF_UNDYING, 2)
        );
    }
}