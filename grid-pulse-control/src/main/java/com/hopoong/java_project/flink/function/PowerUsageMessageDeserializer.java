package com.hopoong.java_project.flink.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.java_project.kafka.message.PowerUsageMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * JSON 문자열을 PowerUsageMessage로 변환
 */
@Slf4j
public class PowerUsageMessageDeserializer implements MapFunction<String, PowerUsageMessage> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PowerUsageMessage map(String jsonString) throws Exception {
        try {
            return objectMapper.readValue(jsonString, PowerUsageMessage.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PowerUsageMessage: {}", jsonString, e);
            throw e;
        }
    }
}

