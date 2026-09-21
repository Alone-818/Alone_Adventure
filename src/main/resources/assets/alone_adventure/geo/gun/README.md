# 枪械几何模型目录（GeckoLib · Blockbench 导入）

存放枪械的 Blockbench（基岩版格式）几何模型 `.geo.json`。

## 资源约定

| 文件 | 说明 |
|---|---|
| `<注册名 id>.geo.json` | 基础模型（必需） |
| `<注册名 id>_raised.geo.json` | 双持举枪变体（可选，缺失自动回退基础模型） |

- 由 Blockbench **文件 → 导出 → 基岩版模型** 生成，重命名放入本目录，F3+T 生效。
- `identifier` 建议规范为 `geometry.alone_adventure.<注册名>`（不参与查找，仅标识）。
- 贴图：`textures/gun/<注册名>.png`（`texture_width/height` 与贴图尺寸一致）。

## 骨骼结构

动画按骨骼名作用，几何模型与动画文件（见 `animations/gun/`）的骨骼名必须一致：

- 手枪 `pistol.geo.json` —— 三骨骼（拼音）：
  - `ZHUTI`（主体）：整枪，持枪姿态/后坐/跑动颠簸都作用于此
  - `DANCHAO`（弹匣）：开火余震、换弹拆装
  - `BANJI`（扳机）：击发扣动、上膛收尾
- rifle / shotgun 占位模型为单骨骼 `root`。

## 动画绑定

动画不放在本目录 —— 见 `../animations/gun/README.md`
（推荐单文件 `pistol.animation.json` 形式，内名即行为名）。
