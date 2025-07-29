package io.fabianbuthere.simplecars.item.custom;

import net.minecraft.world.item.Item;

import java.util.Map;

public abstract class AbstractEngineItem extends AbstractCarPartItem {
    public AbstractEngineItem(Properties pProperties) {
        super(pProperties);
    }

    /// Maximum speed in blocks per tick
    public abstract double getBaseMaxSpeed();
    /// Acceleration in blocks per tick squared
    public abstract double getBaseAcceleration();
    /// Fuel consumption in fuel per tick
    public abstract double getBaseFuelConsumption();
    /// Map of fuel types to fuel efficiency coefficients this engine can use
    public abstract Map<String, Double> getAllowedFuelTypes();

    @Override
    public CarPartType getPartType() {
        return CarPartType.ENGINE;
    }
}
