
# 新建一套 GeoArmor 的完整流程

假设我们现在要新建一套叫：

```text
knight
```

---

## ① 准备资源文件

首先建立自己的资源。

推荐结构：

```text
src/main/resources/assets/alone_adventure/

├── geo/
│   └── knight.geo.json
│
├── animations/
│   └── knight.animation.json
│
└── textures/
    └── armor/
        ├── knight/
        │   ├── knight_base.png
        │   └── knight_dye.png
        │
        └── knight_patterns/
            ├── 1.png
            └── 2.png
```

如果以后有更多 Pattern：

```text
knight_patterns/
├── 1.png
├── 2.png
├── 3.png
├── 4.png
└── 5.png
```

---

# ② 制作 GeoModel

新建：

```text
geo/knight.geo.json
```

里面的骨骼结构需要和当前 GeoArmor Renderer 能对应。

目前已经确认的标准骨骼是：

```text
armorHead
armorBody

armorRightArm
armorLeftArm

armorRightLeg
armorLeftLeg
```

所以新盔甲最好继续使用这套骨骼命名。

---

# ③ 制作 Base Texture

例如：

```text
textures/armor/knight/knight_base.png
```

这是盔甲默认外观。

例如：

```text
银色骑士甲
```

**这里直接画最终颜色。**

因为现在：

> 没有染色时，Renderer 会使用 RGB 1,1,1。

所以不会把原来的 PNG 颜色破坏掉。

---

# ④ 制作 Dye Texture

例如：

```text
textures/armor/knight/knight_dye.png
```

这一层应该主要负责：

> 希望被染料改变颜色的区域。

例如：

```text
Base
┌──────────────┐
│ 金属部分      │ ← Base
│              │
│ 布料部分      │ ← Dye Layer
└──────────────┘
```

Dye Layer 的颜色会根据玩家的染料改变。

---

# ⑤ 制作 Pattern

例如：

```text
textures/armor/knight_patterns/1.png
textures/armor/knight_patterns/2.png
```

Pattern PNG 最好：

> 不需要显示图案的位置使用透明。

例如：

```text
1.png
┌──────────────┐
│      ★       │
│              │
│   图案       │
│              │
└──────────────┘
```

Pattern 会和 Base 使用相同模型 UV。

---

# ⑥ 在 `GeoArmorRegistry.java` 注册

这是新建盔甲最重要的步骤之一。

例如：

```java
GeoArmorConfig knight =
        registerArmor(
                "knight",
                "geo/knight.geo.json",
                "textures/armor/knight/knight_base.png",
                "animations/knight.animation.json",
                true,
                0xffffff
        );
```

然后添加 Dye：

```java
knight.addLayer(
        new ArmorTextureLayer(
                new ResourceLocation(
                        MODID,
                        "textures/armor/knight/knight_dye.png"
                ),
                LayerType.DYE,
                0xffffff
        )
);
```

然后 Pattern 1：

```java
knight.addPattern(
        new PatternPreset(
                "1",
                new ResourceLocation(
                        MODID,
                        "textures/armor/knight_patterns/1.png"
                )
        )
);
```

Pattern 2：

```java
knight.addPattern(
        new PatternPreset(
                "2",
                new ResourceLocation(
                        MODID,
                        "textures/armor/knight_patterns/2.png"
                )
        )
);
```

所以完整逻辑就是：

```text
knight
│
├── Base
│   └── knight_base.png
│
├── Dye
│   └── knight_dye.png
│
└── Patterns
    ├── 1 → 1.png
    └── 2 → 2.png
```

---

# ⑦ 注册 GeoArmorItem

然后在：

```text
init/ModGeoArmorItems.java
```

注册实际的 Minecraft 物品。

例如：

```java
new GeoArmorItem(
        ArmorMaterials.LEATHER,
        ArmorItem.Type.HELMET,
        "knight"
)
```

关键就是：

```java
"knight"
```

必须和：

```java
GeoArmorConfig
```

里的：

```java
"knight"
```

完全一致。

因为：

```text
GeoArmorItem
    ↓
geoId = "knight"
    ↓
GeoArmorManager
    ↓
GeoArmorConfig "knight"
```

---

# ⑧ 如果是一整套盔甲

通常需要四个 Item：

```text
Knight Helmet
Knight Chestplate
Knight Leggings
Knight Boots
```

它们可以全部使用：

```text
"knight"
```

例如：

```java
// Helmet
new GeoArmorItem(
        ArmorMaterials.LEATHER,
        ArmorItem.Type.HELMET,
        "knight"
);

// Chestplate
new GeoArmorItem(
        ArmorMaterials.LEATHER,
        ArmorItem.Type.CHESTPLATE,
        "knight"
);

// Leggings
new GeoArmorItem(
        ArmorMaterials.LEATHER,
        ArmorItem.Type.LEGGINGS,
        "knight"
);

// Boots
new GeoArmorItem(
        ArmorMaterials.LEATHER,
        ArmorItem.Type.BOOTS,
        "knight"
);
```

这样四件装备都使用同一个：

```text
knight
```

配置。

---

# ⑨ 如果需要 ArmorType

如果这套盔甲需要特殊能力：

```java
new GeoArmorItem(
        ArmorMaterials.LEATHER,
        ArmorItem.Type.CHESTPLATE,
        "knight",
        ArmorType.XXX
)
```

那么：

```text
knight
    ↓
GeoArmorConfig

ArmorType
    ↓
特殊能力
```

两套系统是分开的。

---

# ⑩ 新盔甲最终需要改哪些地方？

实际上新建一套盔甲，主要就是：

### 必须新建

```text
geo/knight.geo.json

animations/knight.animation.json

textures/armor/knight/knight_base.png
textures/armor/knight/knight_dye.png

textures/armor/knight_patterns/1.png
textures/armor/knight_patterns/2.png
```

### 必须修改

```text
GeoArmorRegistry.java
ModGeoArmorItems.java
```

### 不需要修改

这些是**通用系统**：

```text
GeoArmorItem.java
GeoArmorConfig.java
PatternPreset.java
ArmorTextureLayer.java
LayerType.java

GeoArmorManager.java
GeoArmorRenderer.java
DynamicGeoArmorModel.java
DynamicGeoArmorRenderer.java
ArmorTextureLayerRenderer.java
```

也就是说：

> **以后新加盔甲，不应该再去改 Renderer。**

只需要增加自己的配置和资源。

---

# 最终可以记成这张表

| 内容                               | 新盔甲是否需要修改 |
| -------------------------------- | --------- |
| `GeoArmorItem.java`              | ❌         |
| `GeoArmorConfig.java`            | ❌         |
| `PatternPreset.java`             | ❌         |
| `ArmorTextureLayer.java`         | ❌         |
| `LayerType.java`                 | ❌         |
| `GeoArmorManager.java`           | ❌         |
| `DynamicGeoArmorModel.java`      | ❌         |
| `DynamicGeoArmorRenderer.java`   | ❌         |
| `ArmorTextureLayerRenderer.java` | ❌         |
| `GeoArmorRegistry.java`          | ✅         |
| `ModGeoArmorItems.java`          | ✅         |
| `.geo.json`                      | ✅ 新建      |
| `.animation.json`                | ✅ 新建      |
| `base.png`                       | ✅ 新建      |
| `dye.png`                        | ✅ 新建      |
| `Pattern PNG`                    | ✅ 新建      |

---

## 最核心的一句话

以后新增盔甲就是：

```text
做资源
  ↓
GeoArmorRegistry 注册 Config
  ↓
注册 Base / Dye / Pattern
  ↓
ModGeoArmorItems 注册物品
  ↓
完成
```

**Renderer、Pattern 系统、染色系统、GeoArmor 系统都是共用的。**

所以以后如果你告诉我：

> “我要新建一个 `dragon` 盔甲。”

我只需要按照这个结构帮你生成：

```text
dragon.geo.json
dragon.animation.json

dragon_base.png
dragon_dye.png

dragon_patterns/1.png
dragon_patterns/2.png
...
```

以及对应的：

```text
GeoArmorRegistry
ModGeoArmorItems
```

注册代码即可。
