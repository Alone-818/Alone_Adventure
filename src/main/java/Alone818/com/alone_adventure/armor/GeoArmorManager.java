package Alone818.com.alone_adventure.armor;


import java.util.HashMap;
import java.util.Map;



public class GeoArmorManager {


    private static final Map<String, GeoArmorConfig> ARMORS =
            new HashMap<>();


    public static void register(
            GeoArmorConfig config
    ){


        ARMORS.put(
                config.id,
                config
        );



    }


    public static GeoArmorConfig get(
            String id
    ){

        return ARMORS.get(id);

    }


}