package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.miscCurios.hunter_serum;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;


/**
 * ============================================================
 * 猎人视野客户端
 * ============================================================
 *
 * Forge:
 *      1.20.1
 *
 * Forge Version:
 *      47.4.23
 *
 * 功能：
 *
 * 1. 开启猎人视野：
 *      - 屏幕黑白
 *      - 生物白色轮廓
 *      - 掉落物白色轮廓
 *      - 容器白色线框
 *
 * 2. 关闭猎人视野：
 *      - 关闭 Post Shader
 *      - 清空容器高亮
 *
 * 3. 不使用：
 *      Entity#setGlowingTag
 *
 * 因此不会修改实体原本的 Glowing 状态。
 *
 * ============================================================
 */
@Mod.EventBusSubscriber(
        modid = Alone_adventure.MODID,
        value = Dist.CLIENT
)
public class HunterVisionClient {

    /*
     * ========================================================
     * 猎人视野状态
     * ========================================================
     */

    /**
     * 剩余 Tick。
     *
     * 0 = 未开启
     */
    private static int remainingTicks = 0;


    /**
     * 猎人视野后处理。
     *
     * 对应资源：
     *
     * assets/
     *   alone_adventure/
     *     shaders/
     *       post/
     *         hunter_vision.json
     */
    private static final ResourceLocation HUNTER_VISION_SHADER =
            new ResourceLocation(
                    Alone_adventure.MODID,
                    "shaders/post/hunter_vision.json"
            );


    /*
     * ========================================================
     * 实体轮廓颜色
     * ========================================================
     */

    /**
     * 生物 / 掉落物轮廓颜色。
     *
     * 白色。
     */
    private static final int OUTLINE_R = 255;
    private static final int OUTLINE_G = 255;
    private static final int OUTLINE_B = 255;
    private static final int OUTLINE_A = 255;


    /*
     * ========================================================
     * 容器
     * ========================================================
     */

    /**
     * 当前范围内发现的容器 AABB。
     */
    private static final List<AABB> CONTAINER_BOXES =
            new ArrayList<>();


    /**
     * 容器线框颜色。
     */
    private static final float CONTAINER_R = 1.0F;
    private static final float CONTAINER_G = 1.0F;
    private static final float CONTAINER_B = 1.0F;
    private static final float CONTAINER_A = 0.90F;


    /**
     * 容器线框宽度。
     *
     * 不要设置太大。
     *
     * 部分显卡对 lineWidth 支持有限。
     */
    private static final float CONTAINER_LINE_WIDTH = 2.0F;


    /*
     * ========================================================
     * 状态接口
     * ========================================================
     */

    /**
     * 判断猎人视野是否开启。
     */
    public static boolean isActive() {
        return remainingTicks > 0;
    }


    /**
     * 获取剩余 Tick。
     */
    public static int getRemainingTicks() {
        return remainingTicks;
    }


    /*
     * ========================================================
     * 开启猎人视野
     * ========================================================
     */

    /**
     * 开启猎人视野。
     *
     * durationTicks：
     *
     * 20 Tick = 1 秒。
     *
     * 例如：
     *
     * activate(20 * 30);
     *
     * = 30 秒。
     */
    public static void activate(int durationTicks) {

        Minecraft mc = Minecraft.getInstance();


        /*
         * 客户端世界不存在。
         */
        if (mc.level == null || mc.player == null) {
            return;
        }


        /*
         * 最少持续 1 Tick。
         */
        remainingTicks =
                Math.max(1, durationTicks);


        /*
         * 如果当前没有后处理效果，
         * 才加载猎人视野。
         *
         * 防止重复 loadEffect。
         */
        if (mc.gameRenderer.currentEffect() == null) {

            try {

                mc.gameRenderer.loadEffect(
                        HUNTER_VISION_SHADER
                );

            } catch (Exception ignored) {

                /*
                 * Shader 加载失败时：
                 *
                 * 不让游戏崩溃。
                 *
                 * 实体 Outline 仍然可以工作。
                 */
            }
        }


        /*
         * 立即扫描一次容器。
         *
         * 不需要等 20 Tick。
         */
        scanContainers(mc);
    }


    /*
     * ========================================================
     * 关闭猎人视野
     * ========================================================
     */

    /**
     * 关闭猎人视野。
     */
    public static void end() {

        remainingTicks = 0;


        /*
         * 清除容器。
         */
        CONTAINER_BOXES.clear();


        Minecraft mc =
                Minecraft.getInstance();


        /*
         * 如果存在 Post Effect，
         * 关闭它。
         */
        if (mc.gameRenderer.currentEffect() != null) {

            try {

                mc.gameRenderer.shutdownEffect();

            } catch (Exception ignored) {

                /*
                 * 不让关闭 Shader 的异常
                 * 影响游戏。
                 */
            }
        }
    }


    /*
     * ========================================================
     * Client Tick
     * ========================================================
     */

    /**
     * 客户端 Tick。
     *
     * 负责：
     *
     * 1. 倒计时
     * 2. 检查世界
     * 3. 定期重新扫描容器
     */
    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {

        /*
         * 只在 END 阶段执行。
         */
        if (event.phase != TickEvent.Phase.END) {
            return;
        }


        /*
         * 没开启。
         */
        if (remainingTicks <= 0) {
            return;
        }


        Minecraft mc =
                Minecraft.getInstance();


        /*
         * 玩家或者世界不存在。
         *
         * 例如：
         *
         * - 回到主菜单
         * - 切换世界
         * - 断开服务器
         */
        if (mc.level == null || mc.player == null) {

            end();

            return;
        }


        /*
         * 倒计时。
         */
        remainingTicks--;


        /*
         * 时间结束。
         */
        if (remainingTicks <= 0) {

            end();

            return;
        }


        /*
         * 每秒重新扫描一次容器。
         *
         * 20 Tick = 1 秒。
         */
        if (mc.player.tickCount % 20 == 0) {

            scanContainers(mc);
        }
    }


    /*
     * ========================================================
     * 生物 / 掉落物 Outline
     * ========================================================
     */

    /**
     * 绘制猎人视野实体轮廓。
     *
     * 使用：
     *
     * OutlineBufferSource
     *
     * 不使用：
     *
     * entity.setGlowingTag(true)
     */
    @SubscribeEvent
    public static void renderHunterEntities(
            RenderLevelStageEvent event
    ) {

        /*
         * 只在实体渲染完成之后执行。
         */
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            return;
        }


        /*
         * 猎人视野没有开启。
         */
        if (!isActive()) {
            return;
        }


        Minecraft mc =
                Minecraft.getInstance();


        /*
         * 世界不存在。
         */
        if (mc.level == null || mc.player == null) {
            return;
        }


        /*
         * 高亮范围。
         */
        double radius =
                hunter_serum.HIGHLIGHT_RADIUS;


        double radiusSq =
                radius * radius;


        /*
         * Entity Renderer Dispatcher。
         */
        EntityRenderDispatcher dispatcher =
                mc.getEntityRenderDispatcher();


        /*
         * Minecraft 原版 Outline Buffer。
         */
        OutlineBufferSource outlineBuffer =
                mc.renderBuffers()
                        .outlineBufferSource();


        /*
         * 白色轮廓。
         */
        outlineBuffer.setColor(
                OUTLINE_R,
                OUTLINE_G,
                OUTLINE_B,
                OUTLINE_A
        );


        /*
         * 当前摄像机。
         */
        Camera camera =
                event.getCamera();


        Vec3 cameraPos =
                camera.getPosition();


        /*
         * 当前 PoseStack。
         */
        PoseStack poseStack =
                event.getPoseStack();


        /*
         * 当前帧插值。
         */
        float partialTick =
                event.getPartialTick();


        poseStack.pushPose();


        try {

            /*
             * 遍历客户端当前可渲染实体。
             */
            for (Entity entity :
                    mc.level.entitiesForRendering()) {


                /*
                 * 玩家自己不显示。
                 */
                if (entity == mc.player) {
                    continue;
                }


                /*
                 * 已删除实体。
                 */
                if (entity.isRemoved()) {
                    continue;
                }


                /*
                 * 只处理生物和掉落物。
                 */
                if (!(entity instanceof LivingEntity)
                        && !(entity instanceof ItemEntity)) {

                    continue;
                }


                /*
                 * 距离检查。
                 */
                if (mc.player.distanceToSqr(entity)
                        > radiusSq) {

                    continue;
                }


                /*
                 * 获取实体 Renderer。
                 */
                EntityRenderDispatcher entityDispatcher =
                        dispatcher;


                var renderer =
                        entityDispatcher.getRenderer(entity);


                if (renderer == null) {
                    continue;
                }


                /*
                 * =================================================
                 * 实体插值位置
                 * =================================================
                 */

                double x =
                        entity.xOld
                                + (
                                entity.getX()
                                        - entity.xOld
                        ) * partialTick
                                - cameraPos.x;


                double y =
                        entity.yOld
                                + (
                                entity.getY()
                                        - entity.yOld
                        ) * partialTick
                                - cameraPos.y;


                double z =
                        entity.zOld
                                + (
                                entity.getZ()
                                        - entity.zOld
                        ) * partialTick
                                - cameraPos.z;


                /*
                 * =================================================
                 * 写入 Outline Buffer
                 * =================================================
                 */
                try {

                    entityDispatcher.render(
                            entity,

                            x,
                            y,
                            z,

                            entity.getYRot(),

                            partialTick,

                            poseStack,

                            outlineBuffer,

                            entityDispatcher.getPackedLightCoords(
                                    entity,
                                    partialTick
                            )
                    );

                } catch (Exception ignored) {

                    /*
                     * 某些特殊实体 Renderer
                     * 可能不允许二次渲染。
                     *
                     * 忽略单个实体，
                     * 不影响其他实体。
                     */
                }
            }

        } finally {

            /*
             * 恢复 PoseStack。
             */
            poseStack.popPose();
        }


        /*
         * 提交 Outline Buffer。
         *
         * 非常重要。
         */
        outlineBuffer.endOutlineBatch();
    }


    /*
     * ========================================================
     * 扫描容器
     * ========================================================
     */

    /**
     * 扫描附近的容器。
     *
     * 使用：
     *
     * BaseContainerBlockEntity
     *
     * 可以检测：
     *
     * - 箱子
     * - 陷阱箱
     * - 木桶
     * - 潜影盒
     * - 其他继承 BaseContainerBlockEntity 的容器
     */
    private static void scanContainers(
            Minecraft mc
    ) {

        /*
         * 清空旧数据。
         */
        CONTAINER_BOXES.clear();


        /*
         * 玩家当前区块。
         */
        ChunkPos center =
                new ChunkPos(
                        mc.player.blockPosition()
                );


        /*
         * 根据高亮距离计算区块范围。
         *
         * +1 防止边缘漏扫描。
         */
        int chunkRadius =
                (int) Math.ceil(
                        hunter_serum.HIGHLIGHT_RADIUS
                                / 16.0
                ) + 1;


        /*
         * 遍历 Chunk。
         */
        for (
                int cx =
                center.x - chunkRadius;

                cx <=
                        center.x + chunkRadius;

                cx++
        ) {


            for (
                    int cz =
                    center.z - chunkRadius;

                    cz <=
                            center.z + chunkRadius;

                    cz++
            ) {


                /*
                 * 没加载的 Chunk 不处理。
                 */
                if (!mc.level.hasChunk(cx, cz)) {
                    continue;
                }


                /*
                 * 获取客户端 Chunk。
                 */
                LevelChunk chunk =
                        mc.level.getChunk(
                                cx,
                                cz
                        );


                /*
                 * 遍历 BlockEntity。
                 */
                for (
                        var entry :
                        chunk.getBlockEntities().entrySet()
                ) {


                    /*
                     * 只处理容器。
                     */
                    if (!(entry.getValue()
                            instanceof BaseContainerBlockEntity)) {

                        continue;
                    }


                    /*
                     * 方块位置。
                     */
                    var blockPos =
                            entry.getKey();


                    /*
                     * 创建方块 AABB。
                     */
                    AABB box =
                            new AABB(blockPos);


                    /*
                     * 进一步检查距离。
                     *
                     * 因为 Chunk 范围会比实际半径稍大。
                     */
                    if (mc.player.distanceToSqr(
                            box.getCenter()
                    ) > (
                            (double) hunter_serum.HIGHLIGHT_RADIUS
                                    * hunter_serum.HIGHLIGHT_RADIUS
                    )) {

                        continue;
                    }


                    /*
                     * 保存。
                     */
                    CONTAINER_BOXES.add(box);
                }
            }
        }
    }


    /*
     * ========================================================
     * 容器线框渲染
     * ========================================================
     */

    /**
     * 绘制容器线框。
     *
     * 阶段：
     *
     * AFTER_TRANSLUCENT_BLOCKS
     *
     * 关闭深度测试，
     * 因此容器可以穿墙显示。
     */
    @SubscribeEvent
    public static void renderContainerBoxes(
            RenderLevelStageEvent event
    ) {

        /*
         * 只在半透明方块之后。
         */
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {

            return;
        }


        /*
         * 猎人视野关闭。
         */
        if (!isActive()) {
            return;
        }


        Minecraft mc =
                Minecraft.getInstance();


        /*
         * 世界不存在。
         */
        if (mc.level == null || mc.player == null) {
            return;
        }


        /*
         * 没有容器。
         */
        if (CONTAINER_BOXES.isEmpty()) {
            return;
        }


        /*
         * 当前 PoseStack。
         */
        PoseStack poseStack =
                event.getPoseStack();


        /*
         * 当前摄像机。
         */
        Vec3 cameraPos =
                event.getCamera()
                        .getPosition();


        poseStack.pushPose();


        try {

            /*
             * 转换为摄像机相对坐标。
             */
            poseStack.translate(
                    -cameraPos.x,
                    -cameraPos.y,
                    -cameraPos.z
            );


            /*
             * Tesselator。
             */
            Tesselator tesselator =
                    Tesselator.getInstance();


            /*
             * Buffer。
             */
            BufferBuilder buffer =
                    tesselator.getBuilder();


            /*
             * POSITION_COLOR Shader。
             */
            RenderSystem.setShader(
                    GameRenderer::getPositionColorShader
            );


            /*
             * 开启 Blend。
             */
            RenderSystem.enableBlend();

            RenderSystem.defaultBlendFunc();


            /*
             * 关闭深度测试。
             *
             * 允许穿墙。
             */
            RenderSystem.disableDepthTest();


            /*
             * 设置线宽。
             */
            RenderSystem.lineWidth(
                    CONTAINER_LINE_WIDTH
            );


            /*
             * 开始绘制。
             */
            buffer.begin(
                    VertexFormat.Mode.DEBUG_LINES,
                    DefaultVertexFormat.POSITION_COLOR
            );


            /*
             * 绘制所有容器。
             */
            for (AABB box :
                    CONTAINER_BOXES) {

                LevelRenderer.renderLineBox(
                        poseStack,
                        buffer,
                        box,

                        CONTAINER_R,
                        CONTAINER_G,
                        CONTAINER_B,
                        CONTAINER_A
                );
            }


            /*
             * 提交顶点。
             */
            tesselator.end();


        } finally {

            /*
             * 恢复 OpenGL 状态。
             */
            RenderSystem.enableDepthTest();

            RenderSystem.disableBlend();

            RenderSystem.lineWidth(1.0F);

            poseStack.popPose();
        }
    }
}

