package Alone818.com.alone_adventure.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import Alone818.com.alone_adventure.Items.injection_template;
import Alone818.com.alone_adventure.init.ModItems;

/**
 * 混合针剂合成配方
 *
 * 形状自由匹配：任意位置放
 *   2 支带效果的针剂
 * + 任意数量的怪物残灰（每格：全部效果等级 +1，时长 ×0.6）
 * + 任意数量的血脉碎片（每格：时长 +60%，按基础时长累加，N 个 = ×(1 + 0.6N)）
 * → 2 支混合针剂
 *
 * 两支针剂的相同效果合并时取更高等级与更长时长（不叠加，
 * 防止反复混合无限刷时长）；混合后的效果种类不能超过
 * injection_template.MAX_EFFECTS（5）种，否则不匹配。
 * 混合针剂仍然是普通针剂，可以正常使用、再次混合。
 *
 * 与 InjectionRecipe 相同按“格子数”匹配：每个非空格子不论堆叠
 * 多少都只算 1 份材料（每次合成从每个非空格子消耗 1 个物品），
 * 想叠加更多残灰/碎片就多放几个格子。
 */
public class InjectionMixRecipe extends CustomRecipe {

    public InjectionMixRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        int syringe = 0;
        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.INJECTION_SYRINGE.get())) {
                if (syringe == 0) {
                    first = stack;
                } else if (syringe == 1) {
                    second = stack;
                }
                syringe++;
            } else if (stack.is(ModItems.MONSTER_ASH.get())) {
                // 残灰数量不限
            } else if (stack.is(ModItems.BLOOD_SHARD.get())) {
                // 血脉碎片数量不限
            } else {
                return false; // 存在其他物品则不匹配
            }
        }
        if (syringe != 2) return false;
        // 两支都必须带效果，且混合后不能超过效果种类上限
        if (injection_template.readEffects(first).isEmpty()) return false;
        if (injection_template.readEffects(second).isEmpty()) return false;
        return injection_template.mixEffects(first, second).size() <= injection_template.MAX_EFFECTS;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        List<ItemStack> syringes = new ArrayList<>();
        int ash = 0;
        int blood = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.INJECTION_SYRINGE.get())) {
                syringes.add(stack);
            } else if (stack.is(ModItems.MONSTER_ASH.get())) {
                ash++;
            } else if (stack.is(ModItems.BLOOD_SHARD.get())) {
                blood++;
            }
        }
        if (syringes.size() != 2) return ItemStack.EMPTY;

        List<MobEffectInstance> mixed = injection_template.mixEffects(syringes.get(0), syringes.get(1));
        mixed = injection_template.applyCatalysts(mixed, ash, blood);
        // 混合针剂不再缩放时长（0.8 倍只在“药水 → 针剂”时应用）
        return injection_template.createSyringe(mixed, 2, 1.0);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    /** 序列化器：配方本身无数据，仅注册类型标识 */
    public static class Serializer implements RecipeSerializer<InjectionMixRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public InjectionMixRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    json.has("category") ? json.get("category").getAsString() : "misc",
                    CraftingBookCategory.MISC);
            return new InjectionMixRecipe(recipeId, category);
        }

        @Override
        public InjectionMixRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new InjectionMixRecipe(recipeId, CraftingBookCategory.MISC);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, InjectionMixRecipe recipe) {
            // 无数据需序列化
        }
    }
}
