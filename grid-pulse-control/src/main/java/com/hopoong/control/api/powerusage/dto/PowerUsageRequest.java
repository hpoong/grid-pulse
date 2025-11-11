package com.hopoong.control.api.powerusage.dto;

import com.hopoong.control.kafka.message.PowerUsageMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PowerUsageRequest {
    private String meterId;
    private PowerUsageMessage event;
}

