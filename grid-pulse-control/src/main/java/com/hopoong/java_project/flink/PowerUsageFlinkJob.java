package com.hopoong.java_project.flink;

import com.hopoong.java_project.common.KafkaTopicManager;
import com.hopoong.java_project.flink.function.PowerUsageEventTimeExtractor;
import com.hopoong.java_project.flink.function.PowerUsageMessageDeserializer;
import com.hopoong.java_project.flink.sink.ElasticsearchRawSink;
import com.hopoong.java_project.kafka.message.PowerUsageMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;

import java.time.Duration;
import java.util.Properties;

/**
 * Flink Job: Kafka에서 전력 사용량 이벤트를 읽어 Elasticsearch에 저장
 * 
 * 파이프라인:
 * Kafka (power-usage-metrics) → Flink (이벤트 타임/워터마크) → Elasticsearch (power-usage-raw-YYYY.MM.DD)
 */
@Slf4j
public class PowerUsageFlinkJob {

    private static final String JOB_NAME = "Power Usage Streaming Job";
    
    // 워터마크 설정
    private static final Duration WATERMARK_DELAY = Duration.ofSeconds(120); // 120초 지연 허용
    private static final Duration MAX_OUT_OF_ORDERNESS = Duration.ofMinutes(10); // 최대 10분 지연 예외 버킷

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 체크포인트 설정
        env.enableCheckpointing(60000); // 60초마다 체크포인트
        env.getCheckpointConfig().setCheckpointingMode(org.apache.flink.streaming.api.CheckpointingMode.EXACTLY_ONCE);
        
        // Kafka Source 설정
        KafkaSource<String> kafkaSource = createKafkaSource();
        
        // 데이터 스트림 생성
        DataStream<String> kafkaStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy
                        .<String>forBoundedOutOfOrderness(MAX_OUT_OF_ORDERNESS)
                        .withTimestampAssigner((element, recordTimestamp) -> {
                            PowerUsageEventTimeExtractor extractor = new PowerUsageEventTimeExtractor();
                            return extractor.extractTimestamp(element, recordTimestamp);
                        }),
                "Kafka Source"
        );
        
        // JSON 문자열을 PowerUsageMessage로 변환
        DataStream<PowerUsageMessage> powerUsageStream = kafkaStream
                .map(new PowerUsageMessageDeserializer())
                .name("Deserialize PowerUsageMessage");
        
        // Elasticsearch Sink 설정
        ElasticsearchSink<PowerUsageMessage> esSink = ElasticsearchRawSink.createSink();
        
        // Sink 연결 (deprecated API이지만 Flink 1.20.2에서 작동)
        powerUsageStream.addSink(esSink).name("Elasticsearch Raw Sink");
        
        // Job 실행
        env.execute(JOB_NAME);
    }

    /**
     * Kafka Source 생성
     */
    private static KafkaSource<String> createKafkaSource() {
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", "localhost:9092");
        kafkaProps.setProperty("group.id", "flink-power-usage-consumer");
        
        return KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics(KafkaTopicManager.POWER_USAGE_METRICS_TOPIC)
                .setGroupId("flink-power-usage-consumer")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperties(kafkaProps)
                .build();
    }
}

