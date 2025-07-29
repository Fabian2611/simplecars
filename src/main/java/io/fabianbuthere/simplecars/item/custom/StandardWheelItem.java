package io.fabianbuthere.simplecars.item.custom;

public class StandardWheelItem extends AbstractWheelItem {
    public StandardWheelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public double getBaseGrip() {
        return 0.85;
    }

    @Override
    public double getBaseTurningCoefficient() {
        return 0.95;
    }

    @Override
    public double getBaseDurability() {
        return 24000.0; // 20 minutes at 20 tps
    }

    @Override
    public double getMaxSpeed() {
        return 18.0 / 20.0; // 0.9 blocks/tick
    }

    @Override
    public double getWeight() {
        return 18.0;
    }
}

