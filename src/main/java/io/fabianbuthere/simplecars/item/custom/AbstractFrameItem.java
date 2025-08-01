package io.fabianbuthere.simplecars.item.custom;

import io.fabianbuthere.simplecars.entity.client.ModModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractFrameItem extends AbstractCarPartItem {
    public AbstractFrameItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public CarPartType getPartType() {
        return CarPartType.FRAME;
    }

    /// Returns the model layer location for this frame item.
    public abstract ModelLayerLocation getModelLayerLocation();
    /// Returns the texture location for this frame item.
    public abstract ResourceLocation getTextureLocation();

    /// Drag coefficient, lower is better
    public abstract double getBaseDrag();
    /// Number of wheels this frame needs
    public abstract int getRequiredWheels();
}
