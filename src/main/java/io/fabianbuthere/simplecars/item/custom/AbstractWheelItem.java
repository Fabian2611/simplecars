package io.fabianbuthere.simplecars.item.custom;

public abstract class AbstractWheelItem extends AbstractCarPartItem {
    public AbstractWheelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public CarPartType getPartType() {
        return CarPartType.WHEEL;
    }

    /// Grip value, as a coefficient for traction
    public abstract double getBaseGrip();
    /// Coefficient to the steering attenuation, lower is better
    public abstract double getBaseTurningCoefficient();
    /// Durability in ticks travelled
    public abstract double getBaseDurability();
    /// Maximum speed in blocks per tick, wheels start to slip at this speed
    public abstract double getMaxSpeed();
}
