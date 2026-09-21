# 枪械模型目录(GeckoLib · Blockbench 导入)

枪械模型走 **GeckoLib + Blockbench(基岩格式)** 管线,渲染器为
`client/GunGeoRenderer`,资源定位见 `client/GunGeoModel`。

## 资源约定

| 类型 | 路径 | 说明 |
|---|---|---|
| 几何模型 | `assets/alone_adventure/geo/gun/<注册名 id>.geo.json` | Blockbench → 导出 → 基岩版模型(Bedrock Model) |
| 双持举枪变体 | `geo/gun/<注册名 id>_raised.geo.json` | **可选**;双持时主手使用,文件不存在自动回退基础模型 |
| 贴图 | `textures/gun/<注册名 id>.png` | 常规 16/32 像素贴图 |
| 动画(单文件) | `animations/gun/<注册名 id>.animation.json` | **推荐**,Blockbench 整包导出,见动画目录 README |
| 动画(逐行为) | `animations/gun/<注册名 id>_<行为>.animation.json` | 兼容旧约定(rifle / shotgun 现状) |
| 持枪姿态 | `models/item/<注册名 id>.json` | **Blockbench display 导出**,见下 |

## 持枪姿态(display)导入

Blockbench 的 **Minecraft Item display** 设置导出后放到
`models/item/<注册名 id>.json`(手枪示范:`item/pistol.json`):

- 文件保留 `"parent": "builtin/entity"` 与 `display` 段(去掉 `credit`、
  `texture_size` 等非原版字段)。
- 原版渲染管线在进入 GeckoLib 渲染器之前会应用这里的 display 变换
  —— 第一/第三人称持枪角度、GUI 图案朝向、物品展示框姿态全部由此控制。
- 只改 display 就不需要重进游戏,F3+T 即可预览。

原版平面贴片模型(`models/gun/*_raised.json` 等)不再参与枪械渲染
(GeckoLib 渲染器接管第一/第三人称与 GUI),仅作历史参考保留。
主手
## Blockbench 制作流程

1. 新建 **基岩版实体(Bedrock Entity)** 项目建模(多骨骼/多方块均可),
   贴图绑定 `textures/gun/<注册名>.png` 的 UV。
2. 需要动画的话在动画模式制作(命名与导出见动画目录 README)。
3. **文件 → 导出 → 基岩版模型**,得到 `.geo.json`,放入 `geo/gun/`,
   命名为 `<注册名>.geo.json`。
4. F3+T 重载资源即生效。
