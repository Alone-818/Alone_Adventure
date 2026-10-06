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
 * Hunter Vision
 *
 * Minecraft 1.20.1
 * Forge 47.4.23
 */
@Mod.EventBusSubscriber(
        modid = Alone_adventure.MODID,
        value = Dist.CLIENT
)
public class HunterVisionClient {

    /*
     * ============================================================
     * 状态
     * ============================================================
     */

    private static int remainingTicks = 0;


    /*
     * ============================================================
     * Shader
     * ============================================================
     */

    private static final ResourceLocation HUNTER_VISION_SHADER =
            new ResourceLocation(
                    Alone_adventure.MODID,
                    "shaders/post/hunter_vision.json"
            );


    /*
     * ============================================================
     * 容器
     * ============================================================
     */

    private static final List<AABB> CONTAINER_BOXES =
            new ArrayList<>();


    private static final float CONTAINER_R = 1.0F;
    private static final float CONTAINER_G = 1.0F;
    private static final float CONTAINER_B = 1.0F;
    private static final float CONTAINER_A = 0.90F;

    private static final float CONTAINER_LINE_WIDTH = 2.0F;


    /*
     * ============================================================
     * 基础接口
     * ============================================================
     */

    public static boolean isActive() {
        return remainingTicks > 0;
    }


    public static int getRemainingTicks() {
        return remainingTicks;
    }


    /*
     * ============================================================
     * 开启 Hunter Vision
     * ============================================================
     */

    public static void activate(int durationTicks) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }


        remainingTicks =
                Math.max(1, durationTicks);


        /*
         * 加载黑白 Post Effect。
         */
        if (mc.gameRenderer.currentEffect() == null) {

            try {

                mc.gameRenderer.loadEffect(
                        HUNTER_VISION_SHADER
                );

            } catch (Exception ignored) {

                /*
                 * Shader 加载失败时不让游戏崩溃。
                 */
            }
        }


        /*
         * 扫描容器。
         */
        scanContainers(mc);
    }


    /*
     * ============================================================
     * 关闭 Hunter Vision
     * ============================================================
     */

    public static void end() {

        remainingTicks = 0;

        CONTAINER_BOXES.clear();

        Minecraft mc =
                Minecraft.getInstance();


        if (mc.gameRenderer.currentEffect() != null) {

            try {

                mc.gameRenderer.shutdownEffect();

            } catch (Exception ignored) {

            }
        }
    }


    /*
     * ============================================================
     * Client Tick
     * ============================================================
     */

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }


        if (remainingTicks <= 0) {
            return;
        }


        Minecraft mc =
                Minecraft.getInstance();


        if (mc.level == null || mc.player == null) {

            end();

            return;
        }


        remainingTicks--;


        if (remainingTicks <= 0) {

            end();

            return;
        }


        /*
         * 每 20 tick 扫描一次容器。
         */
        if (mc.player.tickCount % 20 == 0) {

            scanContainers(mc);
        }
    }


    /*
     * ============================================================
     * Hunter Vision 实体轮廓
     * ============================================================
     *
     * 核心逻辑：
     *
     * 1. 找到附近 LivingEntity / ItemEntity
     * 2. 使用 OutlineBufferSource 渲染轮廓
     * 3. endOutlineBatch()
     * 4. LevelRenderer.doEntityOutline()
     *
     * 第 4 步非常重要。
     *
     * OutlineBufferSource 只是把轮廓写入
     * Minecraft 的 entity outline framebuffer。
     *
     * doEntityOutline() 才会运行 Minecraft
     * 自己的 entity outline 后处理。
     * ============================================================
     */

    @SubscribeEvent
    public static void renderHunterEntities(
            RenderLevelStageEvent event
    ) {

        /*
         * 必须等实体正常渲染完。
         */
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            return;
        }


        if (!isActive()) {
            return;
        }


        Minecraft mc =
                Minecraft.getInstance();


        if (mc.level == null || mc.player == null) {
            return;
        }


        /*
         * ========================================================
         * 高亮范围
         * ========================================================
         */

        double radius =
                hunter_serum.HIGHLIGHT_RADIUS;

        double radiusSq =
                radius * radius;


        /*
         * ========================================================
         * Entity Renderer
         * ========================================================
         */

        EntityRenderDispatcher dispatcher =
                mc.getEntityRenderDispatcher();


        /*
         * ========================================================
         * Outline Buffer
         * ========================================================
         */

        OutlineBufferSource outlineBuffer =
                mc.renderBuffers()
                        .outlineBufferSource();


        /*
         * 白色轮廓。
         */
        outlineBuffer.setColor(
                255,
                255,
                255,
                255
        );


        /*
         * ========================================================
         * 摄像机
         * ========================================================
         */

        Camera camera =
                event.getCamera();


        Vec3 cameraPos =
                camera.getPosition();


        PoseStack poseStack =
                event.getPoseStack();


        float partialTick =
                event.getPartialTick();


        poseStack.pushPose();

        try {

            /*
             * ====================================================
             * 遍历当前世界实体
             * ====================================================
             */

            for (Entity entity :
                    mc.level.entitiesForRendering()) {


                /*
                 * 不高亮玩家自己。
                 */
                if (entity == mc.player) {
                    continue;
                }


                /*
                 * 删除实体跳过。
                 */
                if (entity.isRemoved()) {
                    continue;
                }


                /*
                 * 只处理：
                 *
                 * LivingEntity
                 * ItemEntity
                 */
                if (!(entity instanceof LivingEntity)
                        && !(entity instanceof ItemEntity)) {

                    continue;
                }


                /*
                 * =================================================
                 * 距离检测
                 * =================================================
                 */

                if (mc.player.distanceToSqr(entity)
                        > radiusSq) {

                    continue;
                }


                /*
                 * =================================================
                 * 获取 Renderer
                 * =================================================
                 */

                var renderer =
                        dispatcher.getRenderer(entity);


                if (renderer == null) {
                    continue;
                }


                /*
                 * =================================================
                 * 插值位置
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

                    dispatcher.render(
                            entity,

                            x,
                            y,
                            z,

                            entity.getYRot(),

                            partialTick,

                            poseStack,

                            outlineBuffer,

                            dispatcher.getPackedLightCoords(
                                    entity,
                                    partialTick
                            )
                    );

                } catch (Exception ignored) {

                    /*
                     * 某些特殊 Renderer
                     * 不能安全地二次渲染。
                     *
                     * 忽略该实体即可。
                     */
                }
            }

        } finally {

            poseStack.popPose();
        }


        /*
         * ========================================================
         * 提交 Outline Buffer
         * ========================================================
         */

        outlineBuffer.endOutlineBatch();


        /*
         * ========================================================
         * ★ 关键修复 ★
         * ========================================================
         *
         * OutlineBufferSource 只是把轮廓写入
         * Minecraft 的 entity outline framebuffer。
         *
         * 必须调用 LevelRenderer.doEntityOutline()
         * 才会真正执行 outline 后处理。
         *
         * Minecraft 1.20.1 的 LevelRenderer
         * 本身就提供这个公开方法。
         * ========================================================
         */

        try {

            mc.levelRenderer.doEntityOutline();

        } catch (Exception ignored) {

            /*
             * 防止某些渲染状态下异常导致客户端崩溃。
             */
        }
    }


    /*
     * ============================================================
     * 容器扫描
     * ============================================================
     */

    private static void scanContainers(
            Minecraft mc
    ) {

        CONTAINER_BOXES.clear();


        ChunkPos center =
                new ChunkPos(
                        mc.player.blockPosition()
                );


        /*
         * 根据高亮半径计算 Chunk 范围。
         */
        int chunkRadius =
                (int) Math.ceil(
                        hunter_serum.HIGHLIGHT_RADIUS
                                / 16.0
                ) + 1;


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
                 * Chunk 未加载就跳过。
                 */
                if (!mc.level.hasChunk(cx, cz)) {
                    continue;
                }


                LevelChunk chunk =
                        mc.level.getChunk(
                                cx,
                                cz
                        );


                /*
                 * =================================================
                 * Block Entity
                 * =================================================
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


                    var blockPos =
                            entry.getKey();


                    AABB box =
                            new AABB(blockPos);


                    /*
                     * 实际距离再次检查。
                     */
                    if (mc.player.distanceToSqr(
                            box.getCenter()
                    ) > (
                            (double) hunter_serum.HIGHLIGHT_RADIUS
                                    * hunter_serum.HIGHLIGHT_RADIUS
                    )) {

                        continue;
                    }


                    CONTAINER_BOXES.add(box);
                }
            }
        }
    }


    /*
     * ============================================================
     * 容器线框
     * ============================================================
     */

    @SubscribeEvent
    public static void renderContainerBoxes(
            RenderLevelStageEvent event
    ) {

        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {

            return;
        }


        if (!isActive()) {
            return;
        }


        Minecraft mc =
                Minecraft.getInstance();


        if (mc.level == null || mc.player == null) {
            return;
        }


        if (CONTAINER_BOXES.isEmpty()) {
            return;
        }


        PoseStack poseStack =
                event.getPoseStack();


        Vec3 cameraPos =
                event.getCamera()
                        .getPosition();


        poseStack.pushPose();

        try {

            /*
             * ====================================================
             * 摄像机相对坐标
             * ====================================================
             */

            poseStack.translate(
                    -cameraPos.x,
                    -cameraPos.y,
                    -cameraPos.z
            );


            Tesselator tesselator =
                    Tesselator.getInstance();


            BufferBuilder buffer =
                    tesselator.getBuilder();


            /*
             * Position Color Shader。
             */
            RenderSystem.setShader(
                    GameRenderer::getPositionColorShader
            );


            /*
             * Blend。
             */
            RenderSystem.enableBlend();

            RenderSystem.defaultBlendFunc();


            /*
             * 穿墙。
             */
            RenderSystem.disableDepthTest();


            /*
             * 线宽。
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
             * 提交。
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