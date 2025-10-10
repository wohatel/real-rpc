package com.github.wohatel.interaction.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
public enum RpcCommandType {
    request(1), session(2), response(4),

    file(3),
    ;

    @Getter
    int code;

    public static RpcCommandType fromCode(int codeValue) {
        return Arrays.stream(RpcCommandType.values()).filter(t -> t.getCode() == codeValue).findFirst().orElse(null);
    }
}
