package com.murong.rpc.interaction.base;


import lombok.Data;

import java.util.UUID;

@Data
public class RpcRequest {
    private boolean authen;
    private String requestId = UUID.randomUUID().toString();
    private String requestType; // 请求类型
    private String command; // 请求命令
    private String origin; // 请求来源
    private String website; //  请求站点                   rpc的服务端如果匹配到该站点,则处理此次请求
    private String header;  //  请求头部信息              rpc服务端可解析该请求的header
    private String version; //  请求版本信息              rpc服务端可解析该请求的version
    private String body;    //  请求body体
    private boolean needResponse; // 请求到达服务端后,要求服务端给予响应


    public RpcResponse toResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(this.requestId);
        return response;
    }
}
