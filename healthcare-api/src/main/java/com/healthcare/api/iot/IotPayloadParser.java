package com.healthcare.api.iot;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 解析 Alink 物模型属性上报，兼容常见字段别名。
 *
 * <p>支持的字段：steps/stepCount、waterMl/water/drinkMl、
 * sleepMinutes/sleep、weightKg/weight、battery/batteryLevel。</p>
 */
public final class IotPayloadParser {

    private IotPayloadParser() {
    }

    public record HealthSample(
            Integer steps,
            Integer waterMl,
            Integer sleepMinutes,
            Double weightKg,
            Integer battery) {
    }

    public static HealthSample parse(JsonNode payload) {
        JsonNode params = payload != null && payload.has("params") ? payload.get("params") : payload;
        if (params == null || !params.isObject()) {
            return new HealthSample(null, null, null, null, null);
        }
        return new HealthSample(
                intValue(params, "steps", "stepCount"),
                intValue(params, "waterMl", "water", "drinkMl"),
                intValue(params, "sleepMinutes", "sleep"),
                doubleValue(params, "weightKg", "weight"),
                intValue(params, "battery", "batteryLevel"));
    }

    private static Integer intValue(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isNumber()) {
                return value.asInt();
            }
        }
        return null;
    }

    private static Double doubleValue(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
        }
        return null;
    }
}
