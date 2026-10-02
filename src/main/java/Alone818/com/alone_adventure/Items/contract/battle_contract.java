package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Rarity;


public class battle_contract extends QuestItem {

    public battle_contract() {
        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE),

                // 击杀任务
                QuestItem.KillTask.of(
                        "kill_zombie",
                        EntityType.ZOMBIE,
                        10
                ),

                QuestItem.KillTask.of(
                        "kill_skeleton",
                        EntityType.SKELETON,
                        7
                ),

                QuestItem.KillTask.of(
                        "kill_creeper",
                        EntityType.CREEPER,
                        5
                )
        );
    }
}