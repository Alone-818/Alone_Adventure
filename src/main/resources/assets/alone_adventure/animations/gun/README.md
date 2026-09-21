# 枪械动画目录（GeckoLib · Blockbench 导入）

动画走 **GeckoLib**,文件由 **Blockbench** 制作导出(基岩版动画格式)。

## 命名约定(核心)

```
<本体物品注册名 id>_<行为(英文)>.animation.json
```

例:手枪(注册名 `pistol`):

| 文件 | 行为 | 触发 | 播放方式 |
|---|---|---|---|
| `pistol_fire.animation.json`   | 开火 | 任一手开火,播完即回落 | 单次(连射自动重播) |
| `pistol_reload.animation.json` | 装填 | 该枪处于装填状态(NBT) | 单次,长度建议=装填秒数 |
| `pistol_aim.animation.json`    | 瞄准 | 右键瞄准中 | 循环 |
| `pistol_run.animation.json`    | 跑动 | 疾跑中 | 循环 |
| `pistol_idle.animation.json`   | 待机 | 兜底 | 循环 |

- 行为优先级:**fire > reload > aim > run > idle**,状态间由控制器自动平滑过渡(5 tick)。
- **缺文件自动降级**:只提供 `pistol_fire` 一个文件也完全可以,其余行为落到下一优先级;全缺则模型静止。
- **文件内动画名必须与文件名一致**(即 `<id>_<行为>`,如 `pistol_fire`)——解析按此约定直查同名文件。

## Blockbench 制作流程

1. 打开枪械模型项目(几何模型放 `geo/gun/`,见该目录说明)。
2. 切换到 **动画(Animate)** 模式,新建动画并**命名为 `<物品id>_<行为>`**(如 `pistol_fire`)。
3. 给骨骼(建议用 `root` 根骨骼;多骨骼会被合并解析到整枪)打关键帧:位移/旋转/缩放。
4. 循环类行为(idle/run/aim)在动画设置里把 **Loop** 勾上。
5. 装填动画长度对齐枪械装填秒数(手枪 1.5s / 步枪 3s / 霰弹枪 2.5s)。
6. **动画(Animation) → 导出动画(Export Animations)**,得到 `.animation.json`。
7. 重命名为约定文件名放进本目录,进游戏 **F3+T** 重载资源即生效。

## 单位与方向

- 位移单位为**像素**(16 像素 = 1 格),旋转为**度**。
- 旋转轴映射与 GeckoLib 一致(Y 轴旋转取反);若游戏内方向与 Blockbench 预览相反,在 Blockbench 中把关键帧符号翻转即可。
- 位移/旋转直接作用于整枪根骨骼;第一人称中枪口朝 -Z、枪托朝 +Z。

## 模板

目录内三把枪(手枪/步枪/霰弹枪)各带全部 5 个行为的最简占位动画,可作参照;
删掉某个文件即回退该行为(如删掉 `pistol_fire.animation.json`,开火时不播动画)。
