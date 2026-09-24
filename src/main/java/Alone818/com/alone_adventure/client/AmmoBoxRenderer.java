package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.entity.AmmoBoxEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class AmmoBoxRenderer extends EntityRenderer<AmmoBoxEntity> {

    private final BlockRenderDispatcher blockRenderer;

    private final Font font;

    public AmmoBoxRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);

        this.blockRenderer =
                Minecraft.getInstance().getBlockRenderer();

        this.font =
                context.getFont();

        /*
         * 弹药箱阴影
         */
        this.shadowRadius = 0.05F;
    }

    @Override
    public void render(
            AmmoBoxEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {

        /*
         * =====================================================
         * 渲染弹药箱模型
         * =====================================================
         */

        poseStack.pushPose();

        /*
         * 弹药箱大小：
         *
         * 0.5F = 原版方块大小的 50%
         */
        poseStack.scale(
                0.5F,
                0.5F,
                0.5F
        );

        /*
         * 将箱子模型中心对齐到实体中心。
         */
        poseStack.translate(
                0.5D,
                0.0D,
                0.5D
        );

        /*
         * 根据实体朝向旋转。
         */
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F - entityYaw
                )
        );

        /*
         * 使用原版箱子模型。
         */
        blockRenderer.renderSingleBlock(
                Blocks.CHEST.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();


        /*
         * =====================================================
         * 渲染头顶信息
         * =====================================================
         */

        renderAmmoBoxInfo(
                entity,
                poseStack,
                buffer,
                packedLight
        );

        /*
         * 注意：
         *
         * 这里不要调用：
         *
         * super.render(...)
         *
         * 因为我们已经自己绘制文字。
         *
         * 如果调用 super.render()，
         * Minecraft 还会尝试绘制 CustomName。
         */
    }


    /**
     * 绘制弹药箱头顶的三行文字。
     *
     * 第一行：
     * 弹药箱
     *
     * 第二行：
     * 弹药：32/32
     *
     * 第三行：
     * 剩余时间：60s
     */
    private void renderAmmoBoxInfo(
            AmmoBoxEntity entity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {

        /*
         * =====================================================
         * 获取显示内容
         * =====================================================
         */

        Component title =
                Component.translatable(
                        "entity.alone_adventure.ammo_box"
                );

        Component ammoText =
                Component.translatable(
                        "entity.alone_adventure.ammo_box_ammo",
                        entity.getAmmo(),
                        AmmoBoxEntity.DEFAULT_AMMO
                );

        Component timeText =
                Component.translatable(
                        "entity.alone_adventure.ammo_box_time",
                        entity.getRemainingSeconds()
                );


        /*
         * =====================================================
         * 开始文字渲染
         * =====================================================
         */

        poseStack.pushPose();

        /*
         * =====================================================
         * 文字距离弹药箱顶部的高度
         * =====================================================
         *
         * 实体高度现在为 0.5。
         *
         * 0.5 + 0.35 = 0.85
         *
         * 也就是说文字从弹药箱上方约 0.35 格开始。
         */
        poseStack.translate(
                0.0D,
                entity.getBbHeight() + 1.5D,
                0.0D
        );

        /*
         * =====================================================
         * 让文字始终面向玩家
         * =====================================================
         */
        poseStack.mulPose(
                this.entityRenderDispatcher.cameraOrientation()
        );

        /*
         * =====================================================
         * 文字大小
         * =====================================================
         *
         * Minecraft 默认实体名称文字大约使用：
         *
         * 0.025F
         *
         * 这里保持正常大小。
         */
        poseStack.scale(
                -0.025F,
                -0.025F,
                0.025F
        );


        /*
         * =====================================================
         * 第一行：弹药箱
         * =====================================================
         */

        drawCenteredText(
                title,
                poseStack,
                buffer,
                packedLight,
                0
        );


        /*
         * =====================================================
         * 第二行：弹药数量
         * =====================================================
         */

        drawCenteredText(
                ammoText,
                poseStack,
                buffer,
                packedLight,
                10
        );


        /*
         * =====================================================
         * 第三行：剩余时间
         * =====================================================
         */

        drawCenteredText(
                timeText,
                poseStack,
                buffer,
                packedLight,
                20
        );

        poseStack.popPose();
    }


    /**
     * 绘制一行居中的文字。
     */
    private void drawCenteredText(
            Component text,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int y
    ) {

        int width =
                font.width(text);

        font.drawInBatch(
                text,

                /*
                 * X：
                 * 根据文字宽度向左移动一半，
                 * 实现水平居中。
                 */
                -width / 2.0F,

                /*
                 * Y：
                 * 每行相差 10 像素。
                 */
                y,

                /*
                 * 文字颜色：
                 * 白色
                 */
                0xFFFFFFFF,

                /*
                 * 不使用阴影。
                 */
                false,

                poseStack.last().pose(),

                buffer,

                Font.DisplayMode.NORMAL,

                /*
                 * 文字背景。
                 *
                 * 0 = 不显示背景。
                 */
                0,

                packedLight
        );
    }


    @Override
    public ResourceLocation getTextureLocation(
            AmmoBoxEntity entity
    ) {

        /*
         * BlockRenderDispatcher 自己负责
         * 箱子实际纹理。
         *
         * EntityRenderer 这里需要一个合法的
         * ResourceLocation。
         */
        return new ResourceLocation(
                "minecraft",
                "textures/atlas/blocks.png"
        );
    }
}