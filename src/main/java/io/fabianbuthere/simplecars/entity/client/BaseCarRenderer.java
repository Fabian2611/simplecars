package io.fabianbuthere.simplecars.entity.client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import io.fabianbuthere.simplecars.item.custom.AbstractFrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class BaseCarRenderer extends MobRenderer<BaseCarEntity, BaseCarModel<BaseCarEntity>> {
    private final Map<String, ResourceLocation> plateTextureCache = new HashMap<>();
    private final EntityRendererProvider.Context context;
    private final Map<ModelLayerLocation, BaseCarModel<BaseCarEntity>> modelCache = new HashMap<>();

    public BaseCarRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new BaseCarModel<>(pContext.bakeLayer(ModModelLayers.BASE_CAR_LAYER)), 1.5f);
        this.context = pContext;
    }

    public static BufferedImage generatePlate(String text) {
        int width = 128, height = 32;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Border
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, width - 1, height - 1);

        // Text
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text.toUpperCase());
        int x = (width - textWidth) / 2, y = (height + fm.getAscent()) / 2 - 6;
        g2d.setColor(Color.BLACK);
        g2d.drawString(text.toUpperCase(), x, y);

        g2d.dispose();
        return image;
    }

    public static NativeImage bufferedImageToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                // NativeImage expects RGBA, so convert
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8)  & 0xFF;
                int b = (argb)       & 0xFF;
                int rgba = (r << 24) | (g << 16) | (b << 8) | a;
                nativeImage.setPixelRGBA(x, y, rgba);
            }
        }
        return nativeImage;
    }

    public static ResourceLocation uploadPlateTexture(BufferedImage bufferedImage, String key) {
        try {
            NativeImage nativeImage = bufferedImageToNativeImage(bufferedImage);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation rl = Minecraft.getInstance().getTextureManager().register(key, dynamicTexture);
            return rl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void renderBackLicensePlate(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation plateTexture, float yaw, Vec3 offset) {
        if (plateTexture == null) return;

        poseStack.pushPose();
        // Rotate with car's yaw
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.translate(offset.x, offset.y, offset.z);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(plateTexture));
        Matrix4f matrix = poseStack.last().pose();

        float minX = -0.32f, maxX = 0.32f, minY = 0f, maxY = 0.16f, z = 0f;
        float u0 = 0f, u1 = 1f, v0 = 0f, v1 = 1f;
        int color = 0xFFFFFFFF;
        int overlay = OverlayTexture.NO_OVERLAY;
        float nx = 0f, ny = 0f, nz = 1f;

        // Reverse winding order and flip horizontally by swapping u0 and u1
        vc.vertex(matrix, minX, maxY, z).color(color).uv(u1, v0).overlayCoords(overlay).uv2(packedLight).normal(nx, ny, nz).endVertex();
        vc.vertex(matrix, maxX, maxY, z).color(color).uv(u0, v0).overlayCoords(overlay).uv2(packedLight).normal(nx, ny, nz).endVertex();
        vc.vertex(matrix, maxX, minY, z).color(color).uv(u0, v1).overlayCoords(overlay).uv2(packedLight).normal(nx, ny, nz).endVertex();
        vc.vertex(matrix, minX, minY, z).color(color).uv(u1, v1).overlayCoords(overlay).uv2(packedLight).normal(nx, ny, nz).endVertex();

        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BaseCarEntity pEntity) {
        return pEntity.getFrameItem().getTextureLocation();
    }

    private BaseCarModel<BaseCarEntity> getModelForFrame(AbstractFrameItem frameItem) {
        ModelLayerLocation layer = (frameItem != null) ? frameItem.getModelLayerLocation() : ModModelLayers.BASE_CAR_LAYER;
        return modelCache.computeIfAbsent(layer, l -> new BaseCarModel<>(context.bakeLayer(l)));
    }

    @Override
    public void render(BaseCarEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        AbstractFrameItem frameItem = pEntity.getFrameItem();
        this.model = getModelForFrame(frameItem);
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
        String plateText = pEntity.getLicensePlateText();
        ResourceLocation plateTexture = plateTextureCache.get(plateText);
        if (plateTexture == null) {
            BufferedImage plateImage = generatePlate(plateText);
            plateTexture = uploadPlateTexture(plateImage, "plate_" + pEntity.getUUID() + "_" + plateText.toLowerCase());
            plateTextureCache.put(plateText, plateTexture);
        }
        renderBackLicensePlate(pPoseStack, pBuffer, pPackedLight, plateTexture, pEntityYaw, pEntity.getBackLicensePlateOffset());
    }
}
