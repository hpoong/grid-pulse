package com.hopoong.java_project.api;

import com.hopoong.java_project.response.CommonResponseCodeEnum;
import com.hopoong.java_project.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public SuccessResponse test() {
        return new SuccessResponse(CommonResponseCodeEnum.SERVER, "tests");
    }
}
