package com.github.wohatel.interaction.base;


import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.wohatel.tcp.RpcDataReceiver;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcRequest extends RpcAbstractCompressAble {
    private String origin = RpcDataReceiver.NODEID;
    private String requestId = NanoIdUtils.randomNanoId();
    private String requestType; // 请求类型
    private String command; // 请求命令
    private String website; //  请求站点                   rpc的服务端如果匹配到该站点,则处理此次请求
    private String header;  //  请求头部信息              rpc服务端可解析该请求的header
    private String version; //  请求版本信息              rpc服务端可解析该请求的version
    private String body;    //  请求body体
    private boolean needResponse; // 请求到达服务端后,要求服务端给予响应


    /**
     * 构建一个压缩的请求
     */
    public static RpcRequest withBody(String body) {
        RpcRequest request = new RpcRequest();
        request.setBody(body);
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
        response.setResponseId(this.requestId);
        response.setOrigRequestId(this.requestId);
        response.setNeedCompress(needCompress);
        return response;
    }
}
