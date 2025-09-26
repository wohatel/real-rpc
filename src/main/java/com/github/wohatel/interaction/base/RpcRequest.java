package com.github.wohatel.interaction.base;


import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RpcRequest extends RpcRelay {
    private String requestId = NanoIdUtils.randomNanoId();
    private boolean needResponse; // 请求到达服务端后,要求服务端给予响应
    private String command; // 命令
    private String website; //  站点
    private String version; //  版本信息

    /**
     * 构建一个压缩的请求
     */
    public static RpcRequest withBody(String body) {
        RpcRequest request = new RpcRequest();
        request.setBody(body);
        return request;
    }

    /**
     * 构建响应
     *
     * @return RpcResponse
     */
    public RpcResponse toResponse() {
        return RpcResponse.fromRequest(this);
    }
}
