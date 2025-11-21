package com.hopoong.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전력 사용량 집계 결과 모델
 * 윈도우별 집계된 데이터를 담는 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerUsageAggregation {

    private String siteId;          // 현장 ID
    private String meterId;         // 계량기 ID (siteId별 집계 시 null 가능)
    private long windowStart;        // 윈도우 시작 시간 (epoch ms)
    private long windowEnd;          // 윈도우 종료 시간 (epoch ms)
    private int windowSizeMinutes;    // 윈도우 크기 (1, 5, 15)
    private long count;              // 데이터 개수
    private double avgUsageKw;       // 평균 전력 사용량 (kW)
    private double maxUsageKw;       // 최대 전력 사용량 (kW)
    private double minUsageKw;       // 최소 전력 사용량 (kW)
    private double sumUsageKw;       // 합계 전력 사용량 (kW)
    private Double avgVoltage;       // 평균 전압
    private Double avgCurrent;       // 평균 전류
    private Double avgPf;            // 평균 역률

    /**
     * 집계 중간 결과를 저장하는 누적기 (Accumulator)
     * Flink의 AggregateFunction에서 사용됩니다.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Accumulator {
        private String siteId;
        private String meterId;
        private long count = 0;
        private double sumUsageKw = 0.0;
        private double minUsageKw = Double.MAX_VALUE;
        private double maxUsageKw = Double.MIN_VALUE;
        private double sumVoltage = 0.0;
        private long voltageCount = 0;
        private double sumCurrent = 0.0;
        private long currentCount = 0;
        private double sumPf = 0.0;
        private long pfCount = 0;
    }
}

