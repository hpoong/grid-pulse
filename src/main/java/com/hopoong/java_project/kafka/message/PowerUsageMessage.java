package com.hopoong.java_project.kafka.message;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerUsageMessage {

    private String meterId;     // 계량기 ID
    private String siteId;      // 현장/라인/건물 ID
    private long timestamp;     // epoch ms
    private double usageKw;     // 순간 유효전력(kW) 또는 샘플링 평균
    private Double voltage;     // 선택
    private Double current;     // 선택
    private Double pf;          // 선택
}
