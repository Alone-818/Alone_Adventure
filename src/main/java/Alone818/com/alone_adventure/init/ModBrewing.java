package Alone818.com.alone_adventure.init;


import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.stream.Stream;



public class ModBrewing {


    /**
     * 创建指定药水的Ingredient
     * 通过PotionUtils检查药水NBT
     */
    private static Ingredient potionIngredient(Potion potion) {


        ItemStack stack = PotionUtils.setPotion(
                new ItemStack(Items.POTION),
                potion
        );


        return new Ingredient(
                Stream.of(
                        new Ingredient.ItemValue(stack)
                )
        ) {

            @Override
            public boolean test(ItemStack input) {

                return input.is(Items.POTION)
                        &&
                        PotionUtils.getPotion(input) == potion;
            }
        };
    }




    public static void register(final FMLCommonSetupEvent event) {


        event.enqueueWork(() -> {



            /*
             *
             * =============================
             * 基础药水
             * =============================
             *
             */



            /*
             * 粗制药水
             * +
             * 金甜菜根
             *
             * =
             *
             * 耐力药水
             */

            BrewingRecipeRegistry.addRecipe(
                    new BrewingRecipe(

                            potionIngredient(
                                    Potions.AWKWARD
                            ),


                            Ingredient.of(
                                    ModItems.GOLDEN_BEETROOT.get()
                            ),


                            PotionUtils.setPotion(
                                    new ItemStack(Items.POTION),
                                    ModPotions.ENDURANCE.get()
                            )
                    )
            );




            /*
             * 粗制药水
             *
             * +
             *
             * 钻石
             *
             * =
             *
             * 抗性药水
             */


            BrewingRecipeRegistry.addRecipe(
                    new BrewingRecipe(

                            potionIngredient(
                                    Potions.AWKWARD
                            ),


                            Ingredient.of(
                                    Items.DIAMOND
                            ),


                            PotionUtils.setPotion(
                                    new ItemStack(Items.POTION),
                                    ModPotions.RESISTANCE.get()
                            )
                    )
            );






            /*
             *
             * =============================
             * 红石 延长
             * =============================
             *
             */



            /*
             * 耐力 I
             *
             * +
             *
             * 红石
             *
             * =
             *
             * 耐力 I 8分钟
             */

            BrewingRecipeRegistry.addRecipe(
                    new BrewingRecipe(

                            potionIngredient(
                                    ModPotions.ENDURANCE.get()
                            ),


                            Ingredient.of(
                                    Items.REDSTONE
                            ),


                            PotionUtils.setPotion(
                                    new ItemStack(Items.POTION),
                                    ModPotions.ENDURANCE_LONG.get()
                            )
                    )
            );





            /*
             * 抗性 I
             *
             * +
             *
             * 红石
             *
             * =
             *
             * 抗性 I 8分钟
             */


            BrewingRecipeRegistry.addRecipe(
                    new BrewingRecipe(

                            potionIngredient(
                                    ModPotions.RESISTANCE.get()
                            ),


                            Ingredient.of(
                                    Items.REDSTONE
                            ),


                            PotionUtils.setPotion(
                                    new ItemStack(Items.POTION),
                                    ModPotions.RESISTANCE_LONG.get()
                            )
                    )
            );







            /*
             *
             * =============================
             * 萤石 强效
             * =============================
             *
             */



            /*
             * 耐力 I
             *
             * +
             *
             * 萤石粉
             *
             * =
             *
             * 耐力 II
             */


            BrewingRecipeRegistry.addRecipe(
                    new BrewingRecipe(

                            potionIngredient(
                                    ModPotions.ENDURANCE.get()
                            ),


                            Ingredient.of(
                                    Items.GLOWSTONE_DUST
                            ),


                            PotionUtils.setPotion(
                                    new ItemStack(Items.POTION),
                                    ModPotions.ENDURANCE_STRONG.get()
                            )
                    )
            );







            /*
             * 抗性 I
             *
             * +
             *
             * 萤石粉
             *
             * =
             *
             * 抗性 II
             */


            BrewingRecipeRegistry.addRecipe(
                    new BrewingRecipe(

                            potionIngredient(
                                    ModPotions.RESISTANCE.get()
                            ),


                            Ingredient.of(
                                    Items.GLOWSTONE_DUST
                            ),


                            PotionUtils.setPotion(
                                    new ItemStack(Items.POTION),
                                    ModPotions.RESISTANCE_STRONG.get()
                            )
                    )
            );



        });

    }

}