# 枪械动画目录（GeckoLib · Blockbench 导入）

动画走 **GeckoLib**，文件由 **Blockbench** 制作导出（基岩版动画格式）。

## 推荐形式：单文件多动画

```
animations/gun/<物品注册名 id>.animation.json
```

一个 Blockbench 动画项目的**整包导出**——全部行为动画放在同一个文件里，
动画内名直接用行为名（`fire` / `reload` / `aim` / `run` / `idle`）。

例：手枪（注册名 `pistol`）→ `pistol.animation.json`：

| 内名（Blockbench 动画名） | 行为 | 触发 | 播放方式 |
|---|---|---|---|
| `fire`   | 开火 | 任一手开火，播完即回落 | 单次（连射自动重播） |
| `reload` | 装填 | 该枪处于装填状态（NBT） | 单次，长度对齐装填秒数 |
| `aim`    | 瞄准 | 右键瞄准中 | 循环 |
| `run`    | 跑动 | 疾跑中 | 循环 |
| `idle`   | 待机 | 兜底 | 循环 |

**导入只需一步**：Blockbench 里把动画命名为行为名（英文）→
导出动画 → 文件重命名为 `<物品id>.animation.json` 放进本目录 → F3+T 重载生效。
不用改文件内的任何内容。

## 兼容形式：逐行为文件（旧约定）

```
animations/gun/<物品注册名 id>_<行为(英文)>.animation.json
```

每个行为一个文件（rifle / shotgun 现状）。解析顺序在单文件之后——
同一把枪两种形式并存时单文件优先。

## 解析规则（`client/GunGeoModel`）

- 行为优先级：**fire > reload > aim > run > idle**，状态间由控制器自动平滑过渡（5 tick）。
- **缺行为自动降级**：只提供 `fire` 一个动画也完全可以，其余行为落到下一优先级；全缺则模型静止。
- 文件内动画名宽容匹配：精确名 `<物品id>_<行为>`（如 `pistol_fire`）→ 纯行为名（`fire`）→
  逐行为文件内唯一动画（内名随意）。单文件形式直接用行为名即可。

## Blockbench 制作流程

1. 打开枪械模型项目（几何模型放 `geo/gun/`，见该目录说明）。
2. 切换到 **动画（Animate）** 模式，每个行为新建一个动画，**命名为行为名**
   （`fire` / `reload` / `aim` / `run` / `idle`，英文小写）。
3. 给骨骼打关键帧：位移/旋转/缩放（手枪示范见 `pistol.animation.json`——
   开火后坐、换弹时拆装弹匣、跑动颠簸、待机呼吸等）。
4. 循环类行为（idle/run/aim）在动画设置里把 **Loop** 勾上。
5. 装填动画长度对齐枪械装填秒数（手枪 1.5s / 步枪 3s / 霰弹枪 2.5s）。
6. **动画（Animation） → 导出动画（Export Animations）**，得到 `.animation.json`。
7. 重命名为 `<物品id>.animation.json` 放进本目录，进游戏 **F3+T** 重载资源即生效。

## 单位与方向

- 位移单位为**像素**（16 像素 = 1 格），旋转为**度**。
- 旋转轴映射与 GeckoLib 一致（Y 轴旋转取反）；若游戏内方向与 Blockbench 预览相反，
  在 Blockbench 中把关键帧符号翻转即可。
- 位移/旋转直接作用于骨骼；第一人称中枪口朝 -Z、枪托朝 +Z（由 `models/item/` 的 display 控制）。
