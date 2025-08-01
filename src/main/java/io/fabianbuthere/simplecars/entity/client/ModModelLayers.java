package io.fabianbuthere.simplecars.entity.client;

import io.fabianbuthere.simplecars.SimplecarsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class ModModelLayers {
    public static final ModelLayerLocation BASE_CAR_LAYER = new ModelLayerLocation(
            new ResourceLocation(SimplecarsMod.MOD_ID, "base_car_layer"), "main");

}
