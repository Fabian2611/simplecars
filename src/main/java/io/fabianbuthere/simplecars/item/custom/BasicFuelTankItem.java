package io.fabianbuthere.simplecars.item.custom;

public class BasicFuelTankItem extends AbstractFuelTankItem {
    public BasicFuelTankItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public double getBaseFuelCapacity() {
        return 12000.0; // 12 buckets
    }

    @Override
    public double getFuelConsumptionModifier() {
        return 1.0; // normal consumption
    }

    @Override
    public double getWeight() {
        return 45.0;
    }
}

