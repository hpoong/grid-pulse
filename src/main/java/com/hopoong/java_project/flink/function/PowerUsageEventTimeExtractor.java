package com.hopoong.java_project.flink.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.TimestampAssigner;

import java.io.IOException;

/**
 * PowerUsageMessage의 timestamp 필드에서 이벤트 타임을 추출
 */
public class PowerUsageEventTimeExtractor implements TimestampAssigner<String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public long extractTimestamp(String jsonString, long recordTimestamp) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            if (jsonNode.has("timestamp")) {
                return jsonNode.get("timestamp").asLong();
            }
        } catch (IOException e) {
            // 파싱 실패 시 recordTimestamp 반환
        }
        return recordTimestamp;
    }
}

