package com.github.wohatel.interaction.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcResponse extends RpcAbstractCompressAble {
    private String responseId;
    private String origRequestId; // 原始请求id
    private boolean success = true;
    private String msg; // 异常消息
    private int code; // code
    private String body;
}
