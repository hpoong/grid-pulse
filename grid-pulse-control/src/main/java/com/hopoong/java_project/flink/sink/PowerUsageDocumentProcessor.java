package com.hopoong.java_project.flink.sink;

import com.hopoong.java_project.kafka.message.PowerUsageMessage;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.util.HashMap;
import java.util.Map;

/**
 * PowerUsageMessage를 Elasticsearch 문서로 변환
 */
public class PowerUsageDocumentProcessor implements ElasticsearchSinkFunction<PowerUsageMessage> {

    @Override
    public void process(PowerUsageMessage message, RuntimeContext ctx, RequestIndexer indexer) {
        // 인덱스 이름 생성 (날짜 기반)
        String indexName = ElasticsearchRawSink.getIndexName(message.getTimestamp());
        
        // 문서 ID 생성 (meterId + timestamp로 중복 방지)
        String documentId = message.getMeterId() + "_" + message.getTimestamp();
        
        // 문서 데이터 구성
        Map<String, Object> document = new HashMap<>();
        document.put("meterId", message.getMeterId());
        document.put("siteId", message.getSiteId());
        document.put("timestamp", message.getTimestamp());
        document.put("usageKw", message.getUsageKw());
        
        if (message.getVoltage() != null) {
            document.put("voltage", message.getVoltage());
        }
        if (message.getCurrent() != null) {
            document.put("current", message.getCurrent());
        }
        if (message.getPf() != null) {
            document.put("pf", message.getPf());
        }
        
        // IndexRequest 생성
        IndexRequest indexRequest = Requests.indexRequest()
                .index(indexName)
                .id(documentId)
                .source(document);
        
        indexer.add(indexRequest);
    }
}

