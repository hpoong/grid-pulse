package com.hopoong.control.kafka.consumer;

import com.hopoong.control.util.LoggerUtil;
import com.hopoong.control.kafka.message.PowerUsageMessage;
import com.hopoong.control.common.KafkaTopicManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PowerUsageMessageConsumer {

    @KafkaListener(
            topics = KafkaTopicManager.POWER_USAGE_METRICS_TOPIC,
            groupId = "power-usage-consumer-group"
    )
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload PowerUsageMessage message,
            Acknowledgment acknowledgment
    ) {

        String content = String.format(
                "key: %s, meterId: %s, siteId: %s, timestamp: %d, usageKw: %.2f, voltage: %.2f, current: %.2f, pf: %.2f",
                key,
                message.getMeterId(),
                message.getSiteId(),
                message.getTimestamp(),
                message.getUsageKw(),
                message.getVoltage(),
                message.getCurrent(),
                message.getPf()
        );

        LoggerUtil.block(log, "Power Usage Message Received", content);

        // 수동 커밋 모드이므로 acknowledgment 필요
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }
}
