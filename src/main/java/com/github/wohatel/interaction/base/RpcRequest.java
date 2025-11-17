package com.github.wohatel.interaction.base;


import com.github.wohatel.util.RandomUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * This class represents a RPC (Remote Procedure Call) request that extends RpcRelay.
 * It includes annotations for Lombok to generate getters, setters, toString, equals, and hashCode methods.
 * The @Accessors(chain = true) annotation allows for method chaining.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class RpcRequest extends RpcRelay {
    private String requestId; // Unique identifier for the request
    private boolean needReaction; // 请求到达服务端后,要求服务端给予响应
    private String command; // 命令
    private String website; //  站点
    private String version; //  版本信息

    public RpcRequest() {
        this.requestId = RandomUtil.randomUUID();
    }

    public static RpcRequest withBody(String body) {
        RpcRequest request = new RpcRequest();
        request.setBody(body);
        return request;
    }

    public RpcReaction toReaction() {
        return RpcReaction.fromRequest(this);
    }
}
