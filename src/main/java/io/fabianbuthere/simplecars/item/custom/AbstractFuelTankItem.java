package io.fabianbuthere.simplecars.item.custom;

public abstract class AbstractFuelTankItem extends AbstractCarPartItem {
    public AbstractFuelTankItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public CarPartType getPartType() {
        return CarPartType.FUEL_TANK;
    }

    /// Fuel capacity in millibuckets
    public abstract double getBaseFuelCapacity();
    /// Fuel consumption modifier, where 1.0 is normal consumption, 0.5 is half consumption, etc.
    public abstract double getFuelConsumptionModifier();
}
