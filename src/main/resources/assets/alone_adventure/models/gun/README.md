# 枪械模型目录(GeckoLib · Blockbench 导入)

枪械模型走 **GeckoLib + Blockbench(基岩格式)** 管线,渲染器为
`client/GunGeoRenderer`,资源定位见 `client/GunGeoModel`。

## 资源约定

| 类型 | 路径 | 说明 |
|---|---|---|
| 几何模型 | `assets/alone_adventure/geo/gun/<注册名 id>.geo.json` | Blockbench → 导出 → 基岩版模型(Bedrock Model) |
| 双持举枪变体 | `geo/gun/<注册名 id>_raised.geo.json` | **可选**;双持时主手使用,文件不存在自动回退基础模型 |
| 贴图 | `textures/gun/<注册名 id>.png` | 常规 16/32 像素贴图 |
| 动画 | `animations/gun/<注册名 id>_<行为>.animation.json` | 见动画目录 README |

原版 `models/item/<注册名>.json`(平面贴片模型)与 `models/gun/*_raised.json`
不再参与枪械渲染(GeckoLib 渲染器接管第一/第三人称与 GUI),
仅作回退参考保留。

## Blockbench 制作流程

1. 新建 **基岩版实体(Bedrock Entity)** 项目**,建模(多骨骼/多方块均可,
   建议根骨骼命名 `root`),贴图绑定 `textures/gun/<注册名>.png` 的 UV。
2. 需要动画的话在动画模式制作(命名与导出见动画目录 README)。
3. **文件 → 导出 → 基岩版模型**,得到 `.geo.json`,放入 `geo/gun/`,
   命名为 `<注册名>.geo.json`。
4. F3+T 重载资源即生效。

## 占位模板

`geo/gun/` 内三把枪各带一个最简占位几何模型(根骨骼 `root` + 数个方块),
贴图为纯色占位——请用 Blockbench 导出的正式模型与贴图替换同名文件。
