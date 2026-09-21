package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.hunter_serum;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
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
 * 猎人血清 —— 客户端视野渲染
 *
 * 激活期间（由 HunterVisionPacket 调用 {@link #activate}）：
 * 1. 黑白视野：加载去色后处理着色器（shaders/post/hunter_vision.json）
 * 2. 统一白色线框高亮（几何沿用原版 LevelRenderer.renderLineBox 的 12 边盒）：
 *    - 生物：白色
 *    - 凋落物：白色（盒子外扩 0.15 便于辨认）
 *    - 容器（箱子/木桶/潜影盒等）：白色，每 20 tick 扫描范围内区块
 * 手动批渲染直接控制 GL 状态：关闭深度测试，实体与凋落物
 * 可穿墙看到；全部为纯客户端渲染（仅本人可见），无同步问题。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, value = Dist.CLIENT)
public class HunterVisionClient {

    /** 剩余 tick（0 = 未激活） */
    private static int remainingTicks = 0;
    /** 最近一次扫描到的容器方块盒 */
    private static final List<AABB> CONTAINER_BOXES = new ArrayList<>();

    /** 高亮颜色：统一白色，线条加粗 */
    private static final float[] COLOR_WHITE = {1.0F, 1.0F, 1.0F, 0.9F};
    /** 线框线条粗细 */
    private static final float LINE_WIDTH = 10.0F;

    /** 去色后处理着色器 */
    private static final ResourceLocation VISION_SHADER =
            new ResourceLocation(Alone_adventure.MODID, "shaders/post/hunter_vision.json");

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

    /** 结束视野：卸载着色器（线框为纯渲染，无需清理） */
    public static void end() {
        remainingTicks = 0;
        CONTAINER_BOXES.clear();
        Minecraft.getInstance().gameRenderer.shutdownEffect();
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

        // 容器每 20 tick 重扫一次（方块不会移动，无需每帧/每 tick）
        if (mc.player.tickCount % 20 == 0) {
            scanContainers(mc);
        }
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

    /** 渲染统一白色线框：生物、凋落物、容器每帧实时包围盒 */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (remainingTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double radiusSq = (double) hunter_serum.HIGHLIGHT_RADIUS * hunter_serum.HIGHLIGHT_RADIUS;

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

        // 1. 生物：白色
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (mc.player.distanceToSqr(entity) > radiusSq) continue;

            LevelRenderer.renderLineBox(poseStack, buffer, entity.getBoundingBox(),
                    COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
        }

        // 2. 凋落物：白色（盒子外扩 0.15 便于辨认）
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity)) continue;
            if (mc.player.distanceToSqr(entity) > radiusSq) continue;

            AABB box = entity.getBoundingBox().inflate(0.15);
            LevelRenderer.renderLineBox(poseStack, buffer, box,
                    COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
        }

        // 3. 容器：白色
        for (AABB box : CONTAINER_BOXES) {
            LevelRenderer.renderLineBox(poseStack, buffer, box,
                    COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
        }

        tesselator.end();

        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }
}
