package com.hopoong.control.api.powerusage;

import com.hopoong.control.api.powerusage.dto.PowerUsageRequest;
import com.hopoong.control.api.powerusage.service.PowerUsageService;
import com.hopoong.control.response.CommonResponseCodeEnum;
import com.hopoong.control.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/power-usage")
@RequiredArgsConstructor
public class PowerUsageController {

    private final PowerUsageService powerUsageService;

    @PostMapping("/send")
    public SuccessResponse sendPowerUsage(@RequestBody PowerUsageRequest request) {
        powerUsageService.sendPowerUsageEvent(request.getMeterId(), request.getEvent());
        return new SuccessResponse(CommonResponseCodeEnum.SERVER, "Power usage event sent successfully");
    }



}

