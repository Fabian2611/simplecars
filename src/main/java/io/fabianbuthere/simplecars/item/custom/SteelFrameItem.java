package io.fabianbuthere.simplecars.item.custom;

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
}

