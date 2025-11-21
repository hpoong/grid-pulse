package com.hopoong.aggregator.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hopoong.aggregator.model.PowerUsageMessage;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * Kafka에서 수신한 JSON 문자열을 PowerUsageMessage로 변환하는 Deserializer
 * 
 * Flink의 DeserializationSchema 인터페이스를 구현하여
 * Kafka Source에서 자동으로 사용됩니다.
 */
public class PowerUsageMessageDeserializer implements DeserializationSchema<PowerUsageMessage> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * JSON 바이트 배열을 PowerUsageMessage 객체로 변환
     */
    @Override
    public PowerUsageMessage deserialize(byte[] message) throws IOException {
        if (message == null) {
            return null;
        }
        return objectMapper.readValue(message, PowerUsageMessage.class);
    }

    /**
     * 이벤트가 마지막 이벤트인지 여부 (Kafka에서는 false)
     */
    @Override
    public boolean isEndOfStream(PowerUsageMessage nextElement) {
        return false;
    }

    /**
     * 반환 타입 정보 (Flink가 타입을 알 수 있도록)
     */
    @Override
    public TypeInformation<PowerUsageMessage> getProducedType() {
        return TypeInformation.of(PowerUsageMessage.class);
    }
}

