package io.fabianbuthere.simplecars.item.custom;

public enum CarPartType {
    ENGINE("engine"),
    FRAME("frame"),
    WHEEL("wheel"),
    FUEL_TANK("fuel_tank");

    private final String name;

    CarPartType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
