package com.hopoong.java_project.api.powerusage.dto;

import com.hopoong.java_project.kafka.message.PowerUsageMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PowerUsageRequest {
    private String meterId;
    private PowerUsageMessage event;
}

