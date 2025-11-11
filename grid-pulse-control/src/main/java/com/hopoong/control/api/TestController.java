package com.hopoong.control.api;

import com.hopoong.control.response.CommonResponseCodeEnum;
import com.hopoong.control.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public SuccessResponse test() {
        return new SuccessResponse(CommonResponseCodeEnum.SERVER, "tests");
    }
}
