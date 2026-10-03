# Alone Adventure GeoArmor 装备系统说明文档

> 版本：Minecraft 1.20.1 / Forge 47.x / GeckoLib 4.x  
> 核心原则：**GeoArmor 负责“装备长什么样”，ArmorType 负责“装备有什么力量”。**

---

## 目录

1. 系统总览
2. 核心架构
3. 文件结构
4. 核心类与配置字段
5. Geo 模型与动画
6. 材质 Layer 与染色系统
7. 装备注册
8. ArmorType 装备能力系统
9. 创建新装备流程
10. 完整示例：暗影铠甲
11. 扩展方向
12. 最终总结

---

## 1. 系统总览

GeoArmor 是 Alone Adventure 中用于制作自定义动态装备的系统。

基于：

- Minecraft Forge 1.20.1
- GeckoLib 4

提供：

- 自定义 Geo 模型
- 自定义动画
- 玩家动作同步
- 多材质叠加
- 部分区域染色
- 图案覆盖
- 发光效果
- 装备特殊能力
- 装备强化扩展

设计目标：

- 渲染负责：模型、材质、动画、Layer
- 能力负责：技能、Buff、强化、套装效果
- 避免装备代码越来越混乱

---

## 2. 核心架构

```text
GeoArmorItem
    |
    v
GeoArmorConfig
    |
    +------ Model
    +------ Texture
    +------ Animation
    +------ TextureLayer
    |
    v
ArmorType
    |
    +------ 技能
    +------ Buff
    +------ 强化
    +------ 套装效果
```

职责分离：

| 模块 | 负责内容 |
|---|---|
| GeoArmorItem | 实际装备物品，装备后寻找配置并调用渲染/能力 |
| GeoArmorConfig | 模型、材质、动画、Layer、armorType |
| GeoArmorRegistry | 集中注册装备配置 |
| ArmorType | 技能、属性、强化、特殊效果 |
| ArmorTextureLayer | 额外材质层，如图案、染色、发光 |
| LayerType | 定义材质层类型 |

---

## 3. 文件结构

### Java

```text
alone_adventure
├── Items
│   └── armor
│       ├── GeoArmorItem.java
│       ├── GeoArmorConfig.java
│       ├── GeoArmorManager.java
│       ├── GeoArmorRegistry.java
│       ├── ArmorTextureLayer.java
│       ├── LayerType.java
│       └── type
│           ├── ArmorType.java
│           ├── ArmorTypeRegistry.java
│           ├── KnightArmorType.java
│           ├── FireArmorType.java
│           └── HunterArmorType.java
├── client
│   └── armor
│       ├── DynamicGeoArmorModel.java
│       ├── DynamicGeoArmorRenderer.java
│       └── ArmorTextureLayerRenderer.java
└── init
    └── ModGeoArmorItems.java
```

### 资源

```text
assets/alone_adventure
├── geo
│   └── test.geo.json
├── animations
│   └── test.animation.json
└── textures
    └── armor
        ├── test_base.png
        ├── test_pattern.png
        ├── test_dye.png
        └── test_glow.png
```

---

## 4. 核心类与配置字段

### 4.1 GeoArmorItem

位置：

```text
Items/armor/GeoArmorItem.java
```

作用：

```text
玩家装备
  ↓
寻找 Geo 配置
  ↓
调用 GeckoLib 渲染
  ↓
调用 ArmorType 能力
```

核心数据：

```java
private final String geoId;
```

`geoId` 用于寻找 `GeoArmorConfig`。

例如：

```java
"test"
```

对应：

```java
GeoArmorRegistry.registerArmor(
    "test",
    ...
);
```

### 4.2 GeoArmorConfig

`GeoArmorConfig` 是一个装备的完整配置。

```java
public class GeoArmorConfig {

    String id;

    ResourceLocation model;

    ResourceLocation texture;

    ResourceLocation animation;

    boolean dyeable;

    int defaultColor;

    List<ArmorTextureLayer> layers;

    String armorType;
}
```

字段说明：

| 字段 | 作用 |
|---|---|
| id | 装备唯一 ID |
| model | Geo 模型路径 |
| texture | 基础材质 |
| animation | 动画文件 |
| dyeable | 是否支持染色 |
| defaultColor | 默认颜色 |
| layers | 额外材质层 |
| armorType | 绑定的能力类型 ID |

示例：

```java
GeoArmorConfig test =
    registerArmor(
        "test",
        "geo/test.geo.json",
        "textures/armor/test_base.png",
        "animations/test.animation.json",
        true,
        0xffffff
    );
```

### 4.3 GeoArmorRegistry

位置：

```text
armor/GeoArmorRegistry.java
```

用于集中注册装备配置。

```java
public static void register() {
    registerTestArmor();
}
```

注册示例：

```java
private static void registerTestArmor() {
    GeoArmorConfig test =
        registerArmor(
            "test",
            "geo/test.geo.json",
            "textures/armor/test_base.png",
            "animations/test.animation.json",
            true,
            0xffffff
        );
}
```

---

## 5. Geo 模型与动画

### 5.1 Geo 模型规范

模型路径：

```text
assets/alone_adventure/geo/
```

例如：

```text
test.geo.json
```

推荐骨骼名称：

| 骨骼 | 对应部位 |
|---|---|
| armorHead | 头盔 |
| armorBody | 胸甲 |
| armorRightArm | 右手 |
| armorLeftArm | 左手 |
| armorRightLeg | 右腿 |
| armorLeftLeg | 左腿 |

如果骨骼错误，可能出现：

- 蹲下偏移
- 手臂不同步
- 身体突出
- 装备漂浮

### 5.2 动画系统

动画路径：

```text
assets/alone_adventure/animations/
```

例如：

```text
test.animation.json
```

动画由 GeckoLib 控制。

装备会自动同步玩家动作：

```text
玩家行走
  ↓
玩家骨骼变化
  ↓
GeoArmor 骨骼跟随
```

支持：

- 行走
- 奔跑
- 跳跃
- 蹲下
- 游泳
- 攻击动作

注意：模型骨骼必须正确绑定，否则玩家动作正常，但装备模型不跟随。

---

## 6. 材质 Layer 与染色系统

### 6.1 材质 Layer 系统

一个装备可以拥有多个材质层。

常见最终渲染叠加顺序：

```text
Base Texture
  ↓
Pattern Layer
  ↓
Dye Layer
  ↓
Emissive Layer
```

例如骑士铠甲：

| 层 | 作用 |
|---|---|
| 基础 | 金属盔甲 |
| 图案 | 家族徽章 |
| 染色 | 布料颜色 |
| 发光 | 魔法纹路 |

### 6.2 ArmorTextureLayer

材质层对象：

```java
new ArmorTextureLayer(
    texture,
    LayerType,
    color
);
```

参数：

| 参数 | 说明 |
|---|---|
| texture | 材质路径 |
| LayerType | 层类型 |
| color | 默认颜色 |

示例：

```java
new ArmorTextureLayer(
    new ResourceLocation(
        MODID,
        "textures/armor/test_pattern.png"
    ),
    LayerType.NORMAL,
    0xffffff
);
```

### 6.3 LayerType 类型说明

| 类型 | 特点 | 用途 |
|---|---|---|
| NORMAL | 普通固定图案层，不受染色影响，使用自身颜色 | 徽章、符文、装饰、LOGO |
| DYE | 染色层，受玩家染色影响 | 布料、皮革、可变颜色区域 |
| EMISSIVE | 发光层，不受环境亮度影响，永远高亮 | 魔法装备、科技装备、能量核心 |

注意：

- 只有 `DYE` 层会改变颜色。
- `NORMAL`、`EMISSIVE`、`Base Texture` 不会因染色改变。

### 6.4 添加材质层

完整例子：

```java
GeoArmorConfig test =
    registerArmor(
        "test",
        "geo/test.geo.json",
        "textures/armor/test_base.png",
        "animations/test.animation.json",
        true,
        0xffffff
    );
```

添加图案：

```java
test.addLayer(
    new ArmorTextureLayer(
        new ResourceLocation(
            MODID,
            "textures/armor/test_pattern.png"
        ),
        LayerType.NORMAL,
        0xffffff
    )
);
```

添加染色区域：

```java
test.addLayer(
    new ArmorTextureLayer(
        new ResourceLocation(
            MODID,
            "textures/armor/test_dye.png"
        ),
        LayerType.DYE,
        0xffffff
    )
);
```

添加发光：

```java
test.addLayer(
    new ArmorTextureLayer(
        new ResourceLocation(
            MODID,
            "textures/armor/test_glow.png"
        ),
        LayerType.EMISSIVE,
        0xffffff
    )
);
```

最终效果：

```text
test_base.png
  ↓
test_pattern.png
  ↓
test_dye.png
  ↓
test_glow.png
```

### 6.5 染色系统

`GeoArmorItem` 实现：

```java
DyeableLeatherItem
```

所以可以使用 Minecraft 原版染色系统。

默认颜色：

```java
0xffffff
```

代表纯白。

染色流程：

```text
玩家使用染料
  ↓
ItemStack 保存颜色
  ↓
Renderer 读取颜色
  ↓
DYE Layer 改变颜色
```

### 6.6 染色材质制作规则

例如目标：

- 黑色盔甲
- 红色布料
- 金色徽章
- 蓝色发光纹路

制作：

| 文件 | 内容 | 类型 |
|---|---|---|
| test_base.png | 黑色区域 | Base |
| test_pattern.png | 金色徽章 | LayerType.NORMAL |
| test_dye.png | 白色布料区域 | LayerType.DYE |
| test_glow.png | 蓝色纹理 | LayerType.EMISSIVE |

---

## 7. 装备注册

位置：

```text
init/ModGeoArmorItems.java
```

示例：

```java
public static final RegistryObject<Item> TEST_CHESTPLATE =
    ITEMS.register(
        "test_chestplate",
        () -> new GeoArmorItem(
            ArmorMaterials.LEATHER,
            ArmorItem.Type.CHESTPLATE,
            "test"
        )
    );
```

参数：

| 参数 | 作用 |
|---|---|
| ArmorMaterial | 装备材料 |
| Type | 装备位置 |
| test | GeoArmorConfig ID |

---

## 8. ArmorType 装备能力系统

### 8.1 设计目标

GeoArmor 负责：

- 模型
- 材质
- 动画
- 染色

ArmorType 负责：

- 技能
- 属性
- 强化
- 特殊效果

两者分离。

结构：

```text
GeoArmorItem
    |
    v
ArmorType
    |
    +---- 被动效果
    +---- 主动技能
    +---- 强化等级
    +---- 套装效果
```

目录：

```text
Items/armor/type
├── ArmorType.java
├── ArmorTypeRegistry.java
├── KnightArmorType.java
├── FireArmorType.java
└── HunterArmorType.java
```

### 8.2 ArmorType 接口

```java
package Alone818.com.alone_adventure.Items.armor.type;

import net.minecraft.world.entity.player.Player;

public interface ArmorType {

    /*
     * 装备穿戴时
     */
    default void onEquip(Player player) {
    }

    /*
     * 装备卸下时
     */
    default void onRemove(Player player) {
    }

    /*
     * 每 tick 执行
     */
    default void tick(Player player) {
    }

    /*
     * 主动技能
     */
    default void skill(Player player) {
    }
}
```

### 8.3 创建装备类型示例：KnightArmorType

```java
package Alone818.com.alone_adventure.Items.armor.type;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

public class KnightArmorType implements ArmorType {

    @Override
    public void tick(Player player) {

        if (player.getHealth() < player.getMaxHealth() / 2) {

            player.addEffect(
                new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    40,
                    1
                )
            );
        }
    }
}
```

效果：

```text
玩家低血量
  ↓
获得抗性
```

### 8.4 ArmorTypeRegistry

```java
package Alone818.com.alone_adventure.Items.armor.type;

import java.util.HashMap;
import java.util.Map;

public class ArmorTypeRegistry {

    private static final Map<String, ArmorType> TYPES = new HashMap<>();

    public static void register(String id, ArmorType type) {
        TYPES.put(id, type);
    }

    public static ArmorType get(String id) {
        return TYPES.get(id);
    }
}
```

### 8.5 GeoArmorConfig 增加 Type

修改 `GeoArmorConfig.java`：

```java
public final String armorType;
```

构造增加：

```java
String armorType
```

例如：

```java
new GeoArmorConfig(
    "knight",
    model,
    texture,
    animation,
    true,
    0xffffff,
    "knight"
);
```

### 8.6 GeoArmorItem 绑定能力

```java
public ArmorType getArmorType() {

    GeoArmorConfig config = getConfig();

    if (config == null) {
        return null;
    }

    return ArmorTypeRegistry.get(config.armorType);
}
```

### 8.7 装备 tick 调用

在玩家 tick 事件中调用，例如 `ArmorEvents.java`：

```java
@SubscribeEvent
public static void playerTick(PlayerTickEvent event) {

    Player player = event.player;

    for (ItemStack stack : player.getArmorSlots()) {

        if (stack.getItem() instanceof GeoArmorItem armor) {

            ArmorType type = armor.getArmorType();

            if (type != null) {
                type.tick(player);
            }
        }
    }
}
```

---

## 9. 创建新装备流程

### 第一步：创建模型

```text
assets/alone_adventure/geo/new.geo.json
```

### 第二步：创建动画

```text
assets/alone_adventure/animations/new.animation.json
```

### 第三步：创建基础材质

```text
assets/alone_adventure/textures/armor/new_base.png
```

### 第四步：注册配置

```java
registerArmor(
    "new",
    "geo/new.geo.json",
    "textures/armor/new_base.png",
    "animations/new.animation.json",
    true,
    0xffffff
);
```

### 第五步：添加 Layer

图案：

```java
LayerType.NORMAL
```

染色：

```java
LayerType.DYE
```

发光：

```java
LayerType.EMISSIVE
```

### 第六步：注册 Item

```java
new GeoArmorItem(
    ArmorMaterials.DIAMOND,
    ArmorItem.Type.CHESTPLATE,
    "new"
)
```

### 第七步：绑定 ArmorType

在 `GeoArmorConfig` 中设置 `armorType`，并在 `ArmorTypeRegistry` 注册对应能力。

---

## 10. 完整示例：暗影铠甲

模型：

```text
shadow.geo.json
```

材质：

```text
shadow_base.png
```

图案：

```text
shadow_rune.png
```

发光：

```text
shadow_glow.png
```

注册：

```java
GeoArmorConfig shadow =
    registerArmor(
        "shadow",
        "geo/shadow.geo.json",
        "textures/armor/shadow_base.png",
        "animations/shadow.animation.json",
        true,
        0xffffff,
        "shadow"
    );

shadow.addLayer(
    new ArmorTextureLayer(
        new ResourceLocation(
            MODID,
            "textures/armor/shadow_rune.png"
        ),
        LayerType.NORMAL,
        0xffffff
    )
);

shadow.addLayer(
    new ArmorTextureLayer(
        new ResourceLocation(
            MODID,
            "textures/armor/shadow_glow.png"
        ),
        LayerType.EMISSIVE,
        0xffffff
    )
);
```

能力：

```text
ShadowArmorType
```

效果：

- 夜晚速度提升
- 潜行增强
- 短距离瞬移

---

## 11. 扩展方向

以后可以继续增加：

### 套装系统

```text
3 件：速度
4 件：技能
5 件：终极效果
```

### 强化系统

```text
铁锭
  ↓
强化等级 +1
```

### 附魔兼容

- 锋利
- 保护

### 自定义词条

RPG 词条，例如：

- 暴击率
- 攻击速度
- 生命偷取
- 元素伤害

### 强化等级数据结构

如果需要装备升级，增加 `ArmorData` 保存：

- 等级
- 经验
- 强化属性

结构：

```text
ItemStack
  |
  v
Capability
  |
  +--- level
  +--- exp
  +--- upgrade
```

例如：

```text
骑士胸甲
等级 5
防御 +20%
技能：钢铁意志
```

---

## 12. 最终总结

完整关系：

```text
                  GeoArmorItem
                         |
                         v
                  GeoArmorConfig
          _____________|_____________
          |             |            |
        Model        Texture       Type
          |
     TextureLayer
     /      |       \
 Pattern   Dye    Emissive

ArmorType
   |
   +--- 技能系统
   |
   +--- 强化系统
```

当前 GeoArmor 系统支持：

- ✅ 自定义 Geo 模型
- ✅ 自定义动画
- ✅ 玩家动作同步
- ✅ 多材质 Layer
- ✅ 图案覆盖
- ✅ 部分区域染色
- ✅ 发光纹理
- ✅ 动态颜色
- ✅ 独立装备能力
- ✅ 装备强化扩展

推荐开发流程：

```text
制作模型
  ↓
制作材质
  ↓
注册 GeoArmorConfig
  ↓
添加 Layer
  ↓
注册 Item
  ↓
绑定 ArmorType
  ↓
添加特殊能力
```

一句话总结：

> **GeoArmor 负责“装备长什么样”。  
> ArmorType 负责“装备有什么力量”。**