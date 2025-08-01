package io.fabianbuthere.simplecars.data;

import java.util.Map;
import java.util.HashMap;
import com.google.gson.*;

public class WheelDataLoader {
    private static final Map<String, WheelData> WHEELS = new HashMap<>();

    public static void loadFromJson(String json) {
        WHEELS.clear();
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            float baseGrip = obj.get("baseGrip").getAsFloat();
            float baseTurningCoefficient = obj.get("baseTurningCoefficient").getAsFloat();
            float baseDurability = obj.get("baseDurability").getAsFloat();
            float maxSpeed = obj.get("maxSpeed").getAsFloat();
            float weight = obj.get("weight").getAsFloat();
            WHEELS.put(id, new WheelData(id, baseGrip, baseTurningCoefficient, baseDurability, maxSpeed, weight));
        }
    }

    public static WheelData getWheel(String id) {
        return WHEELS.get(id);
    }

    public static class WheelData {
        public final String id;
        public final float baseGrip;
        public final float baseTurningCoefficient;
        public final float baseDurability;
        public final float maxSpeed;
        public final float weight;

        public WheelData(String id, float baseGrip, float baseTurningCoefficient, float baseDurability, float maxSpeed, float weight) {
            this.id = id;
            this.baseGrip = baseGrip;
            this.baseTurningCoefficient = baseTurningCoefficient;
            this.baseDurability = baseDurability;
            this.maxSpeed = maxSpeed;
            this.weight = weight;
        }
    }
}
