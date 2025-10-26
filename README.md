# grid-pulse

실시간 전력 사용량 모니터링 및 이상 탐지 플랫폼.

Kafka → Flink → Elasticsearch 파이프라인으로 구성되며, Flink가 단일 이벤트 스트림(`PowerUsageEvent`)을 기반으로 집계 및 이상 탐지를 수행합니다.

---

## 1. 프로젝트 개요

### 목적

- 전력 사용량(`usageKw`)의 급등, 고사용, 침묵 등 이상 패턴을 실시간 감지
- Flink 스트리밍으로 이벤트 집계 및 룰 기반/통계 기반 이상 탐지
- Elasticsearch에 집계 및 알람 데이터 저장
- Kibana 대시보드로 실시간 모니터링

---

## 2. 아키텍처

```
Producer → Kafka (power-usage-raw)
              ↓
         Flink Job
       ├─ Aggregation (1m/5m)
       ├─ Derived Metrics (Δ%, MA, σ)
       ├─ Rule-based Detection
       │    ├─ Spike / Sustain / Silence / Z-score
       ├─ Side Outputs (alerts / ops / audit)
       ↓
  Elasticsearch (7.10.2)
       ├─ power-usage-raw-YYYY.MM.DD
       ├─ power-usage-agg-YYYY.MM.DD
       └─ power-alerts-YYYY.MM.DD
              ↓
          Kibana Dashboard
```

---

## 3. 데이터 스키마

**PowerUsageEvent**

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| meterId | string | 계량기 ID |
| siteId | string | 현장/라인/건물 ID |
| timestamp | long | 이벤트 발생 시각(epoch ms) |
| usageKw | float | 순간 유효전력(kW) |
| voltage | float | 전압(V) (옵션) |
| current | float | 전류(A) (옵션) |
| pf | float | 역률 (옵션) |

---

## 4. 주요 기능

### Flink 스트리밍 처리

- **이벤트 타임 + 워터마크**
    - 지연 허용: 120초 (최대 10분 예외 버킷)
- **윈도우 집계**
    - 15초 슬라이딩 / 1분 슬라이딩 / 5분 텀블링
- **이상 탐지**
    - 급등(Δ%) 탐지
    - 지속 고사용률
    - 침묵(센서 정지)
    - 통계 기반(z-score / IQR)
- **Side Outputs**
    - `alerts` – 이상 탐지 이벤트
    - `ops` – 데이터 품질/결측
    - `audit` – 윈도우 통계 스냅샷

---

## 5. Elasticsearch 인덱스 설계

| 인덱스 | 내용 | 보존기간 |
| --- | --- | --- |
| power-usage-raw-YYYY.MM.DD | 원시 이벤트 | 7~14일 |
| power-usage-agg-YYYY.MM.DD | 1m/5m 집계 결과 | 30~90일 |
| power-alerts-YYYY.MM.DD | 이상 탐지 알람 | 30~90일 |

매핑 기본:

```json
{
  "mappings": {
    "properties": {
      "timestamp": { "type": "date" },
      "meterId": { "type": "keyword" },
      "siteId": { "type": "keyword" },
      "usageKw": { "type": "scaled_float", "scaling_factor": 100 },
      "alertType": { "type": "keyword" },
      "alertLevel": { "type": "keyword" },
      "explain": { "type": "text" }
    }
  }
}
```

---

## 6. 로컬 실행 환경 (Docker Compose)

### 구성 서비스

| 서비스 | 버전 | 포트 |
| --- | --- | --- |
| Kafka | 7.8.0 | 9092 |
| Zookeeper | 7.8.0 | 2181 |
| Flink JobManager | 1.20.2 | 8082 |
| Flink TaskManager | 1.20.2 | - |
| Elasticsearch | 7.10.2 | 9200 |
| Kibana | 7.10.2 | 5601 |

### 실행

```bash
docker-compose up -d
```

Flink UI 접속: [http://localhost:8082](http://localhost:8082/)

Kibana 접속: [http://localhost:5601](http://localhost:5601/)