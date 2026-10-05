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
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import java.util.Iterator;
import java.util.List;

/**
 * 猎人血清 —— 客户端视野渲染
 *
 * 激活期间（由 HunterVisionPacket 调用 {@link #activate}）：
 * 1. 黑白视野：加载去色后处理着色器（shaders/post/hunter_vision.json）
 * 2. 生物 + 凋落物：绘制发光级联层（白色描边，穿墙可见）
 * 3. 容器（箱子/木桶/潜影盒等）：保留白色线框高亮，每 20 tick 扫描范围内区块
 *
 * 由于 setGlowingTag 是服务端同步字段，客户端单独调用无效。
 * 因此我们在 AFTER_TRANSLUCENT_BLOCKS 阶段手动绘制发光级联。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, value = Dist.CLIENT)
public class HunterVisionClient {

    /** 剩余 tick（0 = 未激活） */
    private static int remainingTicks = 0;
    /** 最近一次扫描到的容器方块盒 */
    private static final List<AABB> CONTAINER_BOXES = new ArrayList<>();

    /**
     * 本次被我们点亮的实体，
     * 用于在渲染阶段绘制发光级联层。
     */
    private static final List<Entity> GLOWED =
            new ArrayList<>();
    /**
     * 猎人视野轮廓颜色。
     *
     * 255,255,255 = 白色
     */
    private static final int OUTLINE_R = 255;
    private static final int OUTLINE_G = 255;
    private static final int OUTLINE_B = 255;
    private static final int OUTLINE_A = 255;
    /** 高亮颜色：容器线框用白色 */
    private static final float[] COLOR_WHITE = {1.0F, 1.0F, 1.0F, 0.9F};
    /** 线框线条粗细 */
    private static final float LINE_WIDTH = 10.0F;

    /** 去色后处理着色器 */
    private static final ResourceLocation VISION_SHADER =
            new ResourceLocation(Alone_adventure.MODID, "shaders/post/hunter_vision.json");

    /** 白色轮廓纹理（1x1 白点） */
    private static final ResourceLocation WHITE_OUTLINE =
            new ResourceLocation(Alone_adventure.MODID, "textures/entity/faction/white_outline.png");

    public static boolean isActive() {
        return remainingTicks > 0;
    }

    /** 开启猎人视野（仅客户端，由网络包触发） */
    public static void activate(int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        remainingTicks = durationTicks;
        try {
            mc.gameRenderer.loadEffect(VISION_SHADER);
        } catch (Exception ignored) {
            // 着色器加载失败（资源缺失等）：保留线框高亮，仅无去色效果
        }
    }

    /**
     * =========================================================
     * 实体真正轮廓
     * =========================================================
     *
     * 这里是整个系统的核心。
     *
     * 不使用 AABB。
     *
     * 直接让 Minecraft 原版 EntityRenderer
     * 把实体模型写入 OutlineBufferSource。
     */
    @SubscribeEvent
    public static void renderHunterOutline(
            RenderLevelStageEvent event) {

        /*
         * AFTER_ENTITIES 是这里最合适的阶段。
         *
         * Forge 1.20.1 明确提供了 AFTER_ENTITIES。
         */
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        if (remainingTicks <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        /*
         * 原版 OutlineBufferSource。
         */
        OutlineBufferSource outlineBuffer =
                mc.renderBuffers().outlineBufferSource();

        /*
         * 设置轮廓颜色。
         *
         * 白色。
         */
        outlineBuffer.setColor(
                OUTLINE_R,
                OUTLINE_G,
                OUTLINE_B,
                OUTLINE_A
        );

        /*
         * EntityRenderDispatcher。
         */
        EntityRenderDispatcher dispatcher =
                mc.getEntityRenderDispatcher();

        /*
         * 当前摄像机。
         */
        Camera camera =
                event.getCamera();

        Vec3 cameraPos =
                camera.getPosition();

        /*
         * 当前部分 tick。
         *
         * 用于实体动画插值。
         */
        float partialTick =
                event.getPartialTick();

        /*
         * PoseStack。
         */
        PoseStack poseStack =
                event.getPoseStack();

        poseStack.pushPose();

        /*
         * 注意：
         *
         * EntityRenderDispatcher.render()
         * 接收的是相对于摄像机的坐标。
         */
        double radius =
                hunter_serum.HIGHLIGHT_RADIUS;

        double radiusSq =
                radius * radius;

        /*
         * 遍历客户端当前正在渲染的实体。
         */
        for (Entity entity :
                mc.level.entitiesForRendering()) {

            /*
             * 自己不显示。
             */
            if (entity == mc.player) {
                continue;
            }

            /*
             * 只处理：
             *
             * 生物
             * 掉落物
             */
            if (!(entity instanceof LivingEntity)
                    && !(entity instanceof ItemEntity)) {
                continue;
            }

            /*
             * 距离。
             */
            if (mc.player.distanceToSqr(entity)
                    > radiusSq) {
                continue;
            }

            /*
             * 已移除的实体跳过。
             */
            if (entity.isRemoved()) {
                continue;
            }

            /*
             * 当前实体坐标。
             *
             * 使用插值后的坐标。
             */
            double x =
                    entity.xOld
                            + (entity.getX() - entity.xOld)
                            * partialTick
                            - cameraPos.x;

            double y =
                    entity.yOld
                            + (entity.getY() - entity.yOld)
                            * partialTick
                            - cameraPos.y;

            double z =
                    entity.zOld
                            + (entity.getZ() - entity.zOld)
                            * partialTick
                            - cameraPos.z;

            /*
             * 获取实体 Renderer。
             */
            var renderer =
                    dispatcher.getRenderer(entity);

            if (renderer == null) {
                continue;
            }

            /*
             * 真正的模型轮廓渲染。
             *
             * EntityRenderDispatcher 会调用实体自己的
             * renderer / model。
             *
             * OutlineBufferSource 会把模型写入 outline buffer。
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
                 * 某些特殊实体 Renderer 如果不支持重复渲染，
                 * 不让它影响其他实体。
                 */
            }
        }

        poseStack.popPose();

        /*
         * 非常重要：
         *
         * 把 OutlineBufferSource 提交。
         */
        outlineBuffer.endOutlineBatch();
    }


    /** 结束视野：卸载着色器、清除高亮列表 */
    public static void end() {
        remainingTicks = 0;
        CONTAINER_BOXES.clear();
        GLOWED.clear();
        Minecraft.getInstance().gameRenderer.shutdownEffect();
    }

    /** 点亮一个实体并记账（用于后续绘制发光层） */
    private static void glow(Entity entity) {
        if (!GLOWED.contains(entity)) {
            GLOWED.add(entity);
        }
    }

    /** 每 tick：倒计时 + 定期重扫容器 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (remainingTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            end();
            return;
        }

        remainingTicks--;
        if (remainingTicks <= 0) {
            end();
            return;
        }

        // 生物 / 凋落物走原版发光（每 tick 跟随实体进出视野）
        glowEntities(mc);

        // 容器每 20 tick 重扫一次（方块不会移动，无需每帧/每 tick）
        if (mc.player.tickCount % 20 == 0) {
            scanContainers(mc);
        }
    }

    /**
     * 点亮范围内的生物与凋落物。
     *
     * 容器是方块，原版发光机制覆盖不到，
     * 仍走 renderLevelStage 里的白色线框。
     */
    private static void glowEntities(Minecraft mc) {
        double radiusSq = (double) hunter_serum.HIGHLIGHT_RADIUS * hunter_serum.HIGHLIGHT_RADIUS;

        for (Entity entity : mc.level.entitiesForRendering()) {

            if (entity == mc.player) continue;

            boolean isMob = entity instanceof LivingEntity;

            boolean isDrop = entity instanceof ItemEntity;

            if (!isMob && !isDrop) continue;

            if (mc.player.distanceToSqr(entity) > radiusSq) continue;

            glow(entity);
        }

        // 已离开视野 / 已卸载的实体不再点亮
        GLOWED.removeIf(entity -> entity.isRemoved() || !inRange(mc, entity));
    }

    /** 实体是否仍在视野半径内且已加载 */
    private static boolean inRange(Minecraft mc, Entity entity) {
        if (entity.level() != mc.level) return false;
        double radiusSq = (double) hunter_serum.HIGHLIGHT_RADIUS * hunter_serum.HIGHLIGHT_RADIUS;
        return mc.player.distanceToSqr(entity) <= radiusSq;
    }

    /** 扫描范围内已加载区块中的容器（箱子/木桶/潜影盒等 BaseContainerBlockEntity） */
    private static void scanContainers(Minecraft mc) {
        CONTAINER_BOXES.clear();
        int chunkRadius = hunter_serum.HIGHLIGHT_RADIUS >> 4;
        ChunkPos center = new ChunkPos(mc.player.blockPosition());
        for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; cx++) {
            for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; cz++) {
                if (!mc.level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof BaseContainerBlockEntity) {
                        CONTAINER_BOXES.add(new AABB(entry.getKey()));
                    }
                }
            }
        }
    }

    /** 渲染容器线框：生物 / 凋落物已改走原版发光，只有方块容器还靠线框 */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (remainingTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 范围内一个容器都没有：不必批渲染
        if (CONTAINER_BOXES.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // 手动批渲染直接控制 GL 状态：关闭深度测试 → 穿墙可见；
        // 顶点颜色（白色）直接写入 POSITION_COLOR，一批画完
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // 透墙显示
        RenderSystem.lineWidth(LINE_WIDTH);
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // 容器（箱子 / 木桶 / 潜影盒等）：白色
        for (AABB box : CONTAINER_BOXES) {
            LevelRenderer.renderLineBox(poseStack, buffer, box,
                    COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
        }

        tesselator.end();

        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }
}
