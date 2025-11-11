package com.hopoong.control.batch.simulator;

import com.hopoong.control.api.powerusage.dto.PowerUsageRequest;
import com.hopoong.control.kafka.message.PowerUsageMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class PowerUsageGeneratorBatch {

    private final RestTemplate restTemplate;

    @Value("${server.port:2343}")
    private int serverPort;

    private final Random random = new Random();

    // 간단하게 몇 개의 meter/site를 고정으로 돌리는 예시
    private final String[] sites = {"SITE-A", "SITE-B"};
    private final String[] meters = {"MTR-001", "MTR-002", "MTR-003"};


    @Scheduled(fixedRate = 60000)
    public void sendDummyEvents() {
        // site/meter 하나 랜덤 선택
        String siteId = sites[random.nextInt(sites.length)];
        String meterId = meters[random.nextInt(meters.length)];

        long now = Instant.now().toEpochMilli();
        LocalDateTime localTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault());
        int hour = localTime.getHour();
        int dayOfWeek = localTime.getDayOfWeek().getValue(); // 1=월요일, 7=일요일

        // 시간대별 전력 사용 패턴 (실제 전력 사용 패턴 반영)
        // 일반적인 산업/상업 시설 기준: 주간 높음, 야간 낮음
        double timeMultiplier = getTimeMultiplier(hour, dayOfWeek);
        
        // 기본 전력 사용량 범위 (일반적인 중소규모 시설 기준: 50~200kW)
        double baseMinUsage = 50.0;
        double baseMaxUsage = 200.0;
        double baseUsage = baseMinUsage + (baseMaxUsage - baseMinUsage) * random.nextDouble();
        
        // 시간대별 조정
        double adjustedUsage = baseUsage * timeMultiplier;
        
        // 자연스러운 변동성 추가 (±8%)
        double variation = 1.0 + (random.nextGaussian() * 0.08);
        double usageKw = adjustedUsage * variation;
        
        // 음수 방지
        usageKw = Math.max(5.0, usageKw);
        
        // 가끔 스파이크 발생 (5% 확률) - 이상 탐지 테스트용
        if (random.nextDouble() < 0.05) {
            usageKw = baseMaxUsage * (2.0 + random.nextDouble() * 1.5); // 2~3.5배 스파이크
        }

        // 전압: 220V 기준 ±2.5% (실제 전압 변동 범위, 한국 표준: 220V ±5%)
        // 정상 범위: 209V ~ 231V, 실제 운영 범위는 더 좁음
        Double voltage = 220.0 + (random.nextGaussian() * 2.5);
        voltage = Math.max(214.0, Math.min(226.0, voltage)); // 214V ~ 226V 범위 제한

        // 역률: 0.88 ~ 0.95 (일반적인 산업용 역률 범위)
        // 유도 부하가 많은 경우 낮고, 정전류 부하가 많으면 높음
        Double pf = 0.88 + random.nextDouble() * 0.07;
        pf = Math.max(0.88, Math.min(0.95, pf));

        // 전류 계산: P = V * I * PF → I = P / (V * PF)
        // 실제 전류는 유효전력과 무효전력을 모두 고려하지만, 여기서는 유효전력만 사용
        Double current = (usageKw * 1000) / (voltage * pf); // kW를 W로 변환 후 계산
        // 전류는 일반적으로 0 이상이어야 함
        current = Math.max(0.0, current);

        PowerUsageMessage event = PowerUsageMessage.builder()
                .meterId(meterId)
                .siteId(siteId)
                .timestamp(now)
                .usageKw(usageKw)
                .voltage(voltage)
                .current(current)
                .pf(pf)
                .build();

        // API 호출을 통해 Kafka로 전송
        PowerUsageRequest request = new PowerUsageRequest();
        request.setMeterId(meterId);
        request.setEvent(event);

        try {
            String url = "http://localhost:" + serverPort + "/api/power-usage/send";
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to send power usage event via API: meterId={}", meterId, e);
        }
    }

    /**
     * 시간대별 전력 사용 패턴 반환
     * 실제 전력 사용은 시간대와 요일에 따라 다르게 나타남
     * 일반적인 산업/상업 시설 기준 패턴
     */
    private double getTimeMultiplier(int hour, int dayOfWeek) {
        // 주말 처리 (토요일, 일요일)
        boolean isWeekend = (dayOfWeek == 6 || dayOfWeek == 7);
        
        if (isWeekend) {
            // 주말: 전반적으로 낮은 사용량
            if (hour >= 9 && hour < 18) return 0.4;  // 주말 주간
            else return 0.2;                          // 주말 야간
        }
        
        // 평일 패턴
        if (hour >= 8 && hour < 18) {
            // 주간 근무 시간: 정상 운영
            return 1.0;
        } else if (hour >= 18 && hour < 22) {
            // 저녁 시간: 부분 운영 또는 야근
            return 0.5;
        } else if (hour >= 6 && hour < 8) {
            // 출근 준비 시간: 점진적 증가
            return 0.6;
        } else {
            // 심야 시간: 최소 운영 (보안, 냉난방 등)
            return 0.25;
        }
    }

}