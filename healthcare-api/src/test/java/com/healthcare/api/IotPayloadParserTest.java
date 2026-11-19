package com.healthcare.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.api.iot.IotPayloadParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IotPayloadParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesAlinkPropertyPost() throws Exception {
        JsonNode node = mapper.readTree("""
                {"id":"1","version":"1.0","method":"thing.event.property.post",
                 "params":{"steps":4321,"waterMl":1500,"sleepMinutes":450,
                           "weightKg":61.5,"battery":88}}
                """);
        IotPayloadParser.HealthSample sample = IotPayloadParser.parse(node);
        assertThat(sample.steps()).isEqualTo(4321);
        assertThat(sample.waterMl()).isEqualTo(1500);
        assertThat(sample.sleepMinutes()).isEqualTo(450);
        assertThat(sample.weightKg()).isEqualTo(61.5);
        assertThat(sample.battery()).isEqualTo(88);
    }

    @Test
    void supportsAliasesAndPlainPayload() throws Exception {
        JsonNode aliased = mapper.readTree(
                "{\"params\":{\"stepCount\":100,\"water\":200,\"sleep\":300,"
                        + "\"weight\":55.5,\"batteryLevel\":70}}");
        IotPayloadParser.HealthSample sample = IotPayloadParser.parse(aliased);
        assertThat(sample.steps()).isEqualTo(100);
        assertThat(sample.waterMl()).isEqualTo(200);
        assertThat(sample.sleepMinutes()).isEqualTo(300);
        assertThat(sample.weightKg()).isEqualTo(55.5);
        assertThat(sample.battery()).isEqualTo(70);

        IotPayloadParser.HealthSample plain =
                IotPayloadParser.parse(mapper.readTree("{\"steps\":10}"));
        assertThat(plain.steps()).isEqualTo(10);
        assertThat(plain.waterMl()).isNull();
    }

    @Test
    void ignoresInvalidPayload() throws Exception {
        IotPayloadParser.HealthSample sample =
                IotPayloadParser.parse(mapper.readTree("{\"params\":\"nope\"}"));
        assertThat(sample.steps()).isNull();
        assertThat(IotPayloadParser.parse(null).steps()).isNull();
    }
}
