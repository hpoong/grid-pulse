package com.hopoong.flink;

import com.hopoong.flink.aggregation.PowerUsageAggregateFunction;
import com.hopoong.flink.deserializer.PowerUsageMessageDeserializer;
import com.hopoong.flink.model.PowerUsageAggregation;
import com.hopoong.flink.model.PowerUsageMessage;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 전력 사용량 데이터를 실시간으로 집계하는 Flink 애플리케이션
 * 
 * 주요 기능:
 * 1. Kafka에서 전력 사용량 메시지 수신
 * 2. 1분/5분/15분 단위로 시간 윈도우 집계
 * 3. siteId별로 그룹화하여 집계
 * 4. 집계 결과 출력
 * 
 * Flink 개념 설명:
 * - DataStream: 데이터의 흐름을 나타내는 추상화
 * - Source: 데이터를 읽어오는 곳 (Kafka)
 * - KeyBy: 데이터를 그룹화 (같은 키끼리 묶음)
 * - Window: 시간 기반으로 데이터를 묶는 단위
 * - Aggregate: 윈도우 내의 데이터를 집계
 * - Sink: 결과를 출력하는 곳 (현재는 print)
 */
public class GridPulseFlinkApplication {

    // Kafka 설정
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka:29092";
    private static final String KAFKA_TOPIC = "power-usage-metrics";
    private static final String CONSUMER_GROUP_ID = "flink-power-usage-aggregator";

    // 날짜 포맷터 (로그 출력용)
    private static final DateTimeFormatter formatter = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    public static void main(String[] args) throws Exception {
        
        // ============================================
        // 1. Flink 실행 환경 생성
        // ============================================
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Checkpointing 활성화 (장애 복구를 위해)
        // 주기적으로 상태를 저장하여 장애 발생 시 복구 가능
        env.enableCheckpointing(60000); // 60초마다 체크포인트
        
        // ============================================
        // 2. Kafka Source 설정
        // ============================================
        KafkaSource<PowerUsageMessage> kafkaSource = KafkaSource.<PowerUsageMessage>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(KAFKA_TOPIC)
                .setGroupId(CONSUMER_GROUP_ID)
                .setStartingOffsets(OffsetsInitializer.latest()) // 최신 메시지부터 읽기
                .setValueOnlyDeserializer(new PowerUsageMessageDeserializer())
                .build();

        // ============================================
        // 3. Kafka에서 데이터 스트림 생성
        // ============================================
        // WatermarkStrategy: 이벤트 시간 기반 처리
        // - 이벤트 시간: 메시지의 timestamp 필드 사용
        // - Watermark: 지연된 데이터를 처리하기 위한 시간 기준
        DataStream<PowerUsageMessage> powerUsageStream = env
                .fromSource(
                        kafkaSource,
                        WatermarkStrategy.<PowerUsageMessage>forBoundedOutOfOrderness(
                                Duration.ofSeconds(10) // 10초 지연 허용
                        )
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp()),
                        "Kafka Source"
                );

        // ============================================
        // 4. 1분 단위 집계
        // ============================================
        processWindowAggregation(
                powerUsageStream,
                1,
                "1분 집계",
                env
        );

//        // ============================================
//        // 5. 5분 단위 집계
//        // ============================================
//        processWindowAggregation(
//                powerUsageStream,
//                5,
//                "5분 집계",
//                env
//        );
//
//        // ============================================
//        // 6. 15분 단위 집계
//        // ============================================
//        processWindowAggregation(
//                powerUsageStream,
//                15,
//                "15분 집계",
//                env
//        );

        // ============================================
        // 7. Job 실행
        // ============================================
        env.execute("Power Usage Aggregation Job v1");
    }

    /**
     * 윈도우 집계 처리 함수
     * 
     * @param stream 입력 데이터 스트림
     * @param windowSizeMinutes 윈도우 크기 (분)
     * @param jobName 작업 이름
     * @param env 실행 환경
     */
    private static void processWindowAggregation(
            DataStream<PowerUsageMessage> stream,
            int windowSizeMinutes,
            String jobName,
            StreamExecutionEnvironment env) {

        stream
                // KeyBy: siteId를 기준으로 데이터를 그룹화
                // 같은 siteId를 가진 데이터끼리 묶어서 집계
                .keyBy(PowerUsageMessage::getSiteId)
                
                // Window: 시간 기반 윈도우 생성
                // TumblingEventTimeWindows: 고정 크기 윈도우 (겹치지 않음)
                // 예: 1분 윈도우면 00:00-00:01, 00:01-00:02, ... 이런 식으로 구분
                .window(TumblingEventTimeWindows.of(java.time.Duration.ofMinutes(windowSizeMinutes)))
                
                // Aggregate: 윈도우 내의 데이터를 집계
                // AggregateFunction: 각 메시지가 들어올 때마다 누적기에 값 추가
                // ProcessWindowFunction: 윈도우가 끝날 때 윈도우 정보(시작/종료 시간) 추가
                .aggregate(
                        new PowerUsageAggregateFunction(windowSizeMinutes),
                        new ProcessWindowFunction<
                                PowerUsageAggregation,
                                PowerUsageAggregation,
                                String,
                                TimeWindow>() {
                            
                            @Override
                            public void process(
                                    String key, // siteId
                                    ProcessWindowFunction<
                                            PowerUsageAggregation,
                                            PowerUsageAggregation,
                                            String,
                                            TimeWindow>.Context context,
                                    Iterable<PowerUsageAggregation> elements,
                                    Collector<PowerUsageAggregation> out) throws Exception {
                                
                                // 윈도우 정보 추가
                                TimeWindow window = context.window();
                                PowerUsageAggregation aggregation = elements.iterator().next();
                                
                                aggregation.setWindowStart(window.getStart());
                                aggregation.setWindowEnd(window.getEnd());
                                
                                out.collect(aggregation);
                            }
                        })
                
                // 결과 출력
                .map(new MapFunction<PowerUsageAggregation, String>() {
                    @Override
                    public String map(PowerUsageAggregation agg) throws Exception {
                        return String.format(
                                "[%s] SiteId: %s, Window: %s ~ %s, Count: %d, " +
                                "Avg: %.2f kW, Max: %.2f kW, Min: %.2f kW, Sum: %.2f kW",
                                jobName,
                                agg.getSiteId(),
                                formatter.format(Instant.ofEpochMilli(agg.getWindowStart())),
                                formatter.format(Instant.ofEpochMilli(agg.getWindowEnd())),
                                agg.getCount(),
                                agg.getAvgUsageKw(),
                                agg.getMaxUsageKw(),
                                agg.getMinUsageKw(),
                                agg.getSumUsageKw()
                        );
                    }
                })
                .print(jobName + " 결과");
    }
}
