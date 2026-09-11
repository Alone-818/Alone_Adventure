package Alone818.com.alone_adventure.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * 通用「药水升级」酿造配方：
 * 指定源药水 + 指定材料 → 指定目标药水。
 *
 * 用于耐力药水的萤石（延长）与红石（强效）升级。
 */
public class PotionUpgradeRecipe implements IBrewingRecipe {

    private final Potion from;
    private final Ingredient ingredient;
    private final Potion to;

    public PotionUpgradeRecipe(Potion from, Ingredient ingredient, Potion to) {
        this.from = from;
        this.ingredient = ingredient;
        this.to = to;
    }

    @Override
    public boolean isInput(ItemStack input) {
        return input.getItem() instanceof PotionItem
                && !input.isEmpty()
                && PotionUtils.getPotion(input) == from;
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
        // 保留瓶型（喷溅/滞留），只更换药水类型
        ItemStack out = input.copy();
        return PotionUtils.setPotion(out, to);
    }
}