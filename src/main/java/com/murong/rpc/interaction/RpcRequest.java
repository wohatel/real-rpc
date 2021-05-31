package com.murong.rpc.interaction;

import java.util.UUID;

public class RpcRequest {
    private String requestId = UUID.randomUUID().toString();
    private String requestType; // 请求类型
    private String website; //  请求站点                   rpc的服务端如果匹配到该站点,则处理此次请求
    private String header;  //  请求头部信息              rpc服务端可解析该请求的header
    private String version; //  请求版本信息              rpc服务端可解析该请求的version
    private String body;    //  请求body体
    private boolean needResponse; // 请求到达服务端后,要求服务端给予响应


    public boolean isNeedResponse() {
        return needResponse;
    }

    public void setNeedResponse(boolean needResponse) {
        this.needResponse = needResponse;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public RpcResponse toResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(this.requestId);
        return response;
    }
}
