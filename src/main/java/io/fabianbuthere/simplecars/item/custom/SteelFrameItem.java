package io.fabianbuthere.simplecars.item.custom;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.client.ModModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class SteelFrameItem extends AbstractFrameItem {
    public SteelFrameItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public double getBaseDrag() {
        return 0.09;
    }

    @Override
    public int getRequiredWheels() {
        return 4;
    }

    @Override
    public double getWeight() {
        return 220.0;
    }

    @Override
    public ModelLayerLocation getModelLayerLocation() {
        return ModModelLayers.BASE_CAR_LAYER;
    }

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getTextureLocation() {
        return new ResourceLocation(SimplecarsMod.MOD_ID, "textures/entity/empty.png");
    }

    @Override
    public Vec3 getBackLicensePlateOffset() {
        return new Vec3(0, 0.5, -2.1);
    }
}
