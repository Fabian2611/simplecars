package io.fabianbuthere.simplecars.item.custom;

public abstract class AbstractFrameItem extends AbstractCarPartItem {
    public AbstractFrameItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public CarPartType getPartType() {
        return CarPartType.FRAME;
    }

    /// Drag coefficient, lower is better
    public abstract double getBaseDrag();
    /// Number of wheels this frame needs
    public abstract int getRequiredWheels();
}
