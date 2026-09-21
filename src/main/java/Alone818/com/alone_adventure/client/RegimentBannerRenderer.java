package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.RegimentBannerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 连队团旗的旗子渲染器（客户端）。
 *
 * 直接渲染一面原版红色旗帜的物品模型（{@code ItemDisplayContext.NONE}
 * 即不经过任何视角变换的原始方块模型——一面立着的旗），
 * 按插旗时玩家朝向旋转、放大 1.5 倍立在落点上。
 * 不注册渲染器的话旗子实体在客户端不可见。
 */
@OnlyIn(Dist.CLIENT)
public class RegimentBannerRenderer extends EntityRenderer<RegimentBannerEntity> {

    /** 旗帜样式：原版红色旗帜 */
    private static final ItemStack BANNER = new ItemStack(Items.RED_BANNER);

    /** 旗帜整体放大倍数（原模型 1 格高 → 1.5 格） */
    private static final float SCALE = 1.5F;

    private final ItemRenderer itemRenderer;

    public RegimentBannerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(RegimentBannerEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 面向插旗玩家的朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        // 原始模型以中心为原点（y ±0.5），抬高半高再放大，使旗底正好落在插旗点
        poseStack.translate(0.0D, 0.5D * SCALE, 0.0D);
        poseStack.scale(SCALE, SCALE, SCALE);
        this.itemRenderer.renderStatic(BANNER, ItemDisplayContext.NONE,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, null, entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RegimentBannerEntity entity) {
        // 旗帜经由 ItemRenderer 直接写入方块图集缓冲，此返回值不会被实际采样
        return InventoryMenu.BLOCK_ATLAS;
    }
}
