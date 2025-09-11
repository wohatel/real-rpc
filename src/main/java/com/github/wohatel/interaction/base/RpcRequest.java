package com.github.wohatel.interaction.base;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcRequest extends AbstractCompressAble {
    private String requestId = UUID.randomUUID().toString();
    private String requestType; // 请求类型
    private String command; // 请求命令
    private String origin; // 请求来源
    private String website; //  请求站点                   rpc的服务端如果匹配到该站点,则处理此次请求
    private String header;  //  请求头部信息              rpc服务端可解析该请求的header
    private String version; //  请求版本信息              rpc服务端可解析该请求的version
    private String body;    //  请求body体
    private boolean needResponse; // 请求到达服务端后,要求服务端给予响应

    /**
     * 构建一个压缩的请求
     */
    public static RpcRequest compressRequest() {
        RpcRequest request = new RpcRequest();
        request.setNeedCompress(true);
        return request;
    }


    /**
     * 构建一个压缩的响应
     */
    public RpcResponse toResponse() {
        return this.toResponse(false);
    }

    /**
     * 构建响应
     *
     * @param needCompress 是否压缩
     * @return RpcResponse
     */
    public RpcResponse toResponse(boolean needCompress) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(this.requestId);
        response.setNeedCompress(needCompress);
        return response;
    }
}
