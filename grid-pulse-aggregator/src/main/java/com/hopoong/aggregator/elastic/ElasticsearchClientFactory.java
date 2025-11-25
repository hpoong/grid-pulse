package com.hopoong.aggregator.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

/**
 * Elasticsearch Java API 클라이언트 생성을 담당하는 팩토리.
 * <p>
 * RestClient → RestClientTransport → ElasticsearchClient 순으로 생성되며
 * 각 컴포넌트는 명시적으로 close 해주어야 하므로 호출 측에서 관리할 수 있도록
 * 개별 팩토리 메서드를 제공한다.
 */
public final class ElasticsearchClientFactory {

    private ElasticsearchClientFactory() {
    }

    public static RestClient createRestClient(String host, int port, String scheme) {
        return RestClient.builder(new HttpHost(host, port, scheme)).build();
    }

    public static RestClientTransport createTransport(RestClient restClient) {
        return new RestClientTransport(restClient, createJsonpMapper());
    }

    public static ElasticsearchClient createElasticsearchClient(RestClientTransport transport) {
        return new ElasticsearchClient(transport);
    }

    private static JsonpMapper createJsonpMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new JacksonJsonpMapper(objectMapper);
    }
}