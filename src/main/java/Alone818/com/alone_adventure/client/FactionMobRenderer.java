package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.faction.MobClass;
import Alone818.com.alone_adventure.faction.PromotionTier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;

import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 派系生物渲染器。
 *
 * 模型用原版人形模型（史蒂夫模型，
 * 玩家布局，64x64 皮肤格式）。
 *
 * 贴图按 派系_军衔 到
 *
 *   assets/alone_adventure/textures/entity/faction/
 *
 *   例：empire_sergeant.png
 *
 * 贴图未提供时依次回落：
 *
 * 1. 军衔专属贴图（empire_general.png）
 * 2. 职业基础贴图（empire_soldier.png）
 * 3. 原版史蒂夫皮肤
 *
 * 模组生物不会因为缺图渲染成紫黑棋盘格。
 *
 * 喝药自救时右手抬到脸前
 * （见 FactionHumanoidModel）。
 *
 * 突袭成员额外叠加一层派系色描边
 * （见 FactionRaidGlowLayer），
 * 隔着墙也能认出是哪个派系的突袭部队。
 */
public class FactionMobRenderer
        extends MobRenderer<FactionMobEntity, HumanoidModel<FactionMobEntity>> {

    /** 缺省贴图：原版史蒂夫 */
    private static final ResourceLocation DEFAULT_STEVE =
            DefaultPlayerSkin.getDefaultSkin();

    /** 贴图候选（依次回落），最多两个 */
    private final ResourceLocation[] textureCandidates;

    /** 实际使用的贴图，首次渲染时解析并缓存 */
    private ResourceLocation resolvedTexture;

    /** 是否已完成贴图解析 */
    private boolean textureResolved = false;

    public FactionMobRenderer(
            EntityRendererProvider.Context context,
            String factionId,
            MobClass mobClass,
            PromotionTier tier
    ) {

        super(
                context,
                new FactionHumanoidModel(
                        context.bakeLayer(
                                ModelLayers.PLAYER
                        )
                ),
                0.5F
        );

        this.textureCandidates =
                new ResourceLocation[]{
                        factionTexture(
                                factionId,
                                mobClass.getSuffix(tier)
                        ),
                        factionTexture(
                                factionId,
                                mobClass.getSuffix(
                                        PromotionTier.BASE
                                )
                        )
                };

        // 盔甲层（内层胸甲模型 + 外层护甲模型）
        // 用同一个 HumanoidModel 实例，遮挡贴图由原版玩家盔甲贴图提供
        this.addLayer(
                new HumanoidArmorLayer<FactionMobEntity, HumanoidModel<FactionMobEntity>, HumanoidModel<FactionMobEntity>>(
                        this,
                        new FactionHumanoidModel(
                                context.bakeLayer(
                                        ModelLayers.PLAYER_INNER_ARMOR
                                )
                        ),
                        new FactionHumanoidModel(
                                context.bakeLayer(
                                        ModelLayers.PLAYER_OUTER_ARMOR
                                )
                        ),
                        context.getModelManager()
                )
        );

        // 持有物品层：负责把实体手上的 Item 动态渲染到手臂上
        this.addLayer(
                new ItemInHandLayer<>(this, context.getItemInHandRenderer())
        );

        // 突袭成员的派系色描边层
        this.addLayer(
                new FactionRaidGlowLayer(
                        this,
                        factionId
                )
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            FactionMobEntity entity
    ) {

        /*
         * 懒解析：
         * 渲染时资源已加载完毕，
         * 每个渲染器只解析一次。
         */
        if (!textureResolved) {

            textureResolved = true;

            resolvedTexture = resolveTexture();
        }

        return resolvedTexture;
    }

    /**
     * 贴图候选里第一个存在的，
     * 都不存在回落史蒂夫。
     */
    private ResourceLocation resolveTexture() {

        for (ResourceLocation candidate : textureCandidates) {

            if (Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(candidate)
                    .isPresent()) {

                return candidate;
            }
        }

        return DEFAULT_STEVE;
    }

    /**
     * 派系贴图路径。
     */
    private static ResourceLocation factionTexture(
            String factionId,
            String suffix
    ) {

        return new ResourceLocation(
                Alone_adventure.MODID,
                "textures/entity/faction/"
                        + factionId
                        + "_"
                        + suffix
                        + ".png"
        );
    }

    /**
     * 按军衔放大体型：
     * 基础兵 100% / 一阶 115% / 二阶 130%。
     */
    @Override
    protected void scale(
            FactionMobEntity entity,
            PoseStack poseStack,
            float partialTick
    ) {

        float scale =
                1.0F
                        + 0.15F
                        * entity.getRank()
                        .ordinal();

        poseStack.scale(
                scale,
                scale,
                scale
        );
    }

    /**
     * 突袭成员的派系色描边层。
     *
     * 只有带突袭标记的实体才画这一层，
     * 颜色取派系主题色
     * （帝国金 / 亡灵淡蓝 / 恶魔绯红 / 部落青绿）。
     *
     * 用 RenderType.outline 走一遍模型，
     * 顶点色即描边色，
     * 这是原版发光生物轮廓的同一套机制。
     */
    private static class FactionRaidGlowLayer
            extends RenderLayer<FactionMobEntity, HumanoidModel<FactionMobEntity>> {

        private final ResourceLocation outlineTexture;

        FactionRaidGlowLayer(
                RenderLayerParent<FactionMobEntity, HumanoidModel<FactionMobEntity>> parent,
                String factionId
        ) {

            super(parent);

            this.outlineTexture =
                    new ResourceLocation(
                            Alone_adventure.MODID,
                            "textures/entity/faction/"
                                    + factionId
                                    + ".png"
                    );
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                FactionMobEntity entity,
                float limbSwing,
                float limbSwingAmount,
                float partialTick,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {

            // 只画突袭成员
            if (!entity.isRaidMember()) {
                return;
            }

            getParentModel()
                    .setupAnim(
                            entity,
                            limbSwing,
                            limbSwingAmount,
                            ageInTicks,
                            netHeadYaw,
                            headPitch
                    );

            RenderType renderType =
                    RenderType.outline(
                            outlineTexture
                    );

            VertexConsumer consumer =
                    buffer.getBuffer(renderType);

            int color =
                    entity.getFaction()
                            .getColor();

            getParentModel()
                    .renderToBuffer(
                            poseStack,
                            consumer,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            color,
                            1.0F,
                            1.0F,
                            1.0F
                    );
        }
    }

    /**
     * 人形模型：
     * - 喝药时右手抬到脸前
     *   （SelfHealDrinkGoal 的喝药姿势，
     *    手里的治疗药水会跟着举到嘴边）。
     *
     * - 施法时双手举高念咒
     *   （FactionMage 的 CastSpellGoal 控制）。
     *
     * 突袭成员的派系色描边
     * （见 FactionRaidGlowLayer）。
     */
    private static class FactionHumanoidModel
            extends HumanoidModel<FactionMobEntity> {

        FactionHumanoidModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(
                FactionMobEntity entity,
                float limbSwing,
                float limbSwingAmount,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {

            super.setupAnim(
                    entity,
                    limbSwing,
                    limbSwingAmount,
                    ageInTicks,
                    netHeadYaw,
                    headPitch
            );

            if (entity.isDrinkingPotion()) {

                // 右手举到脸前
                rightArm.xRot = -2.0F;
                rightArm.yRot = 0.2F;
                rightArm.zRot = 0.15F;
            }

            if (entity.isCastingSpell()) {

                // 双手举高念咒（原版拉弓姿势的变体）
                // 右臂：原版拉弓角度
                rightArm.xRot = Mth.cos(ageInTicks * 0.09F) * 0.5F + 0.5F;
                rightArm.yRot = 0.0F;
                rightArm.zRot = -0.15F;

                // 左臂：对称抬起
                leftArm.xRot = Mth.cos(ageInTicks * 0.09F) * 0.5F + 0.5F;
                leftArm.yRot = 0.0F;
                leftArm.zRot = 0.15F;
            }

            // 拉弓 / 弩时，让原版 HumanoidModel.setupHandItems 接管动画
            // 不需要额外设置手臂角度，只要手里拿着弓/弩且 isUsingItem=true
            // 渲染器会自动播放拉弓姿势
        }
    }
}
