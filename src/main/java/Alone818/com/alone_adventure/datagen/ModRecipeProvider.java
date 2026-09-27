
package Alone818.com.alone_adventure.datagen;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModItems;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraftforge.common.crafting.PartialNBTIngredient;

import java.util.function.Consumer;


public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {

        // =========================================================
        // 枪械盒 → 枪械
        // =========================================================

        gunBoxRecipe(
                writer,
                ModItems.RIFLE.get(),
                "rifle"
        );

        gunBoxRecipe(
                writer,
                ModItems.DOUBLE_BARREL_SHOTGUN.get(),
                "double_barrel_shotgun"
        );

        gunBoxRecipe(
                writer,
                ModItems.PISTOL.get(),
                "pistol"
        );

        gunBoxRecipe(
                writer,
                ModItems.HEAVY_REVOLVER.get(),
                "heavy_revolver"
        );


        // =========================================================
        // 高塔契约 → 物品
        // 要求 QuestDone:1b
        // =========================================================

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.BROKEN_MASK.get(),
                "broken_mask"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.BINDING_BANDAGE.get(),
                "binding_bandage"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.SEALED_THRONE.get(),
                "sealed_throne"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.NECROMANCER_LEDGER.get(),
                "necromancer_ledger"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.CHARGING_CORE.get(),
                "charging_core"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.PAINSTRIKE_HAMMER.get(),
                "painstrike_hammer"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.INK_BLADE.get(),
                "ink_blade"
        );

        // 星辉大剑
        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.STARLIGHT_GREATSWORD.get(),
                "starlight_greatsword"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.REAPER_SCYTHE.get(),
                "reaper_scythe"
        );

        completedContractRecipe(
                writer,
                ModItems.TOWER_CONTRACT.get(),
                ModItems.MACHINE_CLAW.get(),
                "machine_claw"
        );


        // =========================================================
        // 战斗契约 → 物品
        // 要求 QuestDone:1b
        // =========================================================

        completedContractRecipe(
                writer,
                ModItems.BATTLE_CONTRACT.get(),
                ModItems.BLEEDINGSHIELD.get(),
                "bleedingshield"
        );

        completedContractRecipe(
                writer,
                ModItems.BATTLE_CONTRACT.get(),
                ModItems.CRYSTALLINE_HEART.get(),
                "crystalline_heart"
        );

        completedContractRecipe(
                writer,
                ModItems.BATTLE_CONTRACT.get(),
                ModItems.SINGLE_ACTION_RAPID_FIRE.get(),
                "single_action_rapid_fire"
        );

        completedContractRecipe(
                writer,
                ModItems.BATTLE_CONTRACT.get(),
                ModItems.MAGAZINE_PRESSURE.get(),
                "magazine_pressure"
        );

        completedContractRecipe(
                writer,
                ModItems.BATTLE_CONTRACT.get(),
                ModItems.HUNTER_SERUM.get(),
                "hunter_serum"
        );


        // =========================================================
        // 复生契约 → 物品
        // 要求 QuestDone:1b
        // =========================================================

        completedContractRecipe(
                writer,
                ModItems.RESURRECTION_CONTRACT.get(),
                ModItems.SURVIVAL_WHIMPER.get(),
                "survival_whimper"
        );

        completedContractRecipe(
                writer,
                ModItems.RESURRECTION_CONTRACT.get(),
                ModItems.DRAGON_POWER.get(),
                "dragon_power"
        );


        // =========================================================
        // 帝国契约 → 物品
        // 要求 QuestDone:1b
        // =========================================================

        completedContractRecipe(
                writer,
                ModItems.IMPERIAL_CONTRACT.get(),
                ModItems.POWERSWORD.get(),
                "powersword"
        );

        completedContractRecipe(
                writer,
                ModItems.IMPERIAL_CONTRACT.get(),
                ModItems.CHAINSAW_SWORD.get(),
                "chainsawsword"
        );

        completedContractRecipe(
                writer,
                ModItems.IMPERIAL_CONTRACT.get(),
                ModItems.IMPERIAL_EAGLE.get(),
                "imperial_eagle"
        );


        // =========================================================
        // 邪恶契约 → 物品
        // 要求 QuestDone:1b
        // =========================================================

        completedContractRecipe(
                writer,
                ModItems.EVIL_CONTRACT.get(),
                ModItems.ADAPTIVE_FLESH.get(),
                "adaptive_flesh"
        );

        completedContractRecipe(
                writer,
                ModItems.EVIL_CONTRACT.get(),
                ModItems.NIGHT_CONTRACT.get(),
                "night_contract"
        );
    }


    // =============================================================
    // 完成契约 → 物品
    //
    // 实际配方要求：
    //
    // 契约 + QuestDone:1b
    //
    // 配方书解锁同样要求：
    //
    // 契约 + QuestDone:1b
    // =============================================================

    private void completedContractRecipe(
            Consumer<FinishedRecipe> writer,
            Item contract,
            Item result,
            String resultName
    ) {

        // ---------------------------------------------------------
        // 创建完成契约的 NBT
        // 等价于：
        //
        // QuestDone:1b
        // ---------------------------------------------------------

        CompoundTag completedNbt = new CompoundTag();
        completedNbt.putBoolean("QuestDone", true);


        // ---------------------------------------------------------
        // 实际配方 Ingredient
        //
        // PartialNBTIngredient 会要求契约拥有
        // QuestDone = true
        // ---------------------------------------------------------

        Ingredient completedContract =
                PartialNBTIngredient.of(
                        contract,
                        completedNbt
                );


        // ---------------------------------------------------------
        // 配方书解锁条件
        //
        // 同样要求 QuestDone = true
        // ---------------------------------------------------------

        ItemPredicate completedContractPredicate =
                ItemPredicate.Builder
                        .item()
                        .of(contract)
                        .hasNbt(completedNbt)
                        .build();


        InventoryChangeTrigger.TriggerInstance completedContractTrigger =
                InventoryChangeTrigger.TriggerInstance.hasItems(
                        completedContractPredicate
                );


        // ---------------------------------------------------------
        // 石切台配方
        // ---------------------------------------------------------

        SingleItemRecipeBuilder.stonecutting(
                        completedContract,
                        RecipeCategory.COMBAT,
                        result,
                        1
                )
                .unlockedBy(
                        "has_completed_contract",
                        completedContractTrigger
                )
                .save(
                        writer,
                        Alone_adventure.MODID
                                + ":"
                                + resultName
                                + "_from_completed_contract"
                );
    }


    // =============================================================
    // 枪械盒 → 枪械
    // =============================================================

    private void gunBoxRecipe(
            Consumer<FinishedRecipe> writer,
            Item result,
            String resultName
    ) {

        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(ModItems.GUN_BOX.get()),
                        RecipeCategory.COMBAT,
                        result,
                        1
                )
                .unlockedBy(
                        "has_gun_box",
                        has(ModItems.GUN_BOX.get())
                )
                .save(
                        writer,
                        Alone_adventure.MODID
                                + ":"
                                + resultName
                                + "_from_gun_box"
                );
    }
}
