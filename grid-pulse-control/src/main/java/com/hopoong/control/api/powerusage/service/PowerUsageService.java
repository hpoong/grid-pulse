package com.hopoong.control.api.powerusage.service;

import com.hopoong.control.kafka.message.PowerUsageMessage;
import com.hopoong.control.common.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PowerUsageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPowerUsageEvent(String meterId, PowerUsageMessage event) {
        kafkaTemplate.send(KafkaTopicManager.POWER_USAGE_METRICS_TOPIC, meterId, event);
    }
}

