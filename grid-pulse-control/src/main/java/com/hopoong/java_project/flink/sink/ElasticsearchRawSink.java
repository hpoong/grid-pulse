package com.hopoong.java_project.flink.sink;

import com.hopoong.java_project.kafka.message.PowerUsageMessage;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.http.HttpHost;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch 원시 데이터 저장을 위한 Sink 생성
 * 인덱스: power-usage-raw-YYYY.MM.DD
 */
public class ElasticsearchRawSink {

    private static final String INDEX_PREFIX = "power-usage-raw";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            .withZone(ZoneId.systemDefault());

    /**
     * Elasticsearch Sink 생성 (Flink 1.20.2 + Elasticsearch 7)
     */
    public static ElasticsearchSink<PowerUsageMessage> createSink() {
        List<HttpHost> httpHosts = new ArrayList<>();
        httpHosts.add(new HttpHost("localhost", 9200, "http"));

        ElasticsearchSink.Builder<PowerUsageMessage> builder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new PowerUsageDocumentProcessor()
        );

        // 배치 설정
        builder.setBulkFlushMaxActions(1000); // 1000개마다 flush
        builder.setBulkFlushInterval(5000L); // 5초마다 flush
        builder.setBulkFlushMaxSizeMb(5); // 5MB마다 flush

        return builder.build();
    }

    /**
     * 날짜 기반 인덱스 이름 생성
     */
    public static String getIndexName(long timestamp) {
        String dateStr = DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp));
        return INDEX_PREFIX + "-" + dateStr;
    }
}

