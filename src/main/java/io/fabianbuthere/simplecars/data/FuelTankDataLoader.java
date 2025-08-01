package io.fabianbuthere.simplecars.data;

import java.util.Map;
import java.util.HashMap;
import com.google.gson.*;

public class FuelTankDataLoader {
    private static final Map<String, FuelTankData> FUEL_TANKS = new HashMap<>();

    public static void loadFromJson(String json) {
        FUEL_TANKS.clear();
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            float baseFuelCapacity = obj.get("baseFuelCapacity").getAsFloat();
            float fuelConsumptionModifier = obj.get("fuelConsumptionModifier").getAsFloat();
            float weight = obj.get("weight").getAsFloat();
            FUEL_TANKS.put(id, new FuelTankData(id, baseFuelCapacity, fuelConsumptionModifier, weight));
        }
    }

    public static FuelTankData getFuelTank(String id) {
        return FUEL_TANKS.get(id);
    }

    public static class FuelTankData {
        public final String id;
        public final float baseFuelCapacity;
        public final float fuelConsumptionModifier;
        public final float weight;

        public FuelTankData(String id, float baseFuelCapacity, float fuelConsumptionModifier, float weight) {
            this.id = id;
            this.baseFuelCapacity = baseFuelCapacity;
            this.fuelConsumptionModifier = fuelConsumptionModifier;
            this.weight = weight;
        }
    }
}
