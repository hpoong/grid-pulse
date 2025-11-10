package com.hopoong.java_project.flink.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 인덱스 템플릿 생성 유틸리티
 * power-usage-raw-* 인덱스 템플릿 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexTemplateCreator {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 원시 데이터 인덱스 템플릿 생성
     */
    public void createRawIndexTemplate() {
        try {
            String templateName = "power-usage-raw-template";
            
            // 템플릿이 이미 존재하는지 확인
            boolean exists = elasticsearchClient.indices().existsIndexTemplate(
                    e -> e.name(templateName)
            ).value();

            if (exists) {
                log.info("Index template '{}' already exists", templateName);
                return;
            }

            // 인덱스 템플릿 생성
            PutIndexTemplateRequest request = PutIndexTemplateRequest.of(b -> b
                    .name(templateName)
                    .indexPatterns("power-usage-raw-*")
                    .template(t -> t
                            .mappings(m -> m
                                    .properties("meterId", p -> p.keyword(k -> k))
                                    .properties("siteId", p -> p.keyword(k -> k))
                                    .properties("timestamp", p -> p.date(d -> d))
                                    .properties("usageKw", p -> p.scaledFloat(s -> s.scalingFactor(100)))
                                    .properties("voltage", p -> p.scaledFloat(s -> s.scalingFactor(100)))
                                    .properties("current", p -> p.scaledFloat(s -> s.scalingFactor(100)))
                                    .properties("pf", p -> p.scaledFloat(s -> s.scalingFactor(1000)))
                            )
                            .settings(s -> s
                                    .numberOfShards("1")
                                    .numberOfReplicas("0")
                                    .refreshInterval("30s")
                            )
                    )
            );

            elasticsearchClient.indices().putIndexTemplate(request);
            log.info("Index template '{}' created successfully", templateName);

        } catch (Exception e) {
            log.error("Failed to create index template", e);
            throw new RuntimeException("Failed to create index template", e);
        }
    }

    /**
     * 테스트용 인덱스 생성 (템플릿 적용 확인용)
     */
    public void createTestIndex() {
        try {
            String indexName = "power-usage-raw-test";
            
            boolean exists = elasticsearchClient.indices().exists(
                    ExistsRequest.of(e -> e.index(indexName))
            ).value();

            if (exists) {
                log.info("Test index '{}' already exists", indexName);
                return;
            }

            CreateIndexRequest request = CreateIndexRequest.of(b -> b
                    .index(indexName)
            );

            elasticsearchClient.indices().create(request);
            log.info("Test index '{}' created successfully", indexName);

        } catch (Exception e) {
            log.error("Failed to create test index", e);
        }
    }
}

