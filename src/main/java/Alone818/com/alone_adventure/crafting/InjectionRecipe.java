package Alone818.com.alone_adventure.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

import Alone818.com.alone_adventure.Items.hunterItems.injection_template;
import Alone818.com.alone_adventure.init.ModItems;


public class InjectionRecipe extends CustomRecipe {


    public InjectionRecipe(ResourceLocation id,
                           CraftingBookCategory category) {
        super(id, category);
    }


    @Override
    public boolean matches(CraftingContainer inv, Level level) {

        int potion = 0;
        int ash = 0;
        int empty = 0;


        for (int i = 0; i < inv.getContainerSize(); i++) {

            ItemStack stack = inv.getItem(i);


            if (stack.isEmpty()) {
                continue;
            }


            if (stack.getItem() instanceof PotionItem) {

                List<MobEffectInstance> effects =
                        PotionUtils.getMobEffects(stack);


                // 没有效果的药水不允许
                if (effects.isEmpty()) {
                    return false;
                }


                potion++;


            } else if (stack.is(ModItems.MONSTER_ASH.get())) {

                ash++;


            } else if (stack.is(ModItems.INJECTION_EMPTY.get())) {

                empty++;


            } else {

                return false;
            }
        }


        return potion == 1
                && ash == 1
                && empty == 2;
    }



    @Override
    public ItemStack assemble(CraftingContainer inv,
                              RegistryAccess registryAccess) {


        for (int i = 0; i < inv.getContainerSize(); i++) {


            ItemStack stack = inv.getItem(i);


            if (stack.getItem() instanceof PotionItem) {


                List<MobEffectInstance> effects =
                        injection_template.getEffects(stack);



                if (effects.isEmpty()) {
                    return ItemStack.EMPTY;
                }


                return injection_template.createSyringe(
                        effects,
                        2
                );
            }
        }


        return ItemStack.EMPTY;
    }



    @Override
    public NonNullList<ItemStack> getRemainingItems(
            CraftingContainer inv) {


        NonNullList<ItemStack> result =
                NonNullList.withSize(
                        inv.getContainerSize(),
                        ItemStack.EMPTY
                );



        for (int i = 0; i < inv.getContainerSize(); i++) {


            ItemStack stack = inv.getItem(i);



            if (!stack.isEmpty()
                    && stack.getItem() instanceof PotionItem) {


                result.set(
                        i,
                        new ItemStack(Items.GLASS_BOTTLE)
                );
            }
        }


        return result;
    }



    @Override
    public boolean canCraftInDimensions(
            int width,
            int height) {

        return width * height >= 4;
    }



    @Override
    public RecipeSerializer<?> getSerializer() {

        return Serializer.INSTANCE;
    }





    public static class Serializer
            implements RecipeSerializer<InjectionRecipe> {


        public static final Serializer INSTANCE =
                new Serializer();



        @Override
        public InjectionRecipe fromJson(
                ResourceLocation id,
                JsonObject json) {


            CraftingBookCategory category =
                    CraftingBookCategory.CODEC.byName(
                            json.has("category")
                                    ? json.get("category")
                                    .getAsString()
                                    : "misc",
                            CraftingBookCategory.MISC
                    );


            return new InjectionRecipe(
                    id,
                    category
            );
        }



        @Override
        public InjectionRecipe fromNetwork(
                ResourceLocation id,
                FriendlyByteBuf buffer) {


            return new InjectionRecipe(
                    id,
                    CraftingBookCategory.MISC
            );
        }



        @Override
        public void toNetwork(
                FriendlyByteBuf buffer,
                InjectionRecipe recipe) {

        }
    }
}