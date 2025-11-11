package com.hopoong.control.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonResponseCodeEnum {

    // ************ T1 : **


    // ************ T9 : Server
    SERVER("T9", "C01");

    private final String type;
    private final String code;
}
