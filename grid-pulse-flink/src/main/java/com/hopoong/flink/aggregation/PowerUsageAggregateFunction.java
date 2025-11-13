package com.hopoong.flink.aggregation;

import com.hopoong.flink.model.PowerUsageAggregation;
import com.hopoong.flink.model.PowerUsageMessage;
import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * 전력 사용량 데이터를 집계하는 함수
 * 
 * Flink의 AggregateFunction을 구현하여 윈도우 내의 데이터를 집계합니다.
 * 
 * 동작 방식:
 * 1. createAccumulator(): 초기 누적기 생성
 * 2. add(): 각 메시지가 들어올 때마다 누적기에 값 추가
 * 3. getResult(): 윈도우가 끝날 때 최종 결과 반환
 * 4. merge(): 병렬 처리 시 여러 누적기를 합칠 때 사용
 */
public class PowerUsageAggregateFunction implements AggregateFunction<
        PowerUsageMessage,                    // 입력 타입
        PowerUsageAggregation.Accumulator,    // 누적기 타입
        PowerUsageAggregation                 // 출력 타입
        > {

    private final int windowSizeMinutes;

    public PowerUsageAggregateFunction(int windowSizeMinutes) {
        this.windowSizeMinutes = windowSizeMinutes;
    }

    /**
     * 초기 누적기 생성
     * 윈도우가 시작될 때 호출됩니다.
     */
    @Override
    public PowerUsageAggregation.Accumulator createAccumulator() {
        return new PowerUsageAggregation.Accumulator();
    }

    /**
     * 각 메시지가 들어올 때마다 누적기에 값 추가
     * 윈도우 내의 모든 메시지에 대해 호출됩니다.
     */
    @Override
    public PowerUsageAggregation.Accumulator add(
            PowerUsageMessage value,
            PowerUsageAggregation.Accumulator accumulator) {

        // 첫 번째 메시지인 경우 초기화
        if (accumulator.getCount() == 0) {
            accumulator.setSiteId(value.getSiteId());
            accumulator.setMeterId(value.getMeterId());
            accumulator.setMinUsageKw(value.getUsageKw());
            accumulator.setMaxUsageKw(value.getUsageKw());
        }

        // 카운트 증가
        accumulator.setCount(accumulator.getCount() + 1);

        // 전력 사용량 집계
        accumulator.setSumUsageKw(accumulator.getSumUsageKw() + value.getUsageKw());
        accumulator.setMinUsageKw(Math.min(accumulator.getMinUsageKw(), value.getUsageKw()));
        accumulator.setMaxUsageKw(Math.max(accumulator.getMaxUsageKw(), value.getUsageKw()));

        // 전압, 전류, 역률 집계 (null 체크)
        if (value.getVoltage() != null) {
            accumulator.setSumVoltage(accumulator.getSumVoltage() + value.getVoltage());
            accumulator.setVoltageCount(accumulator.getVoltageCount() + 1);
        }
        if (value.getCurrent() != null) {
            accumulator.setSumCurrent(accumulator.getSumCurrent() + value.getCurrent());
            accumulator.setCurrentCount(accumulator.getCurrentCount() + 1);
        }
        if (value.getPf() != null) {
            accumulator.setSumPf(accumulator.getSumPf() + value.getPf());
            accumulator.setPfCount(accumulator.getPfCount() + 1);
        }

        return accumulator;
    }

    /**
     * 윈도우가 끝날 때 최종 결과 반환
     * 집계된 데이터를 PowerUsageAggregation 객체로 변환합니다.
     */
    @Override
    public PowerUsageAggregation getResult(PowerUsageAggregation.Accumulator accumulator) {
        return PowerUsageAggregation.builder()
                .siteId(accumulator.getSiteId())
                .meterId(accumulator.getMeterId())
                .windowSizeMinutes(windowSizeMinutes)
                .count(accumulator.getCount())
                .avgUsageKw(accumulator.getCount() > 0 
                        ? accumulator.getSumUsageKw() / accumulator.getCount() 
                        : 0.0)
                .maxUsageKw(accumulator.getMaxUsageKw())
                .minUsageKw(accumulator.getMinUsageKw())
                .sumUsageKw(accumulator.getSumUsageKw())
                .avgVoltage(accumulator.getVoltageCount() > 0 
                        ? accumulator.getSumVoltage() / accumulator.getVoltageCount() 
                        : null)
                .avgCurrent(accumulator.getCurrentCount() > 0 
                        ? accumulator.getSumCurrent() / accumulator.getCurrentCount() 
                        : null)
                .avgPf(accumulator.getPfCount() > 0 
                        ? accumulator.getSumPf() / accumulator.getPfCount() 
                        : null)
                .build();
    }

    /**
     * 병렬 처리 시 여러 누적기를 합칠 때 사용
     * (일반적으로 사용되지 않지만 구현 필요)
     */
    @Override
    public PowerUsageAggregation.Accumulator merge(
            PowerUsageAggregation.Accumulator a,
            PowerUsageAggregation.Accumulator b) {
        
        if (a.getCount() == 0) {
            return b;
        }
        if (b.getCount() == 0) {
            return a;
        }

        // 두 누적기 합치기
        a.setCount(a.getCount() + b.getCount());
        a.setSumUsageKw(a.getSumUsageKw() + b.getSumUsageKw());
        a.setMinUsageKw(Math.min(a.getMinUsageKw(), b.getMinUsageKw()));
        a.setMaxUsageKw(Math.max(a.getMaxUsageKw(), b.getMaxUsageKw()));

        // 전압, 전류, 역률 합치기
        a.setSumVoltage(a.getSumVoltage() + b.getSumVoltage());
        a.setVoltageCount(a.getVoltageCount() + b.getVoltageCount());
        a.setSumCurrent(a.getSumCurrent() + b.getSumCurrent());
        a.setCurrentCount(a.getCurrentCount() + b.getCurrentCount());
        a.setSumPf(a.getSumPf() + b.getSumPf());
        a.setPfCount(a.getPfCount() + b.getPfCount());

        return a;
    }
}

