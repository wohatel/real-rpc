package com.murong.rpc.interaction.common;

import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import lombok.Getter;

/**
 * description
 *
 * @author yaochuang 2025/03/28 13:47
 */
public class RpcTransHandlerConfigManager {

    @Getter
    private static RpcFileRequestHandler clientDefaulRpcFileRequestHandler;

    @Getter
    private static RpcSimpleRequestMsgHandler clientDefaultRpcSimpleRequestMsgHandler;


    /**
     * 对所有客户端设置默认的defaultHandler
     */
    public static boolean setClientDefaultRpcFileRequestHandler(RpcFileRequestHandler rpcFileRequestHandler) {
        RpcTransHandlerConfigManager.clientDefaulRpcFileRequestHandler = rpcFileRequestHandler;
        return true;
    }

    /**
     * 对所有客户端设置默认的defaultHandler
     */
    public static boolean setClientDefaultRpcSimpleRequestMsgHandler(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        RpcTransHandlerConfigManager.clientDefaultRpcSimpleRequestMsgHandler = rpcSimpleRequestMsgHandler;
        return true;
    }


}
