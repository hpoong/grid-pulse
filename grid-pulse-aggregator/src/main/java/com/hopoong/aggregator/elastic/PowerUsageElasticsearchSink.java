package com.hopoong.aggregator.elastic;

import com.hopoong.aggregator.model.PowerUsageAggregation;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.elasticsearch.client.RestClient;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Flink PowerUsageAggregation 스트림을 Elasticsearch에 적재하는 Sink.
 * 날짜별 인덱스 자동 생성: power-usage-agg-{windowSize}m-{YYYY.MM.DD}
 */
public class PowerUsageElasticsearchSink extends RichSinkFunction<PowerUsageAggregation> {

    private final String host;
    private final int port;
    private final String scheme;
    private final String indexPrefix; // "power-usage-agg"

    private transient RestClient restClient;
    private transient RestClientTransport transport;
    private transient ElasticsearchClient esClient;
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy.MM.dd")
                    .withZone(ZoneId.of("UTC")); // UTC 기준으로 날짜 생성

    public PowerUsageElasticsearchSink(String host, int port, String scheme, String indexPrefix) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
        this.indexPrefix = indexPrefix;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.restClient = ElasticsearchClientFactory.createRestClient(host, port, scheme);
        this.transport = ElasticsearchClientFactory.createTransport(restClient);
        this.esClient = ElasticsearchClientFactory.createElasticsearchClient(transport);
    }

    @Override
    public void invoke(PowerUsageAggregation value, Context context) throws Exception {
        // 날짜별 인덱스 이름 동적 생성: power-usage-agg-{windowSize}m-{YYYY.MM.DD}
        String indexName = buildIndexName(value);
        
        esClient.index(i -> i
                .index(indexName)
                .id(buildDocId(value))
                .document(value)
        );
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (transport != null) {
            transport.close();
            transport = null;
        }
        if (restClient != null) {
            restClient.close();
            restClient = null;
        }
    }

    /**
     * 날짜별 인덱스 이름 생성
     * 형식: power-usage-agg-{windowSize}m-{YYYY.MM.DD}
     * windowEnd 기준으로 날짜 결정 (윈도우 종료 시점의 날짜 사용)
     */
    private String buildIndexName(PowerUsageAggregation aggregation) {
        String dateStr = DATE_FORMATTER.format(Instant.ofEpochMilli(aggregation.getWindowEnd()));
        return indexPrefix + "-" + aggregation.getWindowSizeMinutes() + "m-" + dateStr;
    }

    /**
     * 문서 ID 생성
     * 형식: {siteId}-{windowStart}
     */
    private String buildDocId(PowerUsageAggregation aggregation) {
        return aggregation.getSiteId() + "-" + aggregation.getWindowStart();
    }
}