# Elasticsearch 인덱스 관리 가이드

## 인덱스 명명 규칙

날짜별 인덱스가 자동으로 생성됩니다:
- `power-usage-agg-1m-2024.01.15` (1분 집계)
- `power-usage-agg-5m-2024.01.15` (5분 집계)
- `power-usage-agg-15m-2024.01.15` (15분 집계)

## 주요 관리 포인트 및 고려사항

### 1. 인덱스 템플릿 설정 (Index Template)

날짜별 인덱스가 자동 생성되므로, 인덱스 템플릿을 설정하여 모든 인덱스에 일관된 매핑과 설정을 적용해야 합니다.

```bash
PUT _index_template/power-usage-agg-template
{
  "index_patterns": ["power-usage-agg-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1,
      "refresh_interval": "30s"
    },
    "mappings": {
      "properties": {
        "siteId": { "type": "keyword" },
        "meterId": { "type": "keyword" },
        "windowStart": { "type": "date" },
        "windowEnd": { "type": "date" },
        "windowSizeMinutes": { "type": "integer" },
        "count": { "type": "long" },
        "avgUsageKw": { "type": "double" },
        "maxUsageKw": { "type": "double" },
        "minUsageKw": { "type": "double" },
        "sumUsageKw": { "type": "double" },
        "avgVoltage": { "type": "double" },
        "avgCurrent": { "type": "double" },
        "avgPf": { "type": "double" }
      }
    }
  },
  "priority": 100
}
```

### 2. 인덱스 라이프사이클 관리 (ILM - Index Lifecycle Management)

오래된 데이터를 자동으로 관리하기 위해 ILM 정책을 설정하는 것을 권장합니다.

```bash
# ILM 정책 생성
PUT _ilm/policy/power-usage-agg-policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "forcemerge": {
            "max_num_segments": 1
          },
          "shrink": {
            "number_of_shards": 1
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "allocate": {
            "number_of_replicas": 0
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}

# 템플릿에 ILM 정책 적용
PUT _index_template/power-usage-agg-template
{
  "index_patterns": ["power-usage-agg-*"],
  "template": {
    "settings": {
      "index.lifecycle.name": "power-usage-agg-policy",
      "index.lifecycle.rollover_alias": "power-usage-agg"
    }
  }
}
```

**주의**: 날짜별 인덱스 패턴에서는 ILM rollover가 적합하지 않을 수 있습니다. 수동 삭제 스크립트 또는 cron 작업을 사용하는 것이 더 나을 수 있습니다.

### 3. 오래된 인덱스 삭제 정책

날짜별 인덱스는 자동으로 계속 생성되므로, 보관 기간을 초과한 인덱스를 주기적으로 삭제해야 합니다.

```bash
# 90일 이전 인덱스 삭제 예시
DELETE power-usage-agg-*-$(date -d '90 days ago' +%Y.%m.%d)

# 또는 30일 이상 된 인덱스만 유지
# 스크립트 예시:
for index in $(curl -s 'localhost:9200/_cat/indices/power-usage-agg-*?h=index'); do
  index_date=$(echo $index | grep -oP '\d{4}\.\d{2}\.\d{2}')
  days_old=$(( ($(date +%s) - $(date -d "$index_date" +%s)) / 86400 ))
  if [ $days_old -gt 90 ]; then
    curl -X DELETE "localhost:9200/$index"
  fi
done
```

### 4. 시간대 관리

현재 코드는 **UTC 기준**으로 날짜를 생성합니다 (`ZoneId.of("UTC")`).

**고려사항**:
- 한국 시간(KST, UTC+9) 기준으로 하고 싶다면 `ZoneId.of("Asia/Seoul")`로 변경
- 일광절약시간(DST) 고려: UTC 사용이 더 안전
- 윈도우 종료 시점(`windowEnd`) 기준으로 날짜 결정: 자정 경계 처리 시 주의 필요

### 5. 성능 최적화

#### 인덱싱 성능
- **Bulk API 사용**: 현재는 단건 인덱싱만 사용 중. 대량 데이터 처리 시 Flink의 `BufferedSinkFunction` 구현 고려
- **Refresh Interval 조정**: 기본값은 1초. 30초~1분으로 늘리면 인덱싱 성능 향상

#### 검색 성능
- **Alias 사용**: 날짜별 인덱스 여러 개를 하나의 alias로 묶어 쿼리 단순화
```bash
# 모든 1분 집계 인덱스를 하나의 alias로
POST _aliases
{
  "actions": [
    { "add": { "index": "power-usage-agg-1m-*", "alias": "power-usage-agg-1m" } }
  ]
}
```

### 6. 모니터링 및 알림

#### 인덱스 상태 모니터링
- 일일 인덱스 생성 확인: 예상치 못한 인덱스 생성 방지
- 디스크 사용량 모니터링: 인덱스 증가로 인한 클러스터 디스크 부족 감지
- 문서 수 모니터링: 집계 로직 오류 감지

#### 주요 메트릭
```bash
# 인덱스 크기 확인
GET _cat/indices/power-usage-agg-*?v&h=index,store.size,docs.count

# 인덱스 상태 확인
GET _cat/indices/power-usage-agg-*?v&s=index
```

### 7. 데이터 일관성

#### 중복 방지
- 문서 ID 형식: `{siteId}-{windowStart}` 
- 동일한 윈도우 집계는 동일한 문서 ID를 가지므로 자동으로 덮어쓰기됨
- **주의**: Flink 재시작 시 같은 윈도우에 대해 여러 번 인덱싱될 수 있음

#### 데이터 검증
- 집계 값의 합리성 확인 (음수, 과도한 값 등)
- 윈도우 시간 범위 검증
- 누락된 siteId 감지

### 8. 장애 복구 시나리오

#### Flink 재시작 시
- Checkpoint에서 복구하면 동일한 윈도우 데이터가 다시 인덱싱될 수 있음
- 문서 ID가 동일하므로 최신 값으로 덮어쓰기됨 (idempotency 보장)

#### Elasticsearch 장애 시
- Flink는 재시도 메커니즘 필요
- 현재 코드에는 재시도 로직이 없으므로 `RetryPolicy` 추가 고려

### 9. 보안 및 접근 제어

- 인덱스 레벨 권한 설정 (Role-Based Access Control)
- 네트워크 접근 제어 (Firewall 규칙)
- 인증 정보 관리 (환경 변수 또는 설정 파일)

### 10. 데이터 백업 전략

- **스냅샷 정책**: 주기적으로 인덱스 스냅샷 생성
- **중요 데이터 식별**: 1분 집계 vs 15분 집계 보관 기간 차별화
- **복원 테스트**: 정기적인 백업 복원 테스트 수행

## 추천 운영 프로세스

1. **일일 모니터링**: 인덱스 생성 상태, 문서 수, 디스크 사용량
2. **주간 리뷰**: 오래된 인덱스 삭제, ILM 정책 조정
3. **월간 점검**: 인덱스 템플릿 업데이트, 백업 정책 검토
4. **분기별 점검**: 보관 기간 정책 재검토, 클러스터 용량 계획

## 참고 사항

- 인덱스 이름에 점(.)이 포함되어 있어 패턴 매칭 시 이스케이프 필요
- 날짜 형식(`yyyy.MM.dd`)은 Elasticsearch 날짜 패턴과 호환
- 인덱스당 샤드 수는 데이터 양에 따라 조정 필요 (하루 1GB 미만이면 샤드 1개 권장)

