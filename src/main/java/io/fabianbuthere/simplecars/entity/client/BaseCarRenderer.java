package io.fabianbuthere.simplecars.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BaseCarRenderer extends MobRenderer<BaseCarEntity, BaseCarModel<BaseCarEntity>> {
    public BaseCarRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new BaseCarModel<>(pContext.bakeLayer(ModModelLayers.BASE_CAR_LAYER)), 1.5f);
    }

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getTextureLocation(BaseCarEntity pEntity) {
        return new ResourceLocation(SimplecarsMod.MOD_ID, "textures/entity/empty.png");
    }

    @Override
    public void render(BaseCarEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }
}
