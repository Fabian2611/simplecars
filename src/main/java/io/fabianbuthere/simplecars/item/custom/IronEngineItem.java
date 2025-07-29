package io.fabianbuthere.simplecars.item.custom;

import java.util.Map;

public class IronEngineItem extends AbstractEngineItem {
    public IronEngineItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public double getBaseMaxSpeed() {
        return 15.0 / 20.0;
    }

    @Override
    public double getBaseAcceleration() {
        return 0.8 / 20.0;
    }

    @Override
    public double getBaseFuelConsumption() {
        return 0.6 / 20.0;
    }

    @Override
    public double getWeight() {
        return 160.0;
    }

    @Override
    public Map<String, Double> getAllowedFuelTypes() {
        return Map.of("fluid.minecraft.lava", 0.6, "fluid.tfmg.diesel", 1.0, "fluid.tfmg.gasoline", 1.2);
    }
}
