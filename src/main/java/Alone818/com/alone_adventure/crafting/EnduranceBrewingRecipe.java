package Alone818.com.alone_adventure.crafting;

import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.init.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * 耐力药水酿造配方
 *
 * 酿造台：水瓶 + 金甜菜根 → 耐力药水
 */
public class EnduranceBrewingRecipe implements IBrewingRecipe {

    @Override
    public boolean isInput(ItemStack input) {
        // 仅接受水瓶（无任何药水效果的 POTION 瓶）
        return input.getItem() instanceof PotionItem
                && input.is(Items.POTION)
                && PotionUtils.getPotion(input) == Potions.WATER;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return ingredient.is(ModItems.GOLDEN_BEETROOT.get());
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
        return PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.ENDURANCE.get());
    }
}