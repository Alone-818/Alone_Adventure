package Alone818.com.alone_adventure.init;


import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;


import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;


import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;



public class ModGeoArmorItems {


    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    Alone_adventure.MODID
            );



    // =========================
    // 测试套装
    // =========================


    public static final RegistryObject<Item> TEST_HELMET =
            registerGeoArmor(
                    "test_helmet",
                    ArmorItem.Type.HELMET,
                    "test"
            );


    public static final RegistryObject<Item> TEST_CHESTPLATE =
            registerGeoArmor(
                    "test_chestplate",
                    ArmorItem.Type.CHESTPLATE,
                    "test"
            );


    public static final RegistryObject<Item> TEST_LEGGINGS =
            registerGeoArmor(
                    "test_leggings",
                    ArmorItem.Type.LEGGINGS,
                    "test"
            );


    public static final RegistryObject<Item> TEST_BOOTS =
            registerGeoArmor(
                    "test_boots",
                    ArmorItem.Type.BOOTS,
                    "test"
            );





    /**
     * 注册 Geo 盔甲
     *
     * @param name 注册名字
     * @param type 部位
     * @param geoId 对应 GeoArmorConfig ID
     */
    private static RegistryObject<Item> registerGeoArmor(
            String name,
            ArmorItem.Type type,
            String geoId
    ){

        return ITEMS.register(
                name,
                () -> new GeoArmorItem(
                        ArmorMaterials.LEATHER,
                        type,
                        geoId
                )
        );

    }



}