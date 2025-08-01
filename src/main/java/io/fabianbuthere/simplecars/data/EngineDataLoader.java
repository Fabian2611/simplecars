package io.fabianbuthere.simplecars.data;

import java.util.Map;
import java.util.HashMap;
import com.google.gson.*;
import java.util.Collections;

public class EngineDataLoader {
    private static final Map<String, EngineData> ENGINES = new HashMap<>();

    public static void loadFromJson(String json) {
        ENGINES.clear();
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            float baseMaxSpeed = obj.get("baseMaxSpeed").getAsFloat();
            float baseAcceleration = obj.get("baseAcceleration").getAsFloat();
            float baseFuelConsumption = obj.get("baseFuelConsumption").getAsFloat();
            float weight = obj.get("weight").getAsFloat();
            Map<String, Double> allowedFuelTypes = new HashMap<>();
            if (obj.has("allowedFuelTypes")) {
                JsonObject fuels = obj.getAsJsonObject("allowedFuelTypes");
                for (String key : fuels.keySet()) {
                    allowedFuelTypes.put(key, fuels.get(key).getAsDouble());
                }
            }
            ENGINES.put(id, new EngineData(id, baseMaxSpeed, baseAcceleration, baseFuelConsumption, weight, allowedFuelTypes));
        }
    }

    public static EngineData getEngine(String id) {
        return ENGINES.get(id);
    }

    public static class EngineData {
        public final String id;
        public final float baseMaxSpeed;
        public final float baseAcceleration;
        public final float baseFuelConsumption;
        public final float weight;
        public final Map<String, Double> allowedFuelTypes;

        public EngineData(String id, float baseMaxSpeed, float baseAcceleration, float baseFuelConsumption, float weight, Map<String, Double> allowedFuelTypes) {
            this.id = id;
            this.baseMaxSpeed = baseMaxSpeed;
            this.baseAcceleration = baseAcceleration;
            this.baseFuelConsumption = baseFuelConsumption;
            this.weight = weight;
            this.allowedFuelTypes = Collections.unmodifiableMap(allowedFuelTypes);
        }
    }
}
