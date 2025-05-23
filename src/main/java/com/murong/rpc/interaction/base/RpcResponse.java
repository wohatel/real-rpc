package com.murong.rpc.interaction.base;

import lombok.Data;

@Data
public class RpcResponse {
    private String requestId;
    private boolean success = true;
    private String msg; // 异常消息
    private int code; // code
    private String body;
}
